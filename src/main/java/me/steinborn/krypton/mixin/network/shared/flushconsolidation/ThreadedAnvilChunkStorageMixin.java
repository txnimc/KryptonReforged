package me.steinborn.krypton.mixin.network.shared.flushconsolidation;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import me.steinborn.krypton.mod.shared.network.util.AutoFlushUtil;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.DebugPacketSender;
import net.minecraft.network.IPacket;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.PlayerGenerationTracker;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ChunkManager;
import net.minecraft.world.server.ServerWorld;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixes into various methods in {@code ThreadedAnvilChunkStorage} to utilize flush consolidation for sending chunks
 * all at once to the client. Helpful for heavy server activity or flying very quickly.
 */
@Mixin(ChunkManager.class)
public abstract class ThreadedAnvilChunkStorageMixin {
    @Shadow @Final private PlayerGenerationTracker playerMap;
    @Shadow @Final private ServerWorld level;
    @Shadow @Final private ChunkManager.ProxyTicketManager distanceManager;
    @Shadow private int viewDistance;
    @Shadow @Final private Int2ObjectMap<ChunkManager.EntityTracker> entityMap;

    @Shadow protected abstract boolean skipPlayer(ServerPlayerEntity player);

    @Shadow protected abstract SectionPos updatePlayerPos(ServerPlayerEntity serverPlayerEntity);

    @Shadow
    private static int checkerboardDistance(ChunkPos pos, int x, int z) {
        throw new AssertionError("pedantic");
    }

    @Shadow
    protected abstract void playerLoadedChunk(ServerPlayerEntity player, IPacket<?>[] packets, Chunk chunk);

    @Shadow @Nullable protected abstract ChunkHolder getVisibleChunkIfPresent(long pos);


    @Shadow
    private static int checkerboardDistance(ChunkPos p_219215_0_, ServerPlayerEntity p_219215_1_, boolean p_219215_2_)
    {
        return 0;
    }

    /**
     * @author Andrew Steinborn
     * @reason Add support for flush consolidation
     */
    @Overwrite
    public void move(ServerPlayerEntity player) {
        for (ChunkManager.EntityTracker entityTracker : this.entityMap.values()) {
            if (entityTracker.entity == player) {
                entityTracker.updatePlayers(this.level.players());
            } else {
                entityTracker.updatePlayer(player);
            }
        }

        SectionPos oldPos = player.getLastSectionPos();
        SectionPos newPos = SectionPos.of(player);

        boolean isWatchingWorld = this.playerMap.ignored(player);
        boolean noChunkGen = this.skipPlayer(player);
        boolean movedSections = !oldPos.equals(newPos);

        if (movedSections || isWatchingWorld != noChunkGen) {
            this.updatePlayerPos(player);

            if (!isWatchingWorld) {
                this.distanceManager.removePlayer(oldPos, player);
            }

            if (!noChunkGen) {
                this.distanceManager.addPlayer(newPos, player);
            }

            if (!isWatchingWorld && noChunkGen) {
                this.playerMap.ignorePlayer(player);
            }

            if (isWatchingWorld && !noChunkGen) {
                this.playerMap.unIgnorePlayer(player);
            }

            long oldChunkPos = ChunkPos.asLong(oldPos.getX(), oldPos.getZ());
            long newChunkPos = ChunkPos.asLong(newPos.getX(), newPos.getZ());
            this.playerMap.updatePlayer(oldChunkPos, newChunkPos, player);

            // If the player is in the same world as this tracker, we should send them chunks.
            if (this.level == player.level) {
                this.sendChunks(oldPos, player);
            }
        }
    }

    private void sendChunks(SectionPos oldPos, ServerPlayerEntity player) {
        AutoFlushUtil.setAutoFlush(player, false);

        try {
            int oldChunkX = oldPos.x();
            int oldChunkZ = oldPos.z();

            int newChunkX = MathHelper.floor(player.getX()) >> 4;
            int newChunkZ = MathHelper.floor(player.getZ()) >> 4;

            if (Math.abs(oldChunkX - newChunkX) <= this.viewDistance * 2 && Math.abs(oldChunkZ - newChunkZ) <= this.viewDistance * 2) {
                int minSendChunkX = Math.min(newChunkX, oldChunkX) - this.viewDistance;
                int maxSendChunkZ = Math.min(newChunkZ, oldChunkZ) - this.viewDistance;
                int q = Math.max(newChunkX, oldChunkX) + this.viewDistance;
                int r = Math.max(newChunkZ, oldChunkZ) + this.viewDistance;

                for (int curX = minSendChunkX; curX <= q; ++curX) {
                    for (int curZ = maxSendChunkZ; curZ <= r; ++curZ) {
                        ChunkPos chunkPos = new ChunkPos(curX, curZ);
                        boolean inOld = checkerboardDistance(chunkPos, oldChunkX, oldChunkZ) <= this.viewDistance;
                        boolean inNew = checkerboardDistance(chunkPos, newChunkX, newChunkZ) <= this.viewDistance;
                        this.sendPacketsForChunk(player, chunkPos, new IPacket[2], inOld, inNew);
                    }
                }
            } else {
                for (int curX = oldChunkX - this.viewDistance; curX <= oldChunkX + this.viewDistance; ++curX) {
                    for (int curZ = oldChunkZ - this.viewDistance; curZ <= oldChunkZ + this.viewDistance; ++curZ) {
                        ChunkPos pos = new ChunkPos(curX, curZ);
                        this.sendPacketsForChunk(player, pos, new IPacket[2], true, false);
                    }
                }

                for (int curX = newChunkX - this.viewDistance; curX <= newChunkX + this.viewDistance; ++curX) {
                    for (int curZ = newChunkZ - this.viewDistance; curZ <= newChunkZ + this.viewDistance; ++curZ) {
                        ChunkPos pos = new ChunkPos(curX, curZ);
                        this.sendPacketsForChunk(player, pos, new IPacket[2], false, true);
                    }
                }
            }
        } finally {
            AutoFlushUtil.setAutoFlush(player, true);
        }
    }

    protected void sendPacketsForChunk(ServerPlayerEntity player, ChunkPos pos, IPacket<?>[] packets, boolean withinMaxWatchDistance, boolean withinViewDistance) {
        if (withinViewDistance && !withinMaxWatchDistance) {
            ChunkHolder chunkHolder = this.getVisibleChunkIfPresent(pos.toLong());
            if (chunkHolder != null) {
                Chunk worldChunk = chunkHolder.getTickingChunk();
                if (worldChunk != null) {
                    this.playerLoadedChunk(player, packets, worldChunk);
                }

                DebugPacketSender.sendPoiPacketsForChunk(this.level, pos);
            }
        }

        if (!withinViewDistance && withinMaxWatchDistance) {
            player.untrackChunk(pos);
        }
    }

    @Inject(method = "tick()V", at = @At("HEAD"))
    public void disableAutoFlushForEntityTracking(CallbackInfo info) {
        for (ServerPlayerEntity player : level.players()) {
            AutoFlushUtil.setAutoFlush(player, false);
        }
    }

    @Inject(method = "tick()V", at = @At("RETURN"))
    public void enableAutoFlushForEntityTracking(CallbackInfo info) {
        for (ServerPlayerEntity player : level.players()) {
            AutoFlushUtil.setAutoFlush(player, true);
        }
    }

}
