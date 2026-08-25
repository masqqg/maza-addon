package com.maza;

import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import com.maza.modules.DebrisFinder;

public class MazaAddon extends MeteorAddon {
    public static final Category CATEGORY = new Category("Maza");

    @Override
    public void onInitialize() {
        Modules.get().add(new DebrisFinder());
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() { return "com.maza"; }
}
