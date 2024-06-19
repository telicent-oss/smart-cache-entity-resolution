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
package io.telicent.smart.cache.cli.commands.projection.search.elastic.canonical;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static io.telicent.smart.cache.canonical.utility.Mapper.getJsonMapper;

/**
 * A class for deserializing Canonical Forms
 */
public class CanonicalFormMapDeserializer implements Deserializer<CanonicalFormMap> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CanonicalFormMapDeserializer.class);

    @Override
    public CanonicalFormMap deserialize(String topic, byte[] data) {
        try {
            return new CanonicalFormMap(getJsonMapper().readValue(data, new TypeReference<>() {}));
        } catch (IOException e) {
            LOGGER.warn("Malformed record encountered: {}", e.getMessage());
            return null;
        }
    }
}
