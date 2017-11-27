package com.veritas.nlp.service;

import com.veritas.nlp.ner.NerSettings;

public class NlpServiceSettings {
    private boolean shutdownListenerEnabled = true;
    private NerSettings nerSettings;

    public NerSettings getNerSettings() {
        return nerSettings;
    }

    public void setNerSettings(NerSettings nerSettings) {
        this.nerSettings = nerSettings;
    }

    public boolean isShutdownListenerEnabled() {
        return shutdownListenerEnabled;
    }

    public void setShutdownListenerEnabled(boolean shutdownListenerEnabled) {
        this.shutdownListenerEnabled = shutdownListenerEnabled;
    }
}
