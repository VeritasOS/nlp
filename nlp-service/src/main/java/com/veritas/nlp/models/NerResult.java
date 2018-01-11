package com.veritas.nlp.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NerResult {
    private List<NlpTagSet> nlpTagSets;    // We use a List rather than a Map<NlpTagType,NlpTagSet> because Swagger sucks

    public NerResult() {
    }

    public NerResult(Map<NlpTagType, NlpTagSet> tagSetsMap) {
        this.nlpTagSets = new ArrayList<>(tagSetsMap.values());
    }

    public List<NlpTagSet> getNlpTagSets() {
        return nlpTagSets;
    }

    public void setNlpTagSets(List<NlpTagSet> nlpTagSets) {
        this.nlpTagSets = nlpTagSets;
    }
}
