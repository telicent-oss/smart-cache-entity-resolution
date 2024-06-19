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

public class DockerFullModelResourceTest extends AbstractConfigurationResourceTests {
    @Override
    public String getType() {
        return "fullmodel";
    }

    @Override
    public String getEntry() {
        return "{\"modelId\":\"" + UNIQUE_ID + "\",\"indexes\":[],\"relations\":[],\"scorers\":[]}";
    }

    @Override
    public String getExpectedResult() {
            return "{\"modelId\":\"" + UNIQUE_ID + "\",\"indexes\":[],\"relations\":[],\"scorers\":[]}";
    }

    @Override
    public String getUpdatedEntry() {
        return "{\"modelId\":\"" + UNIQUE_ID + "\",\"indexes\":[\"canonical_index\"]}";
    }

    @Override
    public String getExpectedUpdatedResult() {
        return "";
    }

    @Override
    public void test_getCall_noEntry() {
        // NO-OP
    }

    @Override
    public void test_deleteCall_noEntry() {
        // NO-OP
    }

    @Override
    public void test_putCall() {
        // NO-OP
    }

}
