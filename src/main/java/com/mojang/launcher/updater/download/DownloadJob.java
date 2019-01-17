package com.mojang.launcher.updater.download;

import com.mojang.launcher.updater.download.DownloadListener;
import com.mojang.launcher.updater.download.Downloadable;
import com.mojang.launcher.updater.download.ProgressContainer;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DownloadJob {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int MAX_ATTEMPTS_PER_FILE = 5;
    private static final int ASSUMED_AVERAGE_FILE_SIZE = 5242880;
    private final Queue<Downloadable> remainingFiles = new ConcurrentLinkedQueue<Downloadable>();
    private final List<Downloadable> allFiles = Collections.synchronizedList(new ArrayList());
    private final List<Downloadable> failures = Collections.synchronizedList(new ArrayList());
    private final List<Downloadable> successful = Collections.synchronizedList(new ArrayList());
    private final DownloadListener listener;
    private final String name;
    private final boolean ignoreFailures;
    private final AtomicInteger remainingThreads = new AtomicInteger();
    private final StopWatch stopWatch = new StopWatch();
    private boolean started;

    public DownloadJob(String name, boolean ignoreFailures, DownloadListener listener, Collection<Downloadable> files) {
        this.name = name;
        this.ignoreFailures = ignoreFailures;
        this.listener = listener;
        if (files != null) {
            this.addDownloadables(files);
        }
    }

    public DownloadJob(String name, boolean ignoreFailures, DownloadListener listener) {
        this(name, ignoreFailures, listener, null);
    }

    public void addDownloadables(Collection<Downloadable> downloadables) {
        if (this.started) {
            throw new IllegalStateException("Cannot add to download job that has already started");
        }
        this.allFiles.addAll(downloadables);
        this.remainingFiles.addAll(downloadables);
        for (Downloadable downloadable : downloadables) {
            if (downloadable.getExpectedSize() == 0L) {
                downloadable.getMonitor().setTotal(0x500000L);
            } else {
                downloadable.getMonitor().setTotal(downloadable.getExpectedSize());
            }
            downloadable.getMonitor().setJob(this);
        }
    }

    public /* varargs */ void addDownloadables(Downloadable ... downloadables) {
        if (this.started) {
            throw new IllegalStateException("Cannot add to download job that has already started");
        }
        for (Downloadable downloadable : downloadables) {
            this.allFiles.add(downloadable);
            this.remainingFiles.add(downloadable);
            if (downloadable.getExpectedSize() == 0L) {
                downloadable.getMonitor().setTotal(0x500000L);
            } else {
                downloadable.getMonitor().setTotal(downloadable.getExpectedSize());
            }
            downloadable.getMonitor().setJob(this);
        }
    }

    public void startDownloading(ThreadPoolExecutor executorService) {
        if (this.started) {
            throw new IllegalStateException("Cannot start download job that has already started");
        }
        this.started = true;
        this.stopWatch.start();
        if (this.allFiles.isEmpty()) {
            LOGGER.info("Download job '" + this.name + "' skipped as there are no files to download");
            this.listener.onDownloadJobFinished(this);
        } else {
            int threads = executorService.getMaximumPoolSize();
            this.remainingThreads.set(threads);
            LOGGER.info("Download job '" + this.name + "' started (" + threads + " threads, " + this.allFiles.size() + " files)");
            for (int i = 0; i < threads; ++i) {
                executorService.submit(new Runnable(){

                    @Override
                    public void run() {
                        DownloadJob.this.popAndDownload();
                    }
                });
            }
        }
    }

    private void popAndDownload() {
        Downloadable downloadable;
        while ((downloadable = this.remainingFiles.poll()) != null) {
            if (downloadable.getStartTime() == 0L) {
                downloadable.setStartTime(System.currentTimeMillis());
            }
            if (downloadable.getNumAttempts() > 5) {
                if (!this.ignoreFailures) {
                    this.failures.add(downloadable);
                }
                LOGGER.error("Gave up trying to download " + downloadable.getUrl() + " for job '" + this.name + "'");
                continue;
            }
            try {
                LOGGER.info("Attempting to download " + downloadable.getTarget() + " for job '" + this.name + "'... (try " + downloadable.getNumAttempts() + ")");
                String result = downloadable.download();
                this.successful.add(downloadable);
                downloadable.setEndTime(System.currentTimeMillis());
                downloadable.getMonitor().setCurrent(downloadable.getMonitor().getTotal());
                LOGGER.info("Finished downloading " + downloadable.getTarget() + " for job '" + this.name + "'" + ": " + result);
            }
            catch (Throwable t) {
                LOGGER.warn("Couldn't download " + downloadable.getUrl() + " for job '" + this.name + "'", t);
                downloadable.getMonitor().setCurrent(downloadable.getMonitor().getTotal());
                this.remainingFiles.add(downloadable);
            }
        }
        if (this.remainingThreads.decrementAndGet() <= 0) {
            this.listener.onDownloadJobFinished(this);
        }
    }

    public boolean shouldIgnoreFailures() {
        return this.ignoreFailures;
    }

    public boolean isStarted() {
        return this.started;
    }

    public boolean isComplete() {
        return this.started && this.remainingFiles.isEmpty() && this.remainingThreads.get() == 0;
    }

    public int getFailures() {
        return this.failures.size();
    }

    public int getSuccessful() {
        return this.successful.size();
    }

    public String getName() {
        return this.name;
    }

    public void updateProgress() {
        this.listener.onDownloadJobProgressChanged(this);
    }

    public List<Downloadable> getAllFiles() {
        return this.allFiles;
    }

    public StopWatch getStopWatch() {
        return this.stopWatch;
    }

}

