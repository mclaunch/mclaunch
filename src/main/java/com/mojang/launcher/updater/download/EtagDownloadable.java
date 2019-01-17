package com.mojang.launcher.updater.download;

import com.mojang.launcher.updater.download.Downloadable;
import com.mojang.launcher.updater.download.MonitoringInputStream;
import com.mojang.launcher.updater.download.ProgressContainer;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;

public class EtagDownloadable
extends Downloadable {
    public EtagDownloadable(Proxy proxy, URL remoteFile, File localFile, boolean forceDownload) {
        super(proxy, remoteFile, localFile, forceDownload);
    }

    @Override
    public String download() throws IOException {
        ++this.numAttempts;
        this.ensureFileWritable(this.getTarget());
        try {
            HttpURLConnection connection = this.makeConnection(this.getUrl());
            int status = connection.getResponseCode();
            if (status == 304) {
                return "Used own copy as it matched etag";
            }
            if (status / 100 == 2) {
                this.updateExpectedSize(connection);
                MonitoringInputStream inputStream = new MonitoringInputStream(connection.getInputStream(), this.getMonitor());
                FileOutputStream outputStream = new FileOutputStream(this.getTarget());
                String md5 = EtagDownloadable.copyAndDigest(inputStream, outputStream, "MD5", 32);
                String etag = EtagDownloadable.getEtag(connection.getHeaderField("ETag"));
                if (etag.contains("-")) {
                    return "Didn't have etag so assuming our copy is good";
                }
                if (etag.equalsIgnoreCase(md5)) {
                    return "Downloaded successfully and etag matched";
                }
                throw new RuntimeException(String.format("E-tag did not match downloaded MD5 (ETag was %s, downloaded %s)", etag, md5));
            }
            if (this.getTarget().isFile()) {
                return "Couldn't connect to server (responded with " + status + ") but have local file, assuming it's good";
            }
            throw new RuntimeException("Server responded with " + status);
        }
        catch (IOException e) {
            if (this.getTarget().isFile()) {
                return "Couldn't connect to server (" + e.getClass().getSimpleName() + ": '" + e.getMessage() + "') but have local file, assuming it's good";
            }
            throw e;
        }
    }

    @Override
    protected HttpURLConnection makeConnection(URL url) throws IOException {
        HttpURLConnection connection = super.makeConnection(url);
        if (!this.shouldIgnoreLocal() && this.getTarget().isFile()) {
            connection.setRequestProperty("If-None-Match", EtagDownloadable.getDigest(this.getTarget(), "MD5", 32));
        }
        return connection;
    }

    public static String getEtag(String etag) {
        if (etag == null) {
            etag = "-";
        } else if (etag.startsWith("\"") && etag.endsWith("\"")) {
            etag = etag.substring(1, etag.length() - 1);
        }
        return etag;
    }
}

