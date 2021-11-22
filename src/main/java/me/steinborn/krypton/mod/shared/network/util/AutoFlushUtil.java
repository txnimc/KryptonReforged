package me.steinborn.krypton.mod.shared.network.util;

import me.steinborn.krypton.mod.shared.network.ConfigurableAutoFlush;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.NetworkManager;

public class AutoFlushUtil {
    public static void setAutoFlush(ServerPlayerEntity player, boolean val) {
        if (player.getClass() == ServerPlayerEntity.class) {
            ConfigurableAutoFlush configurableAutoFlusher = ((ConfigurableAutoFlush) player.connection.getConnection());
            configurableAutoFlusher.setShouldAutoFlush(val);
        }
    }

    public static void setAutoFlush(NetworkManager conn, boolean val) {
        if (conn.getClass() == NetworkManager.class) {
            ConfigurableAutoFlush configurableAutoFlusher = ((ConfigurableAutoFlush) conn);
            configurableAutoFlusher.setShouldAutoFlush(val);
        }
    }

    private AutoFlushUtil() {}
}
