package edu.colorado.clear.wsd.feature.optim;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import edu.colorado.clear.wsd.classifier.Classifier;
import edu.colorado.clear.wsd.classifier.Hyperparameter;
import edu.colorado.clear.wsd.feature.optim.ga.GeneticAlgorithm;
import edu.colorado.clear.wsd.feature.pipeline.NlpClassifier;
import edu.colorado.clear.wsd.type.NlpInstance;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

/**
 * Model trainer searches for the optimal feature architecture
 *
 * @author jamesgung
 */
@Slf4j
@Accessors(fluent = true)
public class EvolutionaryModelTrainer<U extends NlpInstance> implements Classifier<U, String> {

    private static final long serialVersionUID = -1828098138845207546L;

    private transient GeneticAlgorithm<NlpClassifier<U>> ga;

    @Getter
    private NlpClassifier<U> classifier;

    public EvolutionaryModelTrainer(GeneticAlgorithm<NlpClassifier<U>> ga) {
        this.ga = ga;
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
        // find the optimal feature architecture through cross-validation on training data
        ga.run();
        // train on best genotype w/ full training set
        classifier = ga.best().phenotype();
        classifier.train(train, valid);
    }

    @Override
    public List<Hyperparameter> hyperparameters() {
        return classifier.hyperparameters();
    }

    @Override
    public void initialize(Properties properties) {
        classifier.initialize(properties);
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
