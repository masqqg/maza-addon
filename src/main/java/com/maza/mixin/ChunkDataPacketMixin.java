package com.maza.mixin;

import com.maza.modules.DebrisFinder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket")
public class ChunkDataPacketMixin {

    @Inject(method = "<init>(Lnet/minecraft/network/PacketByteBuf;)V", at = @At("RETURN"))
    private void onRead(PacketByteBuf buf, CallbackInfo ci) {
        try {
            // buffer'ı başa sar ve tekrar oku
            int readerIndex = buf.readerIndex();
            buf.readerIndex(0);
            
            int chunkX = buf.readInt();
            int chunkZ = buf.readInt();
            
            // raw byte'ları kopyala
            byte[] data = new byte[buf.readableBytes()];
            buf.readBytes(data);
            
            // buffer'ı eski yerine döndür
            buf.readerIndex(readerIndex);
            
            DebrisFinder.onRawChunkData(new ChunkPos(chunkX, chunkZ), data);
        } catch (Exception ignored) {}
    }
}
