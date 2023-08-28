package com.veritas.nlp.models;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

public class NlpMatchCollection {
    private int total;
    private List<NlpMatch> matches = new ArrayList<>();

    public List<NlpMatch> getMatches() {
        return matches;
    }

    public void setMatches(List<NlpMatch> matches) {
        this.matches = matches;
    }

    @Schema(description = ModelStrings.NLP_MATCHES_TOTAL)
    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }
}
