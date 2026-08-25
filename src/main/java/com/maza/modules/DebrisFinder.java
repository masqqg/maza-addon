package com.maza.modules;

import meteordevelopment.meteorclient.events.world.BlockUpdateEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import com.maza.MazaAddon;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DebrisFinder extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
        .name("range").description("Render range in blocks")
        .defaultValue(256).min(16).max(512).build());

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode").defaultValue(ShapeMode.Both).build());

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color").defaultValue(new SettingColor(255, 165, 0, 60)).build());

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color").defaultValue(new SettingColor(255, 165, 0, 255)).build());

    private static final Set<BlockPos> debris = ConcurrentHashMap.newKeySet();

    public DebrisFinder() {
        super(MazaAddon.CATEGORY, "debris-esp", "ESP for ancient debris (netherite) with raw packet bypass");
    }

    @Override
    public void onActivate() {
        debris.clear();
    }

    @Override
    public void onDeactivate() {
        debris.clear();
    }

    // mixin'den çağrılır, packet decode edilmeden ÖNCE
    public static void onRawChunkPacket(ChunkDataS2CPacket packet) {
        try {
            ChunkPos pos = new ChunkPos(packet.getChunkX(), packet.getChunkZ());
            var chunkData = packet.getChunkData();
            
            // raw buffer'dan section'ları oku, anti-cheat henüz dokunmadı
            for (int y = -4; y < 20; y++) {
                var section = chunkData.getSection(y);
                if (section == null || section.isEmpty()) continue;

                var container = section.getBlockStateContainer();
                var palette = container.getPalette();
                int debrisId = -1;

                for (int i = 0; i < palette.getSize(); i++) {
                    if (palette.get(i).getBlock() == Blocks.ANCIENT_DEBRIS) {
                        debrisId = i;
                        break;
                    }
                }
                if (debrisId == -1) continue;

                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        for (int localY = 0; localY < 16; localY++) {
                            if (container.get(x, localY, z) == debrisId) {
                                int worldY = y * 16 + localY;
                                debris.add(new BlockPos(pos.x * 16 + x, worldY, pos.z * 16 + z));
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    // blok güncellendiğinde yakala (kırılma/konma)
    @EventHandler
    private void onBlockUpdate(BlockUpdateEvent event) {
        if (event.newState.getBlock() == Blocks.ANCIENT_DEBRIS) {
            debris.add(event.pos);
        } else {
            debris.remove(event.pos);
        }
    }

    // ESP render
    @EventHandler
    private void onRender(Render3DEvent event) {
        if (mc.player == null) return;

        double px = mc.player.getX();
        double py = mc.player.getY();
        double pz = mc.player.getZ();
        double rangeSq = range.get() * range.get();

        for (BlockPos pos : debris) {
            double dx = pos.getX() + 0.5 - px;
            double dy = pos.getY() + 0.5 - py;
            double dz = pos.getZ() + 0.5 - pz;
            double distSq = dx * dx + dy * dy + dz * dz;

            if (distSq > rangeSq) continue;

            event.renderer.box(
                pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1,
                sideColor.get(), lineColor.get(), shapeMode.get(), 0
            );
        }
    }
}
