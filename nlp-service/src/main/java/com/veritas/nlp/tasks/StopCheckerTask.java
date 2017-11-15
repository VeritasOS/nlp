package com.veritas.nlp.tasks;

import java.io.PrintWriter;

import com.google.common.collect.ImmutableMultimap;

import com.veritas.nlp.service.NlpMicroService;
import io.dropwizard.servlets.tasks.Task;

public class StopCheckerTask extends Task {
    private final NlpMicroService service;

    public StopCheckerTask(NlpMicroService service) {
        super("stopchecker");
        this.service = service;
    }

    @Override
    public void execute(ImmutableMultimap<String, String> immutableMultimap, PrintWriter printWriter) throws Exception {
        // immutableMultimap contains any query string params
        // printWriter is the response writer with Content-Type MediaType.PLAIN_TEXT_UTF_8
        int ctrlC = '\u0003'; // CTRL+C (aka ^C) means it's time to stop
        if (System.in.available() > 0 && System.in.read() == ctrlC) {
            new Thread(() -> {
                try {
                    service.stop();
                } catch (Exception e) {
                    printWriter.print(e);
                }
            }).start();
            printWriter.print("Stopping");
        }
        else {
            printWriter.print("Running");
        }
    }
}
