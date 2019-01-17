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
import java.nio.charset.Charset;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;

public class ChecksummedDownloadable
extends Downloadable {
    private String localHash;
    private String expectedHash;

    public ChecksummedDownloadable(Proxy proxy, URL remoteFile, File localFile, boolean forceDownload) {
        super(proxy, remoteFile, localFile, forceDownload);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public String download() throws IOException {
        HttpURLConnection connection;
        File target;
        int status;
        InputStream inputStream;
        block19 : {
            ++this.numAttempts;
            this.ensureFileWritable(this.getTarget());
            target = this.getTarget();
            if (this.localHash == null && target.isFile()) {
                this.localHash = ChecksummedDownloadable.getDigest(target, "SHA-1", 40);
            }
            if (this.expectedHash == null) {
                try {
                    connection = this.makeConnection(new URL(this.getUrl().toString() + ".sha1"));
                    status = connection.getResponseCode();
                    if (status / 100 == 2) {
                        inputStream = connection.getInputStream();
                        try {
                            this.expectedHash = IOUtils.toString(inputStream, Charsets.UTF_8).trim();
                            break block19;
                        }
                        catch (IOException e) {
                            this.expectedHash = "";
                            break block19;
                        }
                        finally {
                            IOUtils.closeQuietly(inputStream);
                        }
                    }
                    this.expectedHash = "";
                }
                catch (IOException e) {
                    this.expectedHash = "";
                }
            }
        }
        if (this.expectedHash.length() == 0 && target.isFile()) {
            return "Couldn't find a checksum so assuming our copy is good";
        }
        if (this.expectedHash.equalsIgnoreCase(this.localHash)) {
            return "Remote checksum matches local file";
        }
        try {
            connection = this.makeConnection(this.getUrl());
            status = connection.getResponseCode();
            if (status / 100 == 2) {
                this.updateExpectedSize(connection);
                inputStream = new MonitoringInputStream(connection.getInputStream(), this.getMonitor());
                FileOutputStream outputStream = new FileOutputStream(this.getTarget());
                String digest = ChecksummedDownloadable.copyAndDigest(inputStream, outputStream, "SHA", 40);
                if (this.expectedHash.length() == 0) {
                    return "Didn't have checksum so assuming the downloaded file is good";
                }
                if (this.expectedHash.equalsIgnoreCase(digest)) {
                    return "Downloaded successfully and checksum matched";
                }
                throw new RuntimeException(String.format("Checksum did not match downloaded file (Checksum was %s, downloaded %s)", this.expectedHash, digest));
            }
            if (this.getTarget().isFile()) {
                return "Couldn't connect to server (responded with " + status + ") but have local file, assuming it's good";
            }
            throw new RuntimeException("Server responded with " + status);
        }
        catch (IOException e) {
            if (this.getTarget().isFile() && (this.expectedHash == null || this.expectedHash.length() == 0)) {
                return "Couldn't connect to server (" + e.getClass().getSimpleName() + ": '" + e.getMessage() + "') but have local file, assuming it's good";
            }
            throw e;
        }
    }
}

