package com.mojang.launcher.game.process;

import com.mojang.launcher.game.process.GameProcess;
import com.mojang.launcher.game.process.GameProcessBuilder;
import java.io.IOException;

public interface GameProcessFactory {
    public GameProcess startGame(GameProcessBuilder var1) throws IOException;
}

