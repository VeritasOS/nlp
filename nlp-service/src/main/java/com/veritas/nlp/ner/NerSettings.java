package com.veritas.nlp.ner;

public class NerSettings {
    private int maxNerContentSizeChars = 10485760;
    private int nerChunkSizeChars = 65536;

    public int getMaxNerContentSizeChars() {
        return maxNerContentSizeChars;
    }

    public void setMaxNerContentSizeChars(int maxNerContentSizeChars) {
        this.maxNerContentSizeChars = maxNerContentSizeChars;
    }

    public int getNerChunkSizeChars() {
        return nerChunkSizeChars;
    }

    public void setNerChunkSizeChars(int nerChunkSizeChars) {
        this.nerChunkSizeChars = nerChunkSizeChars;
    }
}
