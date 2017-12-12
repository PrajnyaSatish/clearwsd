package edu.colorado.clear.wsd.feature.extractor;

import edu.colorado.clear.wsd.type.NlpInstance;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * Base feature extractor.
 *
 * @author jamesgung
 */
public abstract class NlpFeatureExtractor<T extends NlpInstance, S> implements FeatureExtractor<T, S> {

    private static final long serialVersionUID = 7179291394532057198L;

    @Getter
    @Accessors(fluent = true)
    protected String id;

    public NlpFeatureExtractor() {
        this.id = this.getClass().getSimpleName();
    }
}