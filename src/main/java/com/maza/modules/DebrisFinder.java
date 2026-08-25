package com.maza.modules;

import meteordevelopment.meteorclient.events.world.ChunkDataEvent;
import meteordevelopment.meteorclient.events.world.BlockUpdateEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;
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

    private final Set<BlockPos> debris = ConcurrentHashMap.newKeySet();

    public DebrisFinder() {
        super(MazaAddon.CATEGORY, "debris-esp", "ESP for ancient debris (netherite)");
    }

    @Override
    public void onActivate() {
        debris.clear();
    }

    @Override
    public void onDeactivate() {
        debris.clear();
    }

    // chunk yüklendiğinde tara
    @EventHandler
    private void onChunkData(ChunkDataEvent event) {
        WorldChunk chunk = event.chunk();
        if (chunk == null || mc.world == null) return;

        ChunkPos pos = chunk.getPos();

        for (int y = -64; y < 128; y++) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    BlockPos p = new BlockPos(pos.x * 16 + x, y, pos.z * 16 + z);
                    if (chunk.getBlockState(p).getBlock() == Blocks.ANCIENT_DEBRIS) {
                        debris.add(p);
                    }
                }
            }
        }
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
