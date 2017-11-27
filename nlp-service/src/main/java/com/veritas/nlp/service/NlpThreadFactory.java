package com.veritas.nlp.service;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Map;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NlpThreadFactory implements ThreadFactory {
    private static final Logger LOG = LoggerFactory.getLogger(NlpThreadFactory.class);
    private final String threadName;
    private final AtomicInteger threadCounter = new AtomicInteger();

    public NlpThreadFactory(String threadName) {
        this.threadName = threadName;
    }

    @Override
    public Thread newThread(Runnable runnable) {
        Map<String,String> logContext = MDC.getCopyOfContextMap();
        Thread thread = new Thread(() -> {
            try {
                if (logContext != null) {
                    MDC.setContextMap(logContext);
                }
                runnable.run();
            } catch (Exception e) {
                LOG.error("Failure on thread", e);
                throw e;
            }
        });
        thread.setDaemon(true);
        if (StringUtils.isNotBlank(threadName)) {
            thread.setName(threadName + "-" + threadCounter.incrementAndGet());
        }
        return thread;
    }
}