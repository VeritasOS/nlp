package com.veritas.nlp.models;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.common.base.Objects;

@JsonPropertyOrder({"subject", "relation", "object"})
public class Relationship {
    private String subject;
    private String object;
    private String relation;
    private int confidencePercentage;

    public Relationship() {
    }

    public Relationship(String subject, String relation, String object, int confidencePercentage) {
        this.subject = subject;
        this.relation = relation;
        this.object = object;
        this.confidencePercentage = confidencePercentage;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public String getRelation() {
        return relation;
    }

    public void setRelation(String relation) {
        this.relation = relation;
    }

    public int getConfidencePercentage() {
        return confidencePercentage;
    }

    public void setConfidencePercentage(int confidencePercentage) {
        this.confidencePercentage = confidencePercentage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Relationship that = (Relationship) o;
        return Objects.equal(subject, that.subject) &&
                Objects.equal(object, that.object) &&
                Objects.equal(relation, that.relation);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(subject, object, relation);
    }
}
