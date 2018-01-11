package com.veritas.nlp.resources;

import com.veritas.nlp.models.NlpTagType;
import org.apache.commons.collections4.CollectionUtils;

import java.time.Duration;
import java.util.EnumSet;
import java.util.Set;

public class NlpRequestParams {
    private Duration timeout = Duration.ofSeconds(30);
    private int minConfidencePercentage;
    private boolean includeMatches;
    private Integer maxContentMatches = 100;
    private EnumSet<NlpTagType> tagTypes = EnumSet.allOf(NlpTagType.class);

    public Duration getTimeout() {
        return timeout;
    }

    public NlpRequestParams setTimeout(Duration timeout) {
        this.timeout = timeout;
        return this;
    }

    public int getMinConfidencePercentage() {
        return minConfidencePercentage;
    }

    public NlpRequestParams setMinConfidencePercentage(int minConfidencePercentage) {
        this.minConfidencePercentage = minConfidencePercentage;
        return this;
    }

    public boolean includeMatches() {
        return includeMatches;
    }

    public NlpRequestParams setIncludeMatches(boolean includeMatches) {
        this.includeMatches = includeMatches;
        return this;
    }

    public Integer getMaxContentMatches() {
        return maxContentMatches;
    }

    public NlpRequestParams setMaxContentMatches(Integer maxContentMatches) {
        if (maxContentMatches != null) {
            this.maxContentMatches = maxContentMatches;
        }
        return this;
    }

    public EnumSet<NlpTagType> getTagTypes() {
        return tagTypes;
    }

    public NlpRequestParams setTagTypes(Set<NlpTagType> tagTypes) {
        if (CollectionUtils.isNotEmpty(tagTypes)) {
            this.tagTypes = EnumSet.copyOf(tagTypes);
        }
        return this;
    }
}
