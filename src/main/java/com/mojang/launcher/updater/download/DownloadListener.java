package com.mojang.launcher.updater.download;

import com.mojang.launcher.updater.download.DownloadJob;

public interface DownloadListener {
    public void onDownloadJobFinished(DownloadJob var1);

    public void onDownloadJobProgressChanged(DownloadJob var1);
}

