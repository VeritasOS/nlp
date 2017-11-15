package com.veritas.nlp.ner;

import com.google.common.base.Enums;
import com.google.common.base.Suppliers;
import com.veritas.nlp.models.NerEntityType;
import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;


class StanfordNLP {
    private static final String NER_3_CLASSIFIER_PATH = "edu/stanford/nlp/models/ner/english.all.3class.distsim.crf.ser.gz";
    private static final Supplier<AbstractSequenceClassifier<CoreLabel>> classifier = Suppliers.memoize(StanfordNLP::createClassifier);

    Map<NerEntityType, Set<String>> getEntities(String text, EnumSet<NerEntityType> entityTypes) {
        return getEntities(classifier.get().classify(text), entityTypes);
    }

    private static AbstractSequenceClassifier<CoreLabel> createClassifier() {
        return CRFClassifier.getClassifierNoExceptions(NER_3_CLASSIFIER_PATH);
    }

    private Map<NerEntityType, Set<String>> getEntities(List<List<CoreLabel>> sentences, EnumSet<NerEntityType> entityTypes) {

        // The API for extraction of the named entities is a bit strange.  We have to look for sequences of words
        // with particular entity type.  For example a sequence of three words of entity type PERSON is considered a
        // single person.
        //
        // There is a simpler alternative API (classifyToCharacterOffsets) that returns whole entities directly,
        // but it is not ideal because it doesn't normalize whitespace characters.  This way we get the individual
        // words separately so we don't have to deal with whitespace at all.
        //
        Map<NerEntityType, Set<String>> entityMap = new EnumMap<>(NerEntityType.class);

        for (List<CoreLabel> sentence : sentences) {

            for (int wordPos = 0; wordPos < sentence.size(); wordPos++) {
                CoreLabel word = sentence.get(wordPos);

                StringBuilder entity = null;
                NerEntityType entityType = getEntityType(word);

                while (entityTypes.contains(entityType)) {
                    if (entity == null) {
                        entity = new StringBuilder();
                    }
                    entity.append(word.originalText());

                    int nextWordPos = wordPos + 1;
                    if (nextWordPos < sentence.size()) {
                        word = sentence.get(nextWordPos);
                        if (entityType.equals(getEntityType(word))) {
                            wordPos = nextWordPos;
                            entity.append(' ');
                            continue;
                        }
                    }
                    break;
                }

                if (entity != null && entity.length() > 0) {
                    Set<String> entitySet = entityMap.computeIfAbsent(entityType, k -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER));
                    entitySet.add(entity.toString().trim());
                }
            }
        }

        return entityMap;
    }

    private NerEntityType getEntityType(CoreLabel coreLabel) {
        String entityTypeString = coreLabel.get(CoreAnnotations.AnswerAnnotation.class);
        return Enums.getIfPresent(NerEntityType.class, entityTypeString).orNull();
    }
}
