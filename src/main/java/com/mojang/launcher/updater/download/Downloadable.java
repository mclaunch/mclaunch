package com.mojang.launcher.updater.download;

import com.mojang.launcher.updater.download.ProgressContainer;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class Downloadable {
    private static final Logger LOGGER = LogManager.getLogger();
    private final URL url;
    private final File target;
    private final boolean forceDownload;
    private final Proxy proxy;
    private final ProgressContainer monitor;
    private long startTime;
    protected int numAttempts;
    private long expectedSize;
    private long endTime;

    public Downloadable(Proxy proxy, URL remoteFile, File localFile, boolean forceDownload) {
        this.proxy = proxy;
        this.url = remoteFile;
        this.target = localFile;
        this.forceDownload = forceDownload;
        this.monitor = new ProgressContainer();
    }

    public ProgressContainer getMonitor() {
        return this.monitor;
    }

    public long getExpectedSize() {
        return this.expectedSize;
    }

    public void setExpectedSize(long expectedSize) {
        this.expectedSize = expectedSize;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static String getDigest(File file, String algorithm, int hashLength) {
        DigestInputStream stream;
        stream = null;
        try {
            int read;
            stream = new DigestInputStream(new FileInputStream(file), MessageDigest.getInstance(algorithm));
            byte[] buffer = new byte[65536];
            while ((read = stream.read(buffer)) > 0) {
            }
            Downloadable.closeSilently(stream);
        }
        catch (Exception ignored) {
            String read = null;
            return read;
        }
        finally {
            Downloadable.closeSilently(stream);
        }
        return String.format("%1$0" + hashLength + "x", new BigInteger(1, stream.getMessageDigest().digest()));
    }

    public abstract String download() throws IOException;

    protected void updateExpectedSize(HttpURLConnection connection) {
        if (this.expectedSize == 0L) {
            this.monitor.setTotal(connection.getContentLength());
            this.setExpectedSize(connection.getContentLength());
        } else {
            this.monitor.setTotal(this.expectedSize);
        }
    }

    protected HttpURLConnection makeConnection(URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection)url.openConnection(this.proxy);
        connection.setUseCaches(false);
        connection.setDefaultUseCaches(false);
        connection.setRequestProperty("Cache-Control", "no-store,max-age=0,no-cache");
        connection.setRequestProperty("Expires", "0");
        connection.setRequestProperty("Pragma", "no-cache");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(30000);
        return connection;
    }

    public URL getUrl() {
        return this.url;
    }

    public File getTarget() {
        return this.target;
    }

    public boolean shouldIgnoreLocal() {
        return this.forceDownload;
    }

    public int getNumAttempts() {
        return this.numAttempts;
    }

    public Proxy getProxy() {
        return this.proxy;
    }

    public static void closeSilently(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            }
            catch (IOException iOException) {
                // empty catch block
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static String copyAndDigest(InputStream inputStream, OutputStream outputStream, String algorithm, int hashLength) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(algorithm);
        }
        catch (NoSuchAlgorithmException e) {
            Downloadable.closeSilently(inputStream);
            Downloadable.closeSilently(outputStream);
            throw new RuntimeException("Missing Digest." + algorithm, e);
        }
        byte[] buffer = new byte[65536];
        try {
            int read = inputStream.read(buffer);
            while (read >= 1) {
                digest.update(buffer, 0, read);
                outputStream.write(buffer, 0, read);
                read = inputStream.read(buffer);
            }
        }
        finally {
            Downloadable.closeSilently(inputStream);
            Downloadable.closeSilently(outputStream);
        }
        return String.format("%1$0" + hashLength + "x", new BigInteger(1, digest.digest()));
    }

    protected void ensureFileWritable(File target) {
        if (target.getParentFile() != null && !target.getParentFile().isDirectory()) {
            LOGGER.info("Making directory " + target.getParentFile());
            if (!target.getParentFile().mkdirs() && !target.getParentFile().isDirectory()) {
                throw new RuntimeException("Could not create directory " + target.getParentFile());
            }
        }
        if (target.isFile() && !target.canWrite()) {
            throw new RuntimeException("Do not have write permissions for " + target + " - aborting!");
        }
    }

    public long getStartTime() {
        return this.startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public String getStatus() {
        return "Downloading " + this.getTarget().getName();
    }

    public long getEndTime() {
        return this.endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }
}

