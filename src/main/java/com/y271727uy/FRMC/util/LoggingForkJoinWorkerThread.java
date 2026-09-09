package com.y271727uy.FRMC.util;

import org.apache.logging.log4j.Logger;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;

public final class LoggingForkJoinWorkerThread extends ForkJoinWorkerThread {
    private final Logger logger;

    public LoggingForkJoinWorkerThread(ForkJoinPool pool, Logger logger) {
        super(pool);
        this.logger = logger;
    }

    @Override
    protected void onTermination(Throwable exception) {
        if (exception != null) logger.warn("{} died", getName(), exception);
        else logger.debug("{} shutdown", getName());
        super.onTermination(exception);
    }
}
