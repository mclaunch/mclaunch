package com.mojang.launcher.game.process;

import com.google.common.base.Predicate;
import com.mojang.launcher.game.process.GameProcess;
import com.mojang.launcher.game.process.GameProcessRunnable;
import java.util.List;

public abstract class AbstractGameProcess
implements GameProcess {
    protected final List<String> arguments;
    protected final Predicate<String> sysOutFilter;
    private GameProcessRunnable onExit;

    public AbstractGameProcess(List<String> arguments, Predicate<String> sysOutFilter) {
        this.arguments = arguments;
        this.sysOutFilter = sysOutFilter;
    }

    @Override
    public Predicate<String> getSysOutFilter() {
        return this.sysOutFilter;
    }

    @Override
    public List<String> getStartupArguments() {
        return this.arguments;
    }

    @Override
    public void setExitRunnable(GameProcessRunnable runnable) {
        this.onExit = runnable;
        if (!this.isRunning() && runnable != null) {
            runnable.onGameProcessEnded(this);
        }
    }

    @Override
    public GameProcessRunnable getExitRunnable() {
        return this.onExit;
    }
}

