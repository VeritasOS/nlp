package com.veritas.nlp.relations;

import com.veritas.nlp.models.NlpTagSet;
import com.veritas.nlp.models.NlpTagType;
import com.veritas.nlp.models.Relationship;
import com.veritas.nlp.ner.StanfordEntityRecogniser;
import edu.stanford.nlp.ie.util.RelationTriple;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.naturalli.NaturalLogicAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.ArrayCoreMap;
import edu.stanford.nlp.util.CoreMap;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

public class RelationshipRecognizer {
    private static String NER_MODEL_PATH = StanfordEntityRecogniser.NER_3_CLASSIFIER_PATH;
    private static StanfordCoreNLP openIePipeline;
    private final Map<NlpTagType, NlpTagSet> entities;
    private final Set<Relationship> relationships;

    static {
        System.out.println("Initialising pipeline");
        configureCorefPipeline();
        System.out.println("Initialising pipeline done");
    }

    static void configureCorefPipeline() {
        Properties props2 = new Properties();

        // Minimum required pipeline for OpenIE is: tokenize,ssplit,pos,lemma,depparse,natlog,openie
        // But to get pronouns resolved to names, e.g. "Obama is president" rather than "He is president", we have to add ner + coref
        // Slow parts of the pipeline are depparse, coref and openie.
        //
        props2.setProperty("annotators", "tokenize,ssplit,pos,lemma,depparse,natlog,ner,coref,openie");
        //props2.setProperty("annotators", "tokenize,ssplit,pos,lemma,depparse,natlog,openie");
        props2.setProperty("openie.resolve_coref", "true");
        props2.setProperty("ner.model", NER_MODEL_PATH);
        props2.setProperty("ner.applyNumericClassifiers", "false");
        props2.setProperty("ner.useSUTime", "false");
        props2.setProperty("ner.applyFineGrained", "false");    // Turn off Regex NER as it's slow, apparently.  Does seem to give better results in some cases, though.
        props2.setProperty("coref.algorithm", "statistical");
        props2.setProperty("openie.max_entailments_per_clause", "100"); // Default is 1000.  Lower is faster but less accurate, apparently.  100 is recommended minimum.
        //props2.setProperty("openie.ignore_affinity", "true");
        //props2.setProperty("openie.triple.strict", "false");
        props2.setProperty("openie.triple.all_nominals", "true");   // Hmm, true seems a bit faster, and slightly better results - for simple test, at least - needs more testing

        //ner.buildEntityMentions = false
        openIePipeline = new StanfordCoreNLP(props2, false);
    }

    public RelationshipRecognizer(Map<NlpTagType, NlpTagSet> entities, Set<Relationship> relationships) {
        this.entities = entities;
        this.relationships = relationships;
    }

    public void dumpTimings() {
        System.out.println("PIPELINE TIMINGS:" + openIePipeline.timingInformation());
    }

    public void findRelationships(List<CoreLabel> previousSentence, List<CoreLabel> sentence, List<CoreLabel> nextSentence) {
        List<CoreMap> sentences = new ArrayList<>();
        sentences.add(toCoreMap(previousSentence));
        sentences.add(toCoreMap(sentence));
        sentences.add(toCoreMap(nextSentence));
        Annotation doc = new Annotation(sentences);
        openIePipeline.annotate(doc);

        for (CoreMap processedSentence : doc.get(CoreAnnotations.SentencesAnnotation.class)) {
            Collection<RelationTriple> triples = processedSentence.get(NaturalLogicAnnotations.RelationTriplesAnnotation.class);

            for (RelationTriple triple : triples) {
                boolean isNamedEntitySubject = false, isNamedEntityObject = false;

                // Careful - triple.canonicalSubject is a list, not a string.
                // TODO: Looks at annotation on subject/object to determine if named entity, as a way to short-circuit?
                String lcSubject = toLowerCaseString(triple.canonicalSubject);
                String lcObject = toLowerCaseString(triple.canonicalObject);

                for (NlpTagSet tagSet : entities.values()) {
                    // TEMP HACK
                    if (tagSet.getType() != NlpTagType.PERSON) {
                        continue;
                    }
                    // TEMP HACK

                    for (String tag : tagSet.getTags()) {
                        if (lcSubject.contains(tag.toLowerCase())) {
                            isNamedEntitySubject = true;
                        }
                        if (lcObject.contains(tag.toLowerCase())) {
                            isNamedEntityObject = true;
                        }
                    }
                }

//                for (CoreLabel word : triple.canonicalSubject) {
//                    if (getEntityType(word.get(CoreAnnotations.NamedEntityTagAnnotation.class)) == null) {
//                        isNamedEntitySubject = false;
//                        break;
//                    }
//                }
//                for (CoreLabel word : triple.canonicalObject) {
//                    if (getEntityType(word.get(CoreAnnotations.NamedEntityTagAnnotation.class)) == null) {
//                        isNamedEntityObject = false;
//                        break;
//                    }
//                }

                //String relationship = triple.subjectGloss() + triple.relationGloss() + triple.objectGloss();
                Relationship relationship = new Relationship(
                        triple.subjectGloss(),
                        triple.relationGloss(),
                        triple.objectGloss(),
                        (int)Math.round(triple.confidence * 100));

                if (!relationships.contains(relationship) && (isNamedEntitySubject || isNamedEntityObject)) {
                    relationships.add(relationship);

                    System.out.println(((isNamedEntitySubject || isNamedEntityObject) ? "!!! " : "         ") +
                            triple.confidence + " --- " +
                            triple.subjectGloss() + " --> " +
                            triple.relationGloss() + " --> " +
                            triple.objectGloss());
                }
            }
        }
    }

    private CoreMap toCoreMap(List<CoreLabel> tokens) {
        CoreMap coreMap = new ArrayCoreMap();
        coreMap.set(CoreAnnotations.TokensAnnotation.class, tokens);
        return coreMap;
    }

    private static String toLowerCaseString(List<CoreLabel> labels) {
        if (CollectionUtils.isEmpty(labels)) {
            return "";
        }
        return labels.stream().map(label -> label.get(CoreAnnotations.TextAnnotation.class)).collect(Collectors.joining(" ")).toLowerCase();
    }






/*
    private void dumpRelationships(String text) {
        System.out.println("RELATIONSHIPS FOR: " + text);
        // Create the Stanford CoreNLP pipeline
        //Properties props = new Properties();
        //props.setProperty("annotators", "tokenize,ssplit,pos,lemma,depparse,natlog,openie");
        //StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

        // Annotate an example document.
        Annotation doc = new Annotation(text);
        coreIePipeline.annotate(doc);

        // Loop over sentences in the document
        for (CoreMap sentence : doc.get(CoreAnnotations.SentencesAnnotation.class)) {
            // Get the OpenIE triples for the sentence
            Collection<RelationTriple> triples =
                    sentence.get(NaturalLogicAnnotations.RelationTriplesAnnotation.class);
            // Print the triples
            for (RelationTriple triple : triples) {
                System.out.println("   " + triple.confidence + " --- " +
                        triple.subjectGloss() + " --- " +
                        triple.relationGloss() + " --- " +
                        triple.objectGloss());
            }
        }

    }

    private void dumpRelationshipsWithEntities(String text) {
        Set<String> targetsToInclude = entities.values().stream()
                .flatMap(tagSet -> tagSet.getTags().stream())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        // Annotate an example document.
        Annotation doc = new Annotation(text);
        coreIePipeline.annotate(doc);

        // Loop over sentences in the document
        for (CoreMap sentence : doc.get(CoreAnnotations.SentencesAnnotation.class)) {
            // Get the OpenIE triples for the sentence
            Collection<RelationTriple> triples = sentence.get(NaturalLogicAnnotations.RelationTriplesAnnotation.class);

            // Print the triples
            for (RelationTriple triple : triples) {
                //if (targetsToInclude.contains(triple.subjectGloss().toLowerCase())
                //|| targetsToInclude.contains(triple.objectGloss().toLowerCase())
                //      ) {
                System.out.println("   " + triple.confidence + " --- " +
                        triple.subjectGloss() + " --- " +
                        triple.relationGloss() + " --- " +
                        triple.objectGloss());
                //triple.subjectLemmaGloss() + " --- " +
                //triple.relationLemmaGloss() + " --- " +
                //triple.objectLemmaGloss());
                //}
            }
        }
    }
    */
}
