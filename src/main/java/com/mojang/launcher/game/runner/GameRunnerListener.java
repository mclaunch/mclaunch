package com.mojang.launcher.game.runner;

import com.mojang.launcher.game.GameInstanceStatus;
import com.mojang.launcher.game.runner.GameRunner;

public interface GameRunnerListener {
    public void onGameInstanceChangedState(GameRunner var1, GameInstanceStatus var2);
}

