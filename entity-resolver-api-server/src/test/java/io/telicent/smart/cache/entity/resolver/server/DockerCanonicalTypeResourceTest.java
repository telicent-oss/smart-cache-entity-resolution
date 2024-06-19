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

public class DockerCanonicalTypeResourceTest  extends AbstractConfigurationResourceTests {
    @Override
    public String getType() {
        return "canonicaltype";
    }

    @Override
    public String getEntry() {
        return "{\"fields\":[{\"name\":\"name\",\"type\":\"text\",\"required\":true},{\"name\":\"village\",\"type\":\"text\",\"required\":true},{\"name\":\"city\",\"type\":\"text\",\"required\":true},{\"name\":\"country\",\"type\":\"text\",\"required\":false},{\"name\":\"location\",\"type\":\"geo-point\",\"required\":false}]}";
    }

    @Override
    public String getExpectedResult() {
        return "{\"type\":\"\",\"index\":\"\",\"fields\":[{\"name\":\"name\",\"type\":\"text\",\"required\":true,\"boost\":1.0,\"exactMatch\":false,\"fuzziness\":null},{\"name\":\"village\",\"type\":\"text\",\"required\":true,\"boost\":1.0,\"exactMatch\":false,\"fuzziness\":null},{\"name\":\"city\",\"type\":\"text\",\"required\":true,\"boost\":1.0,\"exactMatch\":false,\"fuzziness\":null},{\"name\":\"country\",\"type\":\"text\",\"required\":false,\"boost\":1.0,\"exactMatch\":false,\"fuzziness\":null},{\"name\":\"location\",\"type\":\"geo-point\",\"required\":false,\"boost\":1.0,\"exactMatch\":false,\"distance\":null}]}";
    }

    @Override
    public String getUpdatedEntry() {
        return "{\"fields\":[{\"name\":\"name\",\"type\":\"text\",\"required\":false},{\"name\":\"village\",\"type\":\"text\",\"required\":false},{\"name\":\"city\",\"type\":\"text\",\"required\":false},{\"name\":\"country\",\"type\":\"text\",\"required\":true},{\"name\":\"location\",\"type\":\"text\",\"required\":true}]}";
    }

    @Override
    public String getExpectedUpdatedResult() {
        return "{\"type\":\"\",\"index\":\"\",\"fields\":[{\"name\":\"name\",\"type\":\"text\",\"required\":false,\"boost\":1.0,\"exactMatch\":false,\"fuzziness\":null},{\"name\":\"village\",\"type\":\"text\",\"required\":false,\"boost\":1.0,\"exactMatch\":false,\"fuzziness\":null},{\"name\":\"city\",\"type\":\"text\",\"required\":false,\"boost\":1.0,\"exactMatch\":false,\"fuzziness\":null},{\"name\":\"country\",\"type\":\"text\",\"required\":true,\"boost\":1.0,\"exactMatch\":false,\"fuzziness\":null},{\"name\":\"location\",\"type\":\"text\",\"required\":true,\"boost\":1.0,\"exactMatch\":false,\"fuzziness\":null}]}";
    }
}
