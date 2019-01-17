package com.mojang.launcher.updater;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ExceptionalThreadPoolExecutor
extends ThreadPoolExecutor {
    private static final Logger LOGGER = LogManager.getLogger();

    public ExceptionalThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, new LinkedBlockingQueue<Runnable>(), new ThreadFactoryBuilder().setDaemon(true).build());
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        if (t == null && r instanceof Future) {
            try {
                Future future = (Future)((Object)r);
                if (future.isDone()) {
                    future.get();
                }
            }
            catch (CancellationException ce) {
                t = ce;
            }
            catch (ExecutionException ee) {
                t = ee.getCause();
            }
            catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        return new ExceptionalFutureTask<T>(runnable, value);
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        return new ExceptionalFutureTask<T>(callable);
    }

    public class ExceptionalFutureTask<T>
    extends FutureTask<T> {
        public ExceptionalFutureTask(Callable<T> callable) {
            super(callable);
        }

        public ExceptionalFutureTask(Runnable runnable, T result) {
            super(runnable, result);
        }

        @Override
        protected void done() {
            try {
                this.get();
            }
            catch (Throwable t) {
                LOGGER.error("Unhandled exception in executor " + this, t);
            }
        }
    }

}

