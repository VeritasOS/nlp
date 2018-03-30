package com.veritas.nlp.models;

import java.util.*;

public class NerResult {
    private List<NlpTagSet> nlpTagSets;    // We use a List rather than a Map<NlpTagType,NlpTagSet> because Swagger sucks
    private Set<Relationship> relationships;

    public NerResult() {
    }

    public NerResult(Map<NlpTagType, NlpTagSet> tagSetsMap) {
        this.nlpTagSets = new ArrayList<>(tagSetsMap.values());
    }

    public NerResult(Map<NlpTagType, NlpTagSet> tagSetsMap, Set<Relationship> relationships) {
        this.nlpTagSets = new ArrayList<>(tagSetsMap.values());
        this.relationships = new HashSet<>(relationships);
    }

    public List<NlpTagSet> getNlpTagSets() {
        return nlpTagSets;
    }

    public void setNlpTagSets(List<NlpTagSet> nlpTagSets) {
        this.nlpTagSets = nlpTagSets;
    }

    public Set<Relationship> getRelationships() {
        return relationships;
    }

    public void setRelationships(Set<Relationship> relationships) {
        this.relationships = relationships;
    }
}
