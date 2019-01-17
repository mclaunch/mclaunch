package com.mojang.launcher.game.runner;

import com.mojang.launcher.game.GameInstanceStatus;
import com.mojang.launcher.updater.VersionSyncInfo;
import com.mojang.launcher.updater.download.DownloadJob;

public interface GameRunner {
    public GameInstanceStatus getStatus();

    public void playGame(VersionSyncInfo var1);

    public boolean hasRemainingJobs();

    public void addJob(DownloadJob var1);
}

