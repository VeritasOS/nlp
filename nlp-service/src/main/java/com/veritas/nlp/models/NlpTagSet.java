package com.veritas.nlp.models;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.*;

public class NlpTagSet {
    private NlpTagType type;
    private Set<String> tags = new HashSet<>();
    private List<NlpMatch> matches = new ArrayList<>();

    public NlpTagSet() {
    }

    public NlpTagSet(NlpTagType type, Set<String> tags) {
        this.type = type;
        this.tags = tags;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public NlpTagType getType() {
        return type;
    }

    public void setType(NlpTagType type) {
        this.type = type;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public List<NlpMatch> getMatches() {
        return matches;
    }

    public void setMatches(List<NlpMatch> matches) {
        this.matches = matches;
    }
}
