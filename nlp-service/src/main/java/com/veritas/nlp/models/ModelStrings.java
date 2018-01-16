package com.veritas.nlp.models;

interface ModelStrings {
    String CONTENT_MATCH_DESCRIPTION =
            "Details of a match against a particular part of a document (typically a word or phrase).";

    String NLP_MATCHES_TOTAL =
            "The total number of matches, where available. This may be greater than the number of matches returned " +
            "if the list of returned matches is capped.";
}
