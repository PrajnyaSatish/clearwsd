/*
 * Copyright 2017 James Gung
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.clearwsd.feature.optim;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.github.clearwsd.feature.function.FeatureFunction;
import io.github.clearwsd.type.FeatureType;
import io.github.clearwsd.type.NlpInstance;
import io.github.clearwsd.classifier.Classifier;
import io.github.clearwsd.classifier.ClassifierFactory;
import io.github.clearwsd.classifier.DefaultHyperparameter;
import io.github.clearwsd.classifier.Hyperparameter;
import io.github.clearwsd.classifier.SparseClassifier;
import io.github.clearwsd.eval.CrossValidation;
import io.github.clearwsd.eval.CrossValidation.Fold;
import io.github.clearwsd.eval.Evaluation;
import io.github.clearwsd.feature.pipeline.DefaultFeaturePipeline;
import io.github.clearwsd.feature.pipeline.FeaturePipeline;
import io.github.clearwsd.feature.pipeline.NlpClassifier;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

/**
 * Model trainer searches for the optimal feature architecture
 *
 * @author jamesgung
 */
@Slf4j
@Accessors(fluent = true)
public class MetaModelTrainer<U extends NlpInstance> implements Classifier<U, String> {

    private static final long serialVersionUID = 6414091079202692687L;

    public enum MetaModelParameter implements Hyperparameter<MetaModelTrainer> {
        Multithread("use parallel model scoring", "true", (c, value) -> c.parallel = Boolean.valueOf(value)),
        Iterations("maximum number of epochs", "100", (c, value) -> c.iterations = Integer.valueOf(value)),
        Patience("number of epochs with no change in loss before early stopping", "50",
                (c, value) -> c.patience = Integer.valueOf(value)),
        Folds("number of cross validation folds per iteration", "5",
                (c, value) -> c.folds = Integer.valueOf(value)),
        TrainRatio("percentage of data to use in training per fold", "0.8",
                (c, value) -> c.ratio = Double.valueOf(value)),
        Seed("random seed for shuffling", "0", (c, value) -> c.seed = Integer.valueOf(value)),
        Verbose("display training logs", "true", (c, value) -> c.verbose = Boolean.valueOf(value));
        private Hyperparameter<MetaModelTrainer> parameter;

        MetaModelParameter(String parameter, String defaultValue, BiConsumer<MetaModelTrainer, String> assign) {
            this.parameter = new DefaultHyperparameter<>(this.name(),
                    MetaModelTrainer.class.getSimpleName() + ":" + name(), parameter, defaultValue, assign);
        }

        @Override
        public String key() {
            return parameter.key();
        }

        @Override
        public String description() {
            return parameter.description();
        }

        @Override
        public String defaultValue() {
            return parameter.defaultValue();
        }

        @Override
        public void assignValue(MetaModelTrainer model, String value) {
            parameter.assignValue(model, value);
        }
    }

    private final transient FeatureFunctionFactory<U> featureFactory;
    private final transient ClassifierFactory<SparseClassifier> classifierFactory;

    @Setter
    private int seed = 0;
    @Setter
    private int folds = 5;
    @Setter
    private double ratio = 0.8;
    @Setter
    private int iterations = 100;
    @Setter
    private int patience = 50;
    @Setter
    private double maxScore = 1.0;
    @Setter
    private boolean parallel = true;
    @Setter
    private boolean verbose = true;

    private NlpClassifier<U> classifier;

    public MetaModelTrainer(FeatureFunctionFactory<U> featureFactory,
                            ClassifierFactory<SparseClassifier> classifierFactory) {
        this.featureFactory = featureFactory;
        this.classifierFactory = classifierFactory;
    }

    @Override
    public String classify(U instance) {
        return classifier.classify(instance);
    }

    @Override
    public Map<String, Double> score(U instance) {
        return classifier.score(instance);
    }

    @Override
    public void train(List<U> train, List<U> valid) {
        CrossValidation<U> cv = new CrossValidation<>(seed, t -> t.feature(FeatureType.Gold.name()));
        // search for best feature function using cross-validation
        Evaluation best = new Evaluation();
        FeaturePipeline<U> result = null;
        List<Fold<U>> folds = cv.createFolds(train, this.folds, ratio);
        int epochsNoChange = 0;
        for (int i = 1; i <= iterations && epochsNoChange < patience; ++i) {
            FeatureFunction<U> featureFunction = featureFactory.create();
            NlpClassifier<U> classifier = new NlpClassifier<>(classifierFactory.create(),
                    new DefaultFeaturePipeline<>(featureFunction));

            Evaluation eval = new Evaluation(
                    parallel ? cv.crossValidateParallel(() -> new NlpClassifier<>(classifierFactory.create(),
                            new DefaultFeaturePipeline<>(featureFunction)), folds)
                            : cv.crossValidate(classifier, folds)
            );

            if (eval.f1() > best.f1() || result == null) {
                epochsNoChange = 0;
                best = eval;
                result = new DefaultFeaturePipeline<>(featureFunction);
                if (verbose) {
                    log.debug("Iteration {}/{} (F1: {})", i, iterations, new DecimalFormat("#.###").format(best.f1()));
                }
            } else {
                epochsNoChange++;
            }
            if (best.f1() >= maxScore) {
                break;
            }
        }
        // train final classifier using the top-scoring feature function
        classifier = new NlpClassifier<>(classifierFactory.create(), result);
        classifier.train(train, valid);
        if (verbose) {
            log.debug("Overall top score: {}\n{}", new DecimalFormat("#.###").format(best.f1()), best.toString());
        }
    }

    @Override
    public List<Hyperparameter> hyperparameters() {
        return Stream.of(Arrays.asList(MetaModelParameter.values()), classifier.hyperparameters())
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    @Override
    public void initialize(Properties properties) {
        classifier.initialize(properties);
        for (MetaModelParameter property : MetaModelParameter.values()) {
            if (properties.containsKey(property.name())) {
                property.assignValue(this, properties.getProperty(property.name()));
            } else {
                property.assignValue(this, property.defaultValue());
            }
        }
    }

    @Override
    public void load(ObjectInputStream inputStream) {
        try {
            //noinspection unchecked
            classifier = (NlpClassifier<U>) inputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void save(ObjectOutputStream outputStream) {
        try {
            outputStream.writeObject(classifier);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
