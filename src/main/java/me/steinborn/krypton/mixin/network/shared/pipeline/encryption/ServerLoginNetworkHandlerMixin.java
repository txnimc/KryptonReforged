package me.steinborn.krypton.mixin.network.shared.pipeline.encryption;

import me.steinborn.krypton.mod.shared.network.ClientConnectionEncryptionExtension;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.login.ServerLoginNetHandler;
import net.minecraft.network.login.client.CEncryptionResponsePacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.security.GeneralSecurityException;
import java.security.Key;

@Mixin(ServerLoginNetHandler.class)
public class ServerLoginNetworkHandlerMixin {
    @Shadow private SecretKey secretKey;

    @Shadow @Final public NetworkManager connection;

    @Inject(method = "handleKey", at = @At(value = "FIELD", target = "Lnet/minecraft/network/login/ServerLoginNetHandler;secretKey:Ljavax/crypto/SecretKey;", ordinal = 1))
    public void onKey$initializeVelocityCipher(CEncryptionResponsePacket packet, CallbackInfo info) throws GeneralSecurityException {
        ((ClientConnectionEncryptionExtension) this.connection).setupEncryption(this.secretKey);
    }

    @Redirect(method = "handleKey", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/CryptManager;getCipher(ILjava/security/Key;)Ljavax/crypto/Cipher;"))
    private Cipher onKey$ignoreJavaCipherInitialization(int ignored1, Key ignored2) {
        // Turn the operation into a no-op.
        return null;
    }

    @Redirect(method = "handleKey", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkManager;setEncryptionKey(Ljavax/crypto/Cipher;Ljavax/crypto/Cipher;)V"))
    public void onKey$ignoreMinecraftEncryptionPipelineInjection(NetworkManager connection, Cipher ignored1, Cipher ignored2) {
        // Turn the operation into a no-op.
    }
}
