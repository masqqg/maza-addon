package com.maza.mixin;

import com.maza.modules.DebrisFinder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkDataS2CPacket.class)
public class ChunkDataPacketMixin {

    @Inject(method = "<init>(Lio/netty/buffer/ByteBuf;)V", at = @At("RETURN"))
    private void onConstruct(ByteBuf buf, CallbackInfo ci) {
        try {
            // raw buffer'dan chunk pos oku (decode edilmeden önce)
            PacketByteBuf pbuf = new PacketByteBuf(buf.copy());
            int chunkX = pbuf.readInt();
            int chunkZ = pbuf.readInt();
            
            DebrisFinder.onRawChunk(new ChunkPos(chunkX, chunkZ), pbuf);
        } catch (Exception ignored) {}
    }
}
