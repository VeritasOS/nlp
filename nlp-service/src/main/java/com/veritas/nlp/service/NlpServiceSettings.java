package com.veritas.nlp.service;

import com.veritas.nlp.ner.NerSettings;

public class NlpServiceSettings {
    private NerSettings nerSettings;

    public NerSettings getNerSettings() {
        return nerSettings;
    }

    public void setNerSettings(NerSettings nerSettings) {
        this.nerSettings = nerSettings;
    }
}
