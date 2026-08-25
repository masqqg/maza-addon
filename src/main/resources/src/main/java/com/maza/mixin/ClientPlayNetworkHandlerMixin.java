package com.maza.mixin;

import com.maza.modules.DebrisFinder;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

    @Inject(method = "onChunkData", at = @At("HEAD"))
    private void onChunkDataHead(ChunkDataS2CPacket packet, CallbackInfo ci) {
        DebrisFinder.onRawChunkPacket(packet);
    }
}
