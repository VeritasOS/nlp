package com.veritas.nlp.resources;

interface ResourceStrings {
    String ENTITIES_OPERATION_NOTES =
            "Extract named entities from the specified text.  Currently supports people, locations and organizations.";
    String ENTITIES_DOCUMENT =
            "The text document to extract entities from.  Must be UTF-8, UTF-16LE, UTF-16BE, UTF-32LE or UTF-32BE.\n\n" +
            "If the text is anything other than UTF-8, a byte order mark MUST be used.";
    String ENTITIES_TYPES =
            "Entities to extract.  Can be one or more of PERSON, LOCATION and ORGANIZATION.  If not specified, all entities are returned.";
    String ENTITIES_MIN_CONFIDENCE_PERCENTAGE =
            "Minimum confidence for named entities to be returned.  A higher percentage reduces the risk of false positives " +
            "(incorrect names) but increases the risk of false negatives (missed names).";
    String ENTITIES_INCLUDE_MATCHES =
            "If true, information about the location / context of the matches is included in the results.";
    String ENTITIES_MAX_CONTENT_MATCHES =
            "Maximum number of content matches to return, per tag set.  Only relevant if includeMatches=true.";
}
