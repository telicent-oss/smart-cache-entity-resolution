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
package io.telicent.smart.cache.entity.resolver.server;

public class DockerCanonicalTypeResourceWithNoBackendTest extends AbstractConfigurationResourcesWithNoBackend {
    @Override
    public String getType() {
        return "canonicaltype";
    }

    @Override
    public String getEntry() {
        return "{\"type\":\"CoreCanonicalTestType\",\"index\":\"\",\"fields\":[{\"name\":\"text-field-fuzzy\",\"type\":\"text\",\"required\":true,\"boost\":1.2,\"exactMatch\":false,\"fuzziness\":{\"enabled\":true,\"min\":0,\"max\":3}},{\"name\":\"text-field-nonfuzzy\",\"type\":\"text\",\"required\":true,\"boost\":4.0,\"exactMatch\":false,\"fuzziness\":{\"enabled\":false,\"min\":null,\"max\":null}},{\"name\":\"keyword-field-not-required\",\"type\":\"keyword\",\"required\":false,\"boost\":0.1,\"exactMatch\":false},{\"name\":\"keyword-field-no-boost\",\"type\":\"keyword\",\"required\":true,\"boost\":1.0,\"exactMatch\":false},{\"name\":\"age\",\"type\":\"number\",\"required\":true,\"boost\":5.0,\"exactMatch\":false,\"decay\":{\"decay\":0.5,\"offset\":\"10\",\"scale\":\"10\"}},{\"name\":\"date-field\",\"type\":\"date\",\"required\":true,\"boost\":1.5,\"exactMatch\":false,\"distance\":{\"pivot\":\"3d\"}},{\"name\":\"date-field-no-distance\",\"type\":\"number\",\"required\":false,\"boost\":1.5,\"exactMatch\":false,\"decay\":null},{\"name\":\"location-field\",\"type\":\"geo-point\",\"required\":false,\"boost\":10.0,\"exactMatch\":false,\"distance\":{\"pivot\":\"5m\"}},{\"name\":\"location-field-no-distance\",\"type\":\"geo-point\",\"required\":false,\"boost\":1.5,\"exactMatch\":false,\"distance\":null}]}";
    }
}
