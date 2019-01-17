package net.minecraft.launcher;

import com.mojang.launcher.UserInterface;
import com.mojang.launcher.events.GameOutputLogProcessor;
import net.minecraft.launcher.game.MinecraftGameRunner;

public interface MinecraftUserInterface
extends UserInterface {
    public void showOutdatedNotice();

    public String getTitle();

    public GameOutputLogProcessor showGameOutputTab(MinecraftGameRunner var1);

    public boolean shouldDowngradeProfiles();
}

