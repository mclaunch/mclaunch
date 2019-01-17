package net.minecraft.launcher.game;

import com.google.common.collect.Iterators;
import com.mojang.launcher.versions.ReleaseType;
import com.mojang.launcher.versions.ReleaseTypeFactory;
import java.util.Iterator;
import net.minecraft.launcher.game.MinecraftReleaseType;

public class MinecraftReleaseTypeFactory
implements ReleaseTypeFactory<MinecraftReleaseType> {
    private static final MinecraftReleaseTypeFactory FACTORY = new MinecraftReleaseTypeFactory();

    private MinecraftReleaseTypeFactory() {
    }

    @Override
    public MinecraftReleaseType getTypeByName(String name) {
        return MinecraftReleaseType.getByName(name);
    }

    public MinecraftReleaseType[] getAllTypes() {
        return MinecraftReleaseType.values();
    }

    @Override
    public Class<MinecraftReleaseType> getTypeClass() {
        return MinecraftReleaseType.class;
    }

    @Override
    public Iterator<MinecraftReleaseType> iterator() {
        return Iterators.forArray(MinecraftReleaseType.values());
    }

    public static MinecraftReleaseTypeFactory instance() {
        return FACTORY;
    }
}

