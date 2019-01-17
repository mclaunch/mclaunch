package com.mojang.launcher.game.process;

import com.google.common.base.Predicate;
import com.mojang.launcher.game.process.GameProcessRunnable;
import java.util.Collection;
import java.util.List;

public interface GameProcess {
    public List<String> getStartupArguments();

    public Collection<String> getSysOutLines();

    public Predicate<String> getSysOutFilter();

    public boolean isRunning();

    public void setExitRunnable(GameProcessRunnable var1);

    public GameProcessRunnable getExitRunnable();

    public int getExitCode();

    public void stop();
}

