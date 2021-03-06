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

package io.github.clearwsd.feature.extractor.string;

import java.io.Serializable;
import java.util.function.Function;

import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * String function with an ID.
 *
 * @author jamesgung
 */
public abstract class StringFunction implements Function<String, String>, Serializable {

    private static final long serialVersionUID = -5951502410231318293L;

    @Getter
    @Accessors(fluent = true)
    protected String id;

}
