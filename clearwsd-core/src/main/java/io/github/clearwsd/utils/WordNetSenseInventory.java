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

package io.github.clearwsd.utils;

import java.io.Serializable;
import java.util.Set;

/**
 * WordNet-based sense inventory.
 *
 * @author jamesgung
 */
public abstract class WordNetSenseInventory<T> implements SenseInventory<T>, Serializable {

    private static final long serialVersionUID = -5075422456841090098L;

    private transient WordNetFacade<T> wordNet;

    /**
     * Initialize WordNet implementation backing this sense inventory.
     *
     * @return initialized {@link WordNetFacade}
     */
    protected abstract WordNetFacade<T> initializeWordNet();

    @Override
    public Set<String> senses(String lemma) {
        if (wordNet == null) {
            wordNet = initializeWordNet();
        }
        return wordNet.senses(lemma, "VB");
    }

    @Override
    public String defaultSense(String lemma) {
        if (wordNet == null) {
            wordNet = initializeWordNet();
        }
        return wordNet.mfs(lemma, "VB").orElse("");
    }

    @Override
    public void addSense(String lemma, String sense) {
        // pass, do not update sense inventory
    }

    @Override
    public T getSense(String id) {
        return wordNet.lookup(id).orElse(null);
    }

}
