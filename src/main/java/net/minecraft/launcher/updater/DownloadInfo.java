package net.minecraft.launcher.updater;

import java.net.URL;

public class DownloadInfo {
    protected URL url;
    protected String sha1;
    protected int size;

    public DownloadInfo() {
    }

    public DownloadInfo(DownloadInfo other) {
        this.url = other.url;
        this.sha1 = other.sha1;
        this.size = other.size;
    }

    public URL getUrl() {
        return this.url;
    }

    public String getSha1() {
        return this.sha1;
    }

    public int getSize() {
        return this.size;
    }
}

