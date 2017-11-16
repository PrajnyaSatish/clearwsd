package edu.colorodo.clear.wsd.feature.resource;

import java.io.InputStream;

/**
 * Interface for lookup-based resources.
 *
 * @param <K> key type
 * @param <T> lookup value type, such as {@link String}.
 * @author jamesgung
 */
public interface FeatureResource<K, T> {

    /**
     * Unique identifier of this resource.
     */
    String key();

    /**
     * Lookup a value associated with a key.
     *
     * @param key lookup key
     * @return lookup value
     */
    T lookup(K key);

    /**
     * Initialize this resource.
     *
     * @param inputStream resource
     */
    void initialize(InputStream inputStream);

}
