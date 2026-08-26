package com.maza.modules;

import com.maza.MazaAddon;
import com.maza.utils.DebrisScanner;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.BlockUpdateEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

public class DebrisFinder extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
        .name("range").description("Render range in blocks")
        .defaultValue(128).min(16).max(256).build());

    private final Setting<Integer> minY = sgGeneral.add(new IntSetting.Builder()
        .name("min-y").description("Minimum Y level")
        .defaultValue(-11).min(-64).max(320).build());

    private final Setting<Integer> maxY = sgGeneral.add(new IntSetting.Builder()
        .name("max-y").description("Maximum Y level")
        .defaultValue(40).min(-64).max(320).build());

    private final Setting<Boolean> verifyMode = sgGeneral.add(new BoolSetting.Builder()
        .name("verify-mode").description("Only show debris near you (server tells truth nearby)")
        .defaultValue(false).build());

    private final Setting<Integer> verifyRadius = sgGeneral.add(new IntSetting.Builder()
        .name("verify-radius").description("Radius for verify mode")
        .defaultValue(24).min(8).max(64).visible(verifyMode::get).build());

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode").defaultValue(ShapeMode.Both).build());

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color").defaultValue(new SettingColor(255, 165, 0, 60)).build());

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color").defaultValue(new SettingColor(255, 165, 0, 255)).build());

    public DebrisFinder() {
        super(MazaAddon.CATEGORY, "debris-finder", "ESP for ancient debris with verify mode");
    }

    // blok güncellenince: sahte temizlendiyse kutu zaten kaybolur,
    // gerçek debris ortaya çıktıysa anında yakala
    @EventHandler
    private void onBlockUpdate(BlockUpdateEvent event) {
        if (mc.world == null) return;
        if (event.newState.getBlock() == Blocks.ANCIENT_DEBRIS) {
            double d = DebrisScanner.distanceToPlayer(event.pos);
            if (d < 48) info("Debris revealed at %s (%.0f blocks)", event.pos.toShortString(), d);
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (mc.player == null || mc.world == null) return;

        double px = mc.player.getX();
        double py = mc.player.getY();
        double pz = mc.player.getZ();
        double rangeSq = (double) range.get() * range.get();
        double verSq = (double) verifyRadius.get() * verifyRadius.get();
        boolean verify = verifyMode.get();

        int pcx = (int) Math.floor(px / 16);
        int pcz = (int) Math.floor(pz / 16);
        int cr = Math.max(1, range.get() / 16);
        int yMin = minY.get();
        int yMax = maxY.get();

        for (int cx = pcx - cr; cx <= pcx + cr; cx++) {
            for (int cz = pcz - cr; cz <= pcz + cr; cz++) {
                var chunk = mc.world.getChunk(cx, cz);
                if (chunk == null || chunk.isEmpty()) continue;

                var found = DebrisScanner.scanChunk(chunk, yMin, yMax);

                for (BlockPos p : found) {
                    double dx = p.getX() + 0.5 - px;
                    double dy = p.getY() + 0.5 - py;
                    double dz = p.getZ() + 0.5 - pz;
                    double distSq = dx * dx + dy * dy + dz * dz;

                    if (distSq > rangeSq) continue;
                    if (verify && distSq > verSq) continue;

                    event.renderer.box(
                        p.getX(), p.getY(), p.getZ(),
                        p.getX() + 1, p.getY() + 1, p.getZ() + 1,
                        sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                }
            }
        }
    }
}
