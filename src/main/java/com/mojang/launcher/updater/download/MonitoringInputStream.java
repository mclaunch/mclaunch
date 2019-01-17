package com.mojang.launcher.updater.download;

import com.mojang.launcher.updater.download.ProgressContainer;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class MonitoringInputStream
extends FilterInputStream {
    private final ProgressContainer monitor;

    public MonitoringInputStream(InputStream in, ProgressContainer monitor) {
        super(in);
        this.monitor = monitor;
    }

    @Override
    public int read() throws IOException {
        int result = this.in.read();
        if (result >= 0) {
            this.monitor.addProgress(1L);
        }
        return result;
    }

    @Override
    public int read(byte[] buffer) throws IOException {
        int size = this.in.read(buffer);
        if (size >= 0) {
            this.monitor.addProgress(size);
        }
        return size;
    }

    @Override
    public int read(byte[] buffer, int off, int len) throws IOException {
        int size = this.in.read(buffer, off, len);
        if (size > 0) {
            this.monitor.addProgress(size);
        }
        return size;
    }

    @Override
    public long skip(long size) throws IOException {
        long skipped = super.skip(size);
        if (skipped > 0L) {
            this.monitor.addProgress(skipped);
        }
        return skipped;
    }
}

