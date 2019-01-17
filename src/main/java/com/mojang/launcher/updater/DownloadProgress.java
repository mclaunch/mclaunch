package com.mojang.launcher.updater;

public class DownloadProgress {
    private final long current;
    private final long total;
    private final float percent;
    private final String status;

    public DownloadProgress(long current, long total, String status) {
        this.current = current;
        this.total = total;
        this.percent = (float)current / (float)total;
        this.status = status;
    }

    public long getCurrent() {
        return this.current;
    }

    public long getTotal() {
        return this.total;
    }

    public float getPercent() {
        return this.percent;
    }

    public String getStatus() {
        return this.status;
    }
}

