package com.veritas.nlp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Listen for a trigger to shut down, typically through CTRL+C on stdin.
 */
class ShutdownListener {
    private static final Logger LOG = LoggerFactory.getLogger(ShutdownListener.class);
    private static final int CTRL_C = '\u0003';
    private static final ExecutorService EXECUTOR_SERVICE =
            Executors.newSingleThreadExecutor(new NlpThreadFactory("Shutdown Listener"));
    private final Runnable shutdownAction;

    ShutdownListener(Runnable shutdownAction) {
        this.shutdownAction = shutdownAction;
    }

    public void start() {
        LOG.info("Start listening on STDIN for shutdown triggers.");
        EXECUTOR_SERVICE.submit(() -> {
            try {
                while (true) {
                    int byteRead = System.in.read();

                    if (byteRead == CTRL_C) {
                        LOG.info("CTRL+C detected.  Initiating shutdown.");
                        shutdownAction.run();
                        break;
                    }

                    if (byteRead == -1) {
                        // End of stream - likely because the launching process has died.
                        if (isJvmShuttingDown()) {
                            LOG.info("JVM shutting down, so ignoring end of stream on STDIN.");
                        } else {
                            LOG.info("STDIN end of stream detected. Assume launching process has died. Initiating shutdown.");
                            shutdownAction.run();
                        }
                        break;
                    }
                }
            } catch (Exception e) {
                LOG.error("Failed while listening for shutdown", e);
            }
        });
    }

    private boolean isJvmShuttingDown() {
        // No way to directly find out if JVM is shutting down, but we can find out indirectly by
        // trying to add a shutdown hook.  It will fail if we're shutting down.
        try {
            Thread noopShutdownHook = new Thread(() -> {});
            Runtime.getRuntime().addShutdownHook(noopShutdownHook);
            Runtime.getRuntime().removeShutdownHook(noopShutdownHook);
            return false;
        } catch (Exception e) {
            return true;
        }
    }
}
