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

public class DockerRelationsResourceTest extends AbstractConfigurationResourceTests {
    @Override
    public String getType() {
        return "relations";
    }

    @Override
    public String getEntry() {
        return "{\"id\":\"" + UNIQUE_ID + "\",\"fields\":[\"field_1\",\"field_2\"],\"weight\":5}";
    }

    @Override
    public String getExpectedResult() {
        return "{\"id\":\"" + UNIQUE_ID + "\",\"fields\":[\"field_1\",\"field_2\"],\"weight\":5}";
    }

    @Override
    public String getUpdatedEntry() {
        return "{\"id\":\"" + UNIQUE_ID + "\",\"fields\":[\"field_1\",\"field_2\"],\"weight\":10}";
    }

    @Override
    public String getExpectedUpdatedResult() {
        return "{\"id\":\"" + UNIQUE_ID + "\",\"fields\":[\"field_1\",\"field_2\"],\"weight\":10}";
    }

    @Override
    public int getInvalidStatusCode() {
        return 400;
    }
}
