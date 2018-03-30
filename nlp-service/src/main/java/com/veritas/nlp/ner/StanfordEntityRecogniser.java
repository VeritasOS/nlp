package com.veritas.nlp.ner;

import com.google.common.base.Enums;
import com.google.common.base.Stopwatch;
import com.google.common.base.Suppliers;
import com.veritas.nlp.models.*;
import com.veritas.nlp.relations.RelationshipRecognizer;
import com.veritas.nlp.resources.NlpRequestParams;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ie.crf.CRFCliqueTree;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;


public class StanfordEntityRecogniser {
    public static final String NER_3_CLASSIFIER_PATH = "edu/stanford/nlp/models/ner/english.all.3class.distsim.crf.ser.gz";
    private static final Supplier<CRFClassifier<CoreLabel>> classifier = Suppliers.memoize(StanfordEntityRecogniser::createClassifier);
    private static final int CONTEXT_BEFORE_CHARS = 150;
    private static final int CONTEXT_AFTER_CHARS = 150;
    private static final int MAX_SENTENCE_TOKENS_FOR_OPENIE = 50;
    private static final int MAX_SENTENCE_TOKENS_FOR_NER = 300;

    private final Map<NlpTagType, NlpTagSet> entities;
    private final Set<Relationship> relationships;
    private final String text;
    private final NlpRequestParams params;
    private final long matchBaseOffset;

    StanfordEntityRecogniser(
            Map<NlpTagType, NlpTagSet> entities,
            String text,
            NlpRequestParams params,
            long matchBaseOffset,
            Set<Relationship> relationships) {

        this.entities = entities;
        this.text = text;
        this.params = params;
        this.matchBaseOffset = matchBaseOffset;
        this.relationships = relationships;
    }

    void extractEntities() {
        Stopwatch sw = Stopwatch.createStarted();
        //System.out.println("Extracting entities for text: " + text);
        extractEntities(classifier.get().classify(text));
        //dumpRelationshipsWithEntities(text);
        System.out.println("DONE IN " + sw.elapsed(TimeUnit.MILLISECONDS));
    }

    private void extractEntities(List<List<CoreLabel>> sentences) {
        //System.out.println("SENTENCES: " + sentences.size());
        RelationshipRecognizer relationshipRecognizer = new RelationshipRecognizer(entities, relationships);
        for (int i=0; i < sentences.size(); i++) {
            //List<CoreLabel> previousSentence = i == 0 ? Collections.emptyList() : sentences.get(i-1);
            List<CoreLabel> sentence = sentences.get(i);
            List<CoreLabel> nextSentence = (i == sentences.size()-1) ? Collections.emptyList() : sentences.get(i+1);

            //getEntitiesForSentence(previousSentence, sentence, nextSentence);
            //getEntitiesForSentence(Collections.emptyList(), sentence, Collections.emptyList());
            getEntitiesForSentence(relationshipRecognizer, Collections.emptyList(), sentence, nextSentence);
        }
        relationshipRecognizer.dumpTimings();
    }

    private void getEntitiesForSentence(
            RelationshipRecognizer relationshipRecognizer,
            List<CoreLabel> previousSentence,
            List<CoreLabel> sentence,
            List<CoreLabel> nextSentence) {

        if (sentence.size() > MAX_SENTENCE_TOKENS_FOR_NER) {
            return;
        }

        // The API for extraction of the named entities is a bit strange.  We have to look for sequences of words
        // with particular entity type.  For example a sequence of three words of entity type PERSON is considered a
        // single person.
        //
        // There is a simpler alternative API (classifyToCharacterOffsets) that returns whole entities directly,
        // but it is not ideal because it doesn't normalize whitespace characters.  This way we get the individual
        // words separately so we don't have to deal with whitespace at all.
        //
        CRFCliqueTree<String> cliqueTree = null;
        boolean sentenceHasEntities = false;

        for (int wordPos = 0; wordPos < sentence.size(); wordPos++) {
            CoreLabel word = sentence.get(wordPos);

            List<EntityToken> entityTokens = null;
            NlpTagType entityType = getEntityType(word);

            while (params.getTagTypes().contains(entityType)) {
                sentenceHasEntities = true;
                if (entityTokens == null) {
                    entityTokens = new ArrayList<>();
                }
                entityTokens.add(new EntityToken(wordPos, word));

                int nextWordPos = wordPos + 1;
                if (nextWordPos < sentence.size()) {
                    word = sentence.get(nextWordPos);
                    if (entityType.equals(getEntityType(word))) {
                        wordPos = nextWordPos;
                        continue;
                    }
                }
                break;
            }

            if (CollectionUtils.size(entityTokens) > 0) {
                NlpTagSet tagSet = entities.computeIfAbsent(entityType, tagType -> new NlpTagSet(tagType, new TreeSet<String>(String.CASE_INSENSITIVE_ORDER)));
                String entity = getText(entityTokens);

                // Constructing the clique tree is relatively expensive, so we only want to do it if we absolutely
                // have to.  Hence the check here to ensure we haven't already added this entity before we
                // assess the confidence.
                if (!tagSet.getTags().contains(entity) || params.includeMatches()) {
                    if (params.getMinConfidencePercentage() > 0) {
                        cliqueTree = cliqueTree == null ? classifier.get().getCliqueTree(sentence) : cliqueTree;

                        if (getConfidencePercentage(cliqueTree, entityTokens) > params.getMinConfidencePercentage()) {
                            addEntity(tagSet, entityTokens);
                        }
                    } else {
                        // If minConfidence is zero, we can bypass the clique tree construction and confidence
                        // assessment, giving us a performance boost.
                        addEntity(tagSet, entityTokens);
                    }
                }
            }
        }

        if (sentenceHasEntities && sentence.size() < MAX_SENTENCE_TOKENS_FOR_OPENIE) {
            relationshipRecognizer.findRelationships(
                    previousSentence.size() < MAX_SENTENCE_TOKENS_FOR_OPENIE ? previousSentence : Collections.emptyList(),
                    sentence,
                    nextSentence.size() < MAX_SENTENCE_TOKENS_FOR_OPENIE ? nextSentence : Collections.emptyList());

        }
    }


    private void addEntity(NlpTagSet tagSet, List<EntityToken> entityTokens) {
        String entityText = getText(entityTokens);
        tagSet.getTags().add(entityText);

        if (params.includeMatches()) {
            if (tagSet.getMatchCollection() == null) {
                tagSet.setMatchCollection(new NlpMatchCollection());
            }
            tagSet.getMatchCollection().setTotal(tagSet.getMatchCollection().getTotal()+1);
            if (CollectionUtils.size(tagSet.getMatchCollection().getMatches()) < params.getMaxContentMatches()) {
                tagSet.getMatchCollection().getMatches().add(createMatch(entityTokens, entityText));
            }
        }
    }

    private NlpMatch createMatch(List<EntityToken> entityTokens, String entityText) {
        long offset = matchBaseOffset + entityTokens.get(0).coreLabel.beginPosition();
        long length = entityText.length();

        int contextStart = Math.max(0, entityTokens.get(0).coreLabel.beginPosition() - CONTEXT_BEFORE_CHARS);
        int contextEnd = Math.min(text.length(), entityTokens.get(entityTokens.size()-1).coreLabel.endPosition() + CONTEXT_AFTER_CHARS);
        String context = text.substring(contextStart, contextEnd);
        int contextOffset = entityTokens.get(0).coreLabel.beginPosition() - contextStart;

        return new NlpMatch(offset, length, entityText, context, contextOffset);
    }

    private NlpTagType getEntityType(CoreLabel coreLabel) {
        String entityTypeString = coreLabel.get(CoreAnnotations.AnswerAnnotation.class);
        return getEntityType(entityTypeString);
    }

    private NlpTagType getEntityType(String entityTypeString) {
        return Enums.getIfPresent(NlpTagType.class, entityTypeString).orNull();
    }

    private String getText(List<EntityToken> tokens) {
        return text.substring(
                tokens.get(0).coreLabel.beginPosition(),
                tokens.get(tokens.size()-1).coreLabel.endPosition()).trim();
    }

    private double getConfidencePercentage(CRFCliqueTree<String>cliqueTree, List<EntityToken> tokens) {
        // Each word in the entity has a confidence (which is a little strange).  We'll combine them through a simple
        // mean to get an overall confidence for the entity.
        double totalConfidence = 0.0;
        for (EntityToken token : tokens) {
            totalConfidence += cliqueTree.prob(token.pos, token.coreLabel.get(CoreAnnotations.AnswerAnnotation.class));
        }
        return (totalConfidence / (double)tokens.size()) * 100.0;
    }

    private static CRFClassifier<CoreLabel> createClassifier() {
        return CRFClassifier.getClassifierNoExceptions(NER_3_CLASSIFIER_PATH);
    }

    private static class EntityToken {
        EntityToken(int pos, CoreLabel coreLabel) {
            this.pos = pos;
            this.coreLabel = coreLabel;
        }

        final int pos;
        final CoreLabel coreLabel;
    }

}
