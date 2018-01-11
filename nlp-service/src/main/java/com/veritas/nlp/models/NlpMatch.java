package com.veritas.nlp.models;

import io.swagger.annotations.ApiModel;

@ApiModel(description = ModelStrings.CONTENT_MATCH_DESCRIPTION)
public class NlpMatch {
    private long offset;
    private long length;
    private String content;
    private String context;
    private int contextOffset;

    public NlpMatch() {
    }

    public NlpMatch(long offset, long length, String content, String context, int contextOffset) {
        this.offset = offset;
        this.length = length;
        this.content = content;
        this.context = context;
        this.contextOffset = contextOffset;
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public int getContextOffset() {
        return contextOffset;
    }

    public void setContextOffset(int contextOffset) {
        this.contextOffset = contextOffset;
    }
}