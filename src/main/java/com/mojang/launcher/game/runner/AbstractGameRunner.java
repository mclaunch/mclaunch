package com.mojang.launcher.game.runner;

import com.google.common.collect.Lists;
import com.mojang.launcher.Launcher;
import com.mojang.launcher.UserInterface;
import com.mojang.launcher.game.GameInstanceStatus;
import com.mojang.launcher.game.runner.GameRunner;
import com.mojang.launcher.game.runner.GameRunnerListener;
import com.mojang.launcher.updater.DownloadProgress;
import com.mojang.launcher.updater.VersionManager;
import com.mojang.launcher.updater.VersionSyncInfo;
import com.mojang.launcher.updater.download.DownloadJob;
import com.mojang.launcher.updater.download.DownloadListener;
import com.mojang.launcher.updater.download.Downloadable;
import com.mojang.launcher.updater.download.ProgressContainer;
import com.mojang.launcher.versions.CompleteVersion;
import com.mojang.launcher.versions.Version;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class AbstractGameRunner
implements GameRunner,
DownloadListener {
    protected static final Logger LOGGER = LogManager.getLogger();
    protected final Object lock = new Object();
    private final List<DownloadJob> jobs = new ArrayList<DownloadJob>();
    protected CompleteVersion version;
    private GameInstanceStatus status = GameInstanceStatus.IDLE;
    private final List<GameRunnerListener> listeners = Lists.newArrayList();

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    protected void setStatus(GameInstanceStatus status) {
        Object object = this.lock;
        synchronized (object) {
            this.status = status;
            for (GameRunnerListener listener : Lists.newArrayList(this.listeners)) {
                listener.onGameInstanceChangedState(this, status);
            }
        }
    }

    protected abstract Launcher getLauncher();

    @Override
    public GameInstanceStatus getStatus() {
        return this.status;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void playGame(VersionSyncInfo syncInfo) {
        Object object = this.lock;
        synchronized (object) {
            if (this.getStatus() != GameInstanceStatus.IDLE) {
                LOGGER.warn("Tried to play game but game is already starting!");
                return;
            }
            this.setStatus(GameInstanceStatus.PREPARING);
        }
        LOGGER.info("Getting syncinfo for selected version");
        if (syncInfo == null) {
            LOGGER.warn("Tried to launch a version without a version being selected...");
            this.setStatus(GameInstanceStatus.IDLE);
            return;
        }
        object = this.lock;
        synchronized (object) {
            LOGGER.info("Queueing library & version downloads");
            try {
                this.version = this.getLauncher().getVersionManager().getLatestCompleteVersion(syncInfo);
            }
            catch (IOException e) {
                LOGGER.error("Couldn't get complete version info for " + syncInfo.getLatestVersion(), (Throwable)e);
                this.setStatus(GameInstanceStatus.IDLE);
                return;
            }
            if (syncInfo.getRemoteVersion() != null && syncInfo.getLatestSource() != VersionSyncInfo.VersionSource.REMOTE && !this.version.isSynced()) {
                try {
                    syncInfo = this.getLauncher().getVersionManager().syncVersion(syncInfo);
                    this.version = this.getLauncher().getVersionManager().getLatestCompleteVersion(syncInfo);
                }
                catch (IOException e) {
                    LOGGER.error("Couldn't sync local and remote versions", (Throwable)e);
                }
                this.version.setSynced(true);
            }
            if (!this.version.appliesToCurrentEnvironment()) {
                String reason = this.version.getIncompatibilityReason();
                if (reason == null) {
                    reason = "This version is incompatible with your computer. Please try another one by going into Edit Profile and selecting one through the dropdown. Sorry!";
                }
                LOGGER.error("Version " + this.version.getId() + " is incompatible with current environment: " + reason);
                this.getLauncher().getUserInterface().gameLaunchFailure(reason);
                this.setStatus(GameInstanceStatus.IDLE);
                return;
            }
            if (this.version.getMinimumLauncherVersion() > this.getLauncher().getLauncherFormatVersion()) {
                LOGGER.error("An update to your launcher is available and is required to play " + this.version.getId() + ". Please restart your launcher.");
                this.setStatus(GameInstanceStatus.IDLE);
                return;
            }
            if (!syncInfo.isUpToDate()) {
                try {
                    this.getLauncher().getVersionManager().installVersion(this.version);
                }
                catch (IOException e) {
                    LOGGER.error("Couldn't save version info to install " + syncInfo.getLatestVersion(), (Throwable)e);
                    this.setStatus(GameInstanceStatus.IDLE);
                    return;
                }
            }
            this.setStatus(GameInstanceStatus.DOWNLOADING);
            this.downloadRequiredFiles(syncInfo);
        }
    }

    protected void downloadRequiredFiles(VersionSyncInfo syncInfo) {
        try {
            DownloadJob librariesJob = new DownloadJob("Version & Libraries", false, this);
            this.addJob(librariesJob);
            this.getLauncher().getVersionManager().downloadVersion(syncInfo, librariesJob);
            librariesJob.startDownloading(this.getLauncher().getDownloaderExecutorService());
            DownloadJob resourceJob = new DownloadJob("Resources", true, this);
            this.addJob(resourceJob);
            this.getLauncher().getVersionManager().downloadResources(resourceJob, this.version);
            resourceJob.startDownloading(this.getLauncher().getDownloaderExecutorService());
        }
        catch (IOException e) {
            LOGGER.error("Couldn't get version info for " + syncInfo.getLatestVersion(), (Throwable)e);
            this.setStatus(GameInstanceStatus.IDLE);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    protected void updateProgressBar() {
        Object object = this.lock;
        synchronized (object) {
            if (this.hasRemainingJobs()) {
                long total = 0L;
                long current = 0L;
                Downloadable longestRunning = null;
                for (DownloadJob job : this.jobs) {
                    for (Downloadable file : job.getAllFiles()) {
                        total += file.getMonitor().getTotal();
                        current += file.getMonitor().getCurrent();
                        if (longestRunning != null && longestRunning.getEndTime() <= 0L && (file.getStartTime() >= longestRunning.getStartTime() || file.getEndTime() != 0L)) continue;
                        longestRunning = file;
                    }
                }
                this.getLauncher().getUserInterface().setDownloadProgress(new DownloadProgress(current, total, longestRunning == null ? null : longestRunning.getStatus()));
            } else {
                this.jobs.clear();
                this.getLauncher().getUserInterface().hideDownloadProgress();
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public boolean hasRemainingJobs() {
        Object object = this.lock;
        synchronized (object) {
            for (DownloadJob job : this.jobs) {
                if (job.isComplete()) continue;
                return true;
            }
        }
        return false;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void addJob(DownloadJob job) {
        Object object = this.lock;
        synchronized (object) {
            this.jobs.add(job);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void onDownloadJobFinished(DownloadJob job) {
        this.updateProgressBar();
        Object object = this.lock;
        synchronized (object) {
            if (job.getFailures() > 0) {
                LOGGER.error("Job '" + job.getName() + "' finished with " + job.getFailures() + " failure(s)! (took " + job.getStopWatch().toString() + ")");
                this.setStatus(GameInstanceStatus.IDLE);
            } else {
                LOGGER.info("Job '" + job.getName() + "' finished successfully (took " + job.getStopWatch().toString() + ")");
                if (this.getStatus() != GameInstanceStatus.IDLE && !this.hasRemainingJobs()) {
                    try {
                        this.setStatus(GameInstanceStatus.LAUNCHING);
                        this.launchGame();
                    }
                    catch (Throwable ex) {
                        LOGGER.fatal("Fatal error launching game. Report this to http://bugs.mojang.com please!", ex);
                    }
                }
            }
        }
    }

    protected abstract void launchGame() throws IOException;

    @Override
    public void onDownloadJobProgressChanged(DownloadJob job) {
        this.updateProgressBar();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void addListener(GameRunnerListener listener) {
        Object object = this.lock;
        synchronized (object) {
            this.listeners.add(listener);
        }
    }
}

