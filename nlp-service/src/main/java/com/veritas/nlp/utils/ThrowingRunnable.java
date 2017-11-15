package com.veritas.nlp.utils;

/**
 * Like Runnable, but can throw exceptions.
 */
@FunctionalInterface
public interface ThrowingRunnable {
    void run() throws Exception;
}