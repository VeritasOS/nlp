package com.veritas.nlp.ner;

import com.google.common.base.Enums;
import com.google.common.base.Suppliers;
import com.veritas.nlp.models.NerEntityType;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ie.crf.CRFCliqueTree;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

class StanfordNER {
    private static final String NER_3_CLASSIFIER_PATH = "edu/stanford/nlp/models/ner/english.all.3class.distsim.crf.ser.gz";
    private static final Supplier<CRFClassifier<CoreLabel>> classifier = Suppliers.memoize(StanfordNER::createClassifier);

    Map<NerEntityType, Set<String>> getEntities(String text, EnumSet<NerEntityType> entityTypes, double minConfidence) {
        return getEntities(classifier.get().classify(text), entityTypes, minConfidence);
    }

    private static CRFClassifier<CoreLabel> createClassifier() {
        return CRFClassifier.getClassifierNoExceptions(NER_3_CLASSIFIER_PATH);
    }

    private Map<NerEntityType, Set<String>> getEntities(
            List<List<CoreLabel>> sentences, EnumSet<NerEntityType> entityTypes, double minConfidence) {

        Map<NerEntityType, Set<String>> entityMap = new EnumMap<>(NerEntityType.class);

        for (List<CoreLabel> sentence : sentences) {
            getEntitiesForSentence(entityMap, sentence, entityTypes, minConfidence);
        }

        return entityMap;
    }

    private void getEntitiesForSentence(Map<NerEntityType, Set<String>> entityMap, List<CoreLabel> sentence,
                                        EnumSet<NerEntityType> entityTypes, double minConfidence) {

        // The API for extraction of the named entities is a bit strange.  We have to look for sequences of words
        // with particular entity type.  For example a sequence of three words of entity type PERSON is considered a
        // single person.
        //
        // There is a simpler alternative API (classifyToCharacterOffsets) that returns whole entities directly,
        // but it is not ideal because it doesn't normalize whitespace characters.  This way we get the individual
        // words separately so we don't have to deal with whitespace at all.
        //

        CRFCliqueTree<String> cliqueTree = null;

        for (int wordPos = 0; wordPos < sentence.size(); wordPos++) {
            CoreLabel word = sentence.get(wordPos);

            List<EntityToken> entityTokens = null;
            NerEntityType entityType = getEntityType(word);

            while (entityTypes.contains(entityType)) {
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
                Set<String> entitySet = entityMap.computeIfAbsent(entityType, k -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER));
                String entity = getText(entityTokens).trim();

                if (!entitySet.contains(entity)) {
                    if (minConfidence > 0) {
                        // Constructing the clique tree is expensive, so we only want to do it if we absolutely
                        // have to.  Hence the check here to ensure we haven't already added this entity before we
                        // assess the confidence.
                        if (cliqueTree == null) {
                            cliqueTree = classifier.get().getCliqueTree(sentence);
                        }
                        if (getConfidence(cliqueTree, entityTokens) > minConfidence) {
                            entitySet.add(entity);
                        }
                    } else {
                        // If minConfidence is zero, we can bypass the clique tree construction and confidence
                        // assessment, giving us a (modest) performance boost.
                        entitySet.add(entity);
                    }
                }
            }
        }
    }

    private NerEntityType getEntityType(CoreLabel coreLabel) {
        String entityTypeString = coreLabel.get(CoreAnnotations.AnswerAnnotation.class);
        return Enums.getIfPresent(NerEntityType.class, entityTypeString).orNull();
    }

    private String getText(List<EntityToken> tokens) {
        return tokens.stream()
                .map(entityToken -> entityToken.coreLabel.originalText())
                .collect(Collectors.joining(" "));
    }

    private double getConfidence(CRFCliqueTree<String>cliqueTree, List<EntityToken> tokens) {
        // Each word in the entity has a confidence (which is a little strange).  We'll combine them through a simple
        // mean to get an overall confidence for the entity.
        double totalConfidence = 0.0;
        for (EntityToken token : tokens) {
            totalConfidence += cliqueTree.prob(token.pos, token.coreLabel.get(CoreAnnotations.AnswerAnnotation.class));
        }
        return totalConfidence / (double)tokens.size();
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
