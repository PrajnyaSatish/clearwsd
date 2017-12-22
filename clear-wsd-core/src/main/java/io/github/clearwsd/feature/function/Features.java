package io.github.clearwsd.feature.function;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import io.github.clearwsd.feature.extractor.FeatureExtractor;
import io.github.clearwsd.feature.extractor.StringExtractor;
import io.github.clearwsd.feature.extractor.StringListExtractor;
import io.github.clearwsd.type.NlpInstance;
import io.github.clearwsd.feature.context.NlpContextFactory;
import io.github.clearwsd.feature.extractor.FeatureExtractor;
import io.github.clearwsd.feature.extractor.StringExtractor;
import io.github.clearwsd.feature.extractor.StringListExtractor;

/**
 * Feature function utilities.
 *
 * @author jamesgung
 */
public class Features {

    private Features() {
    }

    public static <I extends NlpInstance, T extends NlpInstance, U> FeatureFunction<I> function(
            NlpContextFactory<I, T> context, List<? extends FeatureExtractor<T, U>> extractors) {
        if (extractors.size() == 0) {
            throw new IllegalArgumentException("Feature function requires at least one extractor, got 0.");
        }
        FeatureExtractor<T, U> extractor = extractors.get(0);
        if (extractor instanceof StringExtractor) {
            List<StringExtractor<T>> stringExtractors = extractors.stream()
                    .map(e -> ((StringExtractor<T>) e))
                    .collect(Collectors.toList());
            return new StringFeatureFunction<>(context, stringExtractors);
        } else if (extractor instanceof StringListExtractor) {
            List<StringListExtractor<T>> stringListExtractors = extractors.stream()
                    .map(e -> ((StringListExtractor<T>) e))
                    .collect(Collectors.toList());
            return new MultiStringFeatureFunction<>(context, stringListExtractors);
        }
        throw new IllegalArgumentException("Unsupported extractor :" + extractors.getClass());
    }

    public static <I extends NlpInstance, T extends NlpInstance, U> FeatureFunction<I> function(NlpContextFactory<I, T> context,
                                                                                                FeatureExtractor<T, U> extractor) {
        return function(context, Collections.singletonList(extractor));
    }

    public static <T extends NlpInstance> ConjunctionFunction<T> cross(FeatureFunction<T> first, FeatureFunction<T> second) {
        return new ConjunctionFunction<>(first, second);
    }

    public static <T extends NlpInstance> ConjunctionFunction<T> cross(FeatureFunction<T> self) {
        return new ConjunctionFunction<>(self, self);
    }

    public static <T extends NlpInstance> BiasFeatureFunction<T> bias() {
        return new BiasFeatureFunction<>();
    }

}