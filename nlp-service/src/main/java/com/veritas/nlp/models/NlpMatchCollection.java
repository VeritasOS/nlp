package com.veritas.nlp.models;

import io.swagger.annotations.ApiModelProperty;

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

    @ApiModelProperty(value = ModelStrings.NLP_MATCHES_TOTAL)
    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }
}
