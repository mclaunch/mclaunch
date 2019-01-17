package com.mojang.launcher;

import com.mojang.launcher.updater.DownloadProgress;
import com.mojang.launcher.versions.CompleteVersion;
import java.io.File;

public interface UserInterface {
    public void showLoginPrompt();

    public void setVisible(boolean var1);

    public void shutdownLauncher();

    public void hideDownloadProgress();

    public void setDownloadProgress(DownloadProgress var1);

    public void showCrashReport(CompleteVersion var1, File var2, String var3);

    public void gameLaunchFailure(String var1);

    public void updatePlayState();
}

