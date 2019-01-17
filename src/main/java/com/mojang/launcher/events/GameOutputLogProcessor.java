package com.mojang.launcher.events;

import com.mojang.launcher.game.process.GameProcess;

public interface GameOutputLogProcessor {
    public void onGameOutput(GameProcess var1, String var2);
}

