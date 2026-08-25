package com.maza.modules;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import com.maza.MazaAddon;

public class DebrisFinder extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
        .name("range").description("Render range in blocks")
        .defaultValue(128).min(16).max(256).build());

    private final Setting<Integer> minY = sgGeneral.add(new IntSetting.Builder()
        .name("min-y").description("Minimum Y level to scan")
        .defaultValue(-11).min(-64).max(320).build());

    private final Setting<Integer> maxY = sgGeneral.add(new IntSetting.Builder()
        .name("max-y").description("Maximum Y level to scan")
        .defaultValue(40).min(-64).max(320).build());

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode").defaultValue(ShapeMode.Both).build());

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color").defaultValue(new SettingColor(255, 165, 0, 60)).build());

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color").defaultValue(new SettingColor(255, 165, 0, 255)).build());

    public DebrisFinder() {
        super(MazaAddon.CATEGORY, "debris-esp", "ESP for ancient debris (y -11 to 40)");
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (mc.player == null || mc.world == null) return;

        double px = mc.player.getX();
        double py = mc.player.getY();
        double pz = mc.player.getZ();
        double rangeSq = (double) range.get() * range.get();

        int playerChunkX = (int) Math.floor(px / 16);
        int playerChunkZ = (int) Math.floor(pz / 16);
        int chunkRange = Math.max(1, range.get() / 16);

        int yMin = minY.get();
        int yMax = maxY.get();

        for (int cx = playerChunkX - chunkRange; cx <= playerChunkX + chunkRange; cx++) {
            for (int cz = playerChunkZ - chunkRange; cz <= playerChunkZ + chunkRange; cz++) {
                var chunk = mc.world.getChunk(cx, cz);
                if (chunk == null || chunk.isEmpty()) continue;

                for (int y = yMin; y <= yMax; y++) {
                    for (int x = 0; x < 16; x++) {
                        for (int z = 0; z < 16; z++) {
                            BlockPos p = new BlockPos(cx * 16 + x, y, cz * 16 + z);

                            if (chunk.getBlockState(p).getBlock() != Blocks.ANCIENT_DEBRIS) continue;

                            double dx = p.getX() + 0.5 - px;
                            double dy = p.getY() + 0.5 - py;
                            double dz = p.getZ() + 0.5 - pz;
                            if (dx * dx + dy * dy + dz * dz > rangeSq) continue;

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
