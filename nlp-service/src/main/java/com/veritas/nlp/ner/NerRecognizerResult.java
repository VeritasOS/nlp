package com.veritas.nlp.ner;

import com.veritas.nlp.models.NlpTagSet;
import com.veritas.nlp.models.NlpTagType;
import com.veritas.nlp.models.Relationship;

import java.util.Map;
import java.util.Set;

public class NerRecognizerResult {
    private final Map<NlpTagType, NlpTagSet> entities;
    private final Set<Relationship> relationships;

    public NerRecognizerResult(Map<NlpTagType, NlpTagSet> entities, Set<Relationship> relationships) {
        this.entities = entities;
        this.relationships = relationships;
    }

    public Map<NlpTagType, NlpTagSet> getEntities() {
        return entities;
    }

    public Set<Relationship> getRelationships() {
        return relationships;
    }
}
