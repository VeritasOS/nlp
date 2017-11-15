package com.veritas.nlp.resources;

interface ResourceStrings {
    String ENTITIES_OPERATION_NOTES = "Extract named entities from the specified text.  Currently supports people, locations and organizations.";
    String ENTITIES_DOCUMENT = "The text document to extract entities from. Must be UTF-8, UTF-16LE, UTF-16BE, UTF-32LE or UTF-32BE.\n\n" +
            "If the text is anything other than UTF-8, a byte order mark MUST be used.";
    String ENTITIES_TYPES = "Entities to extract.  Can be one or more of PERSON, LOCATION and ORGANIZATION. If not specified, all entities are returned";

}
