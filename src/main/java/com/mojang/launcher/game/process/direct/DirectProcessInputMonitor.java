package com.mojang.launcher.game.process.direct;

import com.google.common.base.Predicate;
import com.mojang.launcher.events.GameOutputLogProcessor;
import com.mojang.launcher.game.process.GameProcess;
import com.mojang.launcher.game.process.GameProcessRunnable;
import com.mojang.launcher.game.process.direct.DirectGameProcess;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DirectProcessInputMonitor
extends Thread {
    private static final Logger LOGGER = LogManager.getLogger();
    private final DirectGameProcess process;
    private final GameOutputLogProcessor logProcessor;

    public DirectProcessInputMonitor(DirectGameProcess process, GameOutputLogProcessor logProcessor) {
        this.process = process;
        this.logProcessor = logProcessor;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void run() {
        InputStreamReader reader = new InputStreamReader(this.process.getRawProcess().getInputStream());
        BufferedReader buf = new BufferedReader(reader);
        String line = null;
        while (this.process.isRunning()) {
            try {
                while ((line = buf.readLine()) != null) {
                    this.logProcessor.onGameOutput(this.process, line);
                    if (this.process.getSysOutFilter().apply(line) != Boolean.TRUE.booleanValue()) continue;
                    this.process.getSysOutLines().add(line);
                }
            }
            catch (IOException ex) {
                LOGGER.error(ex);
            }
            finally {
                IOUtils.closeQuietly(reader);
            }
        }
        GameProcessRunnable onExit = this.process.getExitRunnable();
        if (onExit != null) {
            onExit.onGameProcessEnded(this.process);
        }
    }
}

