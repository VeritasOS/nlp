package com.veritas.nlp.models;

import io.swagger.annotations.ApiModelProperty;

import java.util.Map;
import java.util.Set;

public class NerResult {
    private Map<NerEntityType, Set<String>> entities;

    public NerResult() {}

    public NerResult(Map<NerEntityType, Set<String>> entities) {
        this.entities = entities;
    }

    @ApiModelProperty(value="Map of entity types (e.g. PERSON, LOCATION) to set of entities (e.g. \"Sue Smith\", \"London\")")
    public Map<NerEntityType, Set<String>> getEntities() {
        return entities;
    }

    public void setEntities(Map<NerEntityType, Set<String>> entities) {
        this.entities = entities;
    }
}
