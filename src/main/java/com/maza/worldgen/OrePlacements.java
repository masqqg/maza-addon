package com.maza.worldgen;

import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.intprovider.ConstantIntProvider;
import net.minecraft.util.math.intprovider.IntProvider;
import net.minecraft.world.gen.YOffset;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.OreFeatureConfig;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.placementmodifier.HeightRangePlacementModifier;
import net.minecraft.world.gen.placementmodifier.PlacementModifier;

import java.util.*;

public class OrePlacements {
    public final int index;
    public final int step;
    public final IntProvider count;
    public final YOffset height;
    public final Object heightContext;
    public final float rarityChance;
    public final int size;
    public final boolean scattered;
    public final float discardOnAirChance;

    private OrePlacements(int index, int step, IntProvider count, YOffset height, Object heightContext,
                          float rarityChance, int size, boolean scattered, float discardOnAirChance) {
        this.index = index;
        this.step = step;
        this.count = count;
        this.height = height;
        this.heightContext = heightContext;
        this.rarityChance = rarityChance;
        this.size = size;
        this.scattered = scattered;
        this.discardOnAirChance = discardOnAirChance;
    }

    public boolean isDebris() {
        return true; // sadece debris için kullanıyoruz
    }

    public static Map<RegistryEntry<?>, List<OrePlacements>> buildBiomeTable() {
        Map<RegistryEntry<?>, List<OrePlacements>> table = new HashMap<>();
        
        // nether debris config
        // vanilla: 2 vein types
        // 1) y=8-22, size=3, scattered
        // 2) y=8-119, size=2, rare
        
        List<OrePlacements> netherFeatures = new ArrayList<>();
        
        // type 1: common, y=8-22
        netherFeatures.add(new OrePlacements(
            0, 0,
            ConstantIntProvider.create(2),
            YOffset.aboveBottom(8), null,
            1.0f, 3, true, 0.0f
        ));
        
        // type 2: rare, y=8-119
        netherFeatures.add(new OrePlacements(
            1, 1,
            ConstantIntProvider.create(1),
            YOffset.aboveBottom(8), null,
            0.015625f, 2, false, 0.5f
        ));
        
        // tüm nether biomes'a ekle
        // basit yaklaşım: null key'e koy, sonra hepsine uygula
        table.put(null, netherFeatures);
        
        return table;
    }
          }
