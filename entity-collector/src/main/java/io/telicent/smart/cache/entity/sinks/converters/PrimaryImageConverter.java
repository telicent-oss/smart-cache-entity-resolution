/**
 *   Copyright (c) Telicent Ltd.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package io.telicent.smart.cache.entity.sinks.converters;

import io.telicent.smart.cache.entity.vocabulary.Telicent;

/**
 * An Entity Output Converter that simply adds an output field conveying the primary image of the entity
 */
public class PrimaryImageConverter extends AbstractPrimaryLiteralConverter {

    /**
     * Creates a new converter using the default output field name {@value DefaultOutputFields#PRIMARY_NAME}
     */
    public PrimaryImageConverter() {
        super(DefaultOutputFields.PRIMARY_IMAGE, Telicent.PRIMARY_IMAGE);
    }

}
