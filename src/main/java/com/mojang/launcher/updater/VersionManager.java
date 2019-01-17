package com.mojang.launcher.updater;

import com.mojang.launcher.events.RefreshedVersionsListener;
import com.mojang.launcher.updater.VersionFilter;
import com.mojang.launcher.updater.VersionSyncInfo;
import com.mojang.launcher.updater.download.DownloadJob;
import com.mojang.launcher.versions.CompleteVersion;
import com.mojang.launcher.versions.ReleaseType;
import com.mojang.launcher.versions.Version;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

public interface VersionManager {
    public void refreshVersions() throws IOException;

    public List<VersionSyncInfo> getVersions();

    public List<VersionSyncInfo> getVersions(VersionFilter<? extends ReleaseType> var1);

    public VersionSyncInfo getVersionSyncInfo(Version var1);

    public VersionSyncInfo getVersionSyncInfo(String var1);

    public VersionSyncInfo getVersionSyncInfo(Version var1, Version var2);

    public List<VersionSyncInfo> getInstalledVersions();

    public CompleteVersion getLatestCompleteVersion(VersionSyncInfo var1) throws IOException;

    public DownloadJob downloadVersion(VersionSyncInfo var1, DownloadJob var2) throws IOException;

    public DownloadJob downloadResources(DownloadJob var1, CompleteVersion var2) throws IOException;

    public ThreadPoolExecutor getExecutorService();

    public void addRefreshedVersionsListener(RefreshedVersionsListener var1);

    public void removeRefreshedVersionsListener(RefreshedVersionsListener var1);

    public VersionSyncInfo syncVersion(VersionSyncInfo var1) throws IOException;

    public void installVersion(CompleteVersion var1) throws IOException;

    public void uninstallVersion(CompleteVersion var1) throws IOException;
}

