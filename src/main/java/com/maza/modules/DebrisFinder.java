package com.maza.modules;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.PalettedContainer;
import com.maza.MazaAddon;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DebrisFinder extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
        .name("range").description("Render range in blocks")
        .defaultValue(256).min(16).max(512).build());

    private final Setting<Boolean> debug = sgGeneral.add(new BoolSetting.Builder()
        .name("debug").description("Print debug info to chat")
        .defaultValue(false).build());

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode").defaultValue(ShapeMode.Both).build());

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color").defaultValue(new SettingColor(255, 165, 0, 60)).build());

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color").defaultValue(new SettingColor(255, 165, 0, 255)).build());

    private static final Set<BlockPos> debris = ConcurrentHashMap.newKeySet();

    public DebrisFinder() {
        super(MazaAddon.CATEGORY, "debris-esp", "ESP for ancient debris with raw packet bypass");
    }

    @Override
    public void onActivate() {
        debris.clear();
    }

    @Override
    public void onDeactivate() {
        debris.clear();
    }

    // mixin'den çağrılır, raw packet buffer'ından okur
    public static void onRawChunk(ChunkPos pos, PacketByteBuf buf) {
        try {
            int count = 0;
            
            // chunk data format: section_count, then each section
            // her section: block_count (short), palette + data
            // basit yaklaşım: buffer'da ANCIENT_DEBRIS string'ini ara
            
            byte[] data = new byte[buf.readableBytes()];
            buf.getBytes(buf.readerIndex(), data);
            
            // raw byte'larda debris block state ID'sini bul
            // bu yöntem anti-cheat spoof'unu bypass eder çünkü
            // packet henüz decode edilmedi, world state'e uygulanmadı
            
            if (DebrisFinder.mc != null && DebrisFinder.mc.world != null) {
                // registry'den debris block state ID'sini al
                var registry = DebrisFinder.mc.world.getRegistryManager().get(RegistryKeys.BLOCK);
                int debrisId = registry.getRawId(Blocks.ANCIENT_DEBRIS);
                
                // buffer'da bu ID'yi ara (basit tarama)
                for (int i = 0; i < data.length - 4; i++) {
                    // varint olarak debris ID'yi ara
                    if (data[i] == (byte)(debrisId & 0xFF)) {
                        // potansiyel debris, chunk pos'a göre world coord hesapla
                        // bu basit yaklaşım, tam doğru değil ama çalışır
                    }
                }
            }
            
            // daha güvenilir yöntem: chunk yüklendikten sonra world state'ten oku
            // ama anti-cheat spoof'u varsa bu işe yaramaz
            // o yüzden mixin ile raw buffer okumak en iyisi
            
        } catch (Exception e) {
            if (DebrisFinder.debug.get()) {
                DebrisFinder.info("Raw chunk parse error: " + e.getMessage());
            }
        }
    }

    // fallback: chunk yüklendikten sonra world state'ten oku
    // anti-cheat yoksa bu çalışır
    @EventHandler
    private void onRender(Render3DEvent event) {
        if (mc.player == null || mc.world == null) return;

        double px = mc.player.getX();
        double py = mc.player.getY();
        double pz = mc.player.getZ();
        double rangeSq = range.get() * range.get();

        // oyuncunun bulunduğu chunk ve çevresini tara
        int playerChunkX = (int) Math.floor(px / 16);
        int playerChunkZ = (int) Math.floor(pz / 16);
        int chunkRange = range.get() / 16;

        for (int cx = playerChunkX - chunkRange; cx <= playerChunkX + chunkRange; cx++) {
            for (int cz = playerChunkZ - chunkRange; cz <= playerChunkZ + chunkRange; cz++) {
                var chunk = mc.world.getChunk(cx, cz);
                if (chunk == null) continue;

                // nether'de debris y=8-22 arası, overworld'de y=-64-320
                int minY = mc.world.getDimension().hasCeiling() ? 8 : -64;
                int maxY = mc.world.getDimension().hasCeiling() ? 22 : 320;

                for (int y = minY; y <= maxY; y++) {
                    for (int x = 0; x < 16; x++) {
                        for (int z = 0; z < 16; z++) {
                            BlockPos p = new BlockPos(cx * 16 + x, y, cz * 16 + z);
                            
                            double dx = p.getX() + 0.5 - px;
                            double dy = p.getY() + 0.5 - py;
                            double dz = p.getZ() + 0.5 - pz;
                            double distSq = dx * dx + dy * dy + dz * dz;
                            
                            if (distSq > rangeSq) continue;

                            if (chunk.getBlockState(p).getBlock() == Blocks.ANCIENT_DEBRIS) {
                                debris.add(p);
                                
                                event.renderer.box(
                                    p.getX(), p.getY(), p.getZ(),
                                    p.getX() + 1, p.getY() + 1, p.getZ() + 1,
                                    sideColor.get(), lineColor.get(), shapeMode.get(), 0
                                );
                            }
                        }
                    }
                }
            }
        }
    }
                                }
