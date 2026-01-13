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

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;

public class DockerFullModelResourceTest extends AbstractConfigurationResourceTests {
    @Override
    public String getType() {
        return "fullmodel";
    }

    @Override
    public String getPathType() {
        return "full-models";
    }

    @Override
    public String getEntry() {
        return "{\"index\":\"\",\"relations\":[],\"scores\":null}";
    }

    @Override
    public String getExpectedResult() {
            return "{\"id\":\"" + UNIQUE_ID + "\",\"index\":\"\",\"relations\":[],\"scores\":null}";
    }

    @Override
    public String getUpdatedEntry() {
        return "{\"index\":\"canonical_index\"}";
    }

    @Override
    public String getExpectedUpdatedResult() {
        return "{\"id\":\"" + UNIQUE_ID + "\",\"index\":\"canonical_index\",\"relations\":[],\"scores\":null}";
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

    @Override
    public void test_getCall_validate_noEntry() {
        // NO-OP
    }

    @Override
    @Test
    public void test_postInvalidCall() throws IOException {
        WebTarget target = forApiServer("/config/" + getPathType() + "/" + UNIQUE_ID);
        try (Response response = target.request()
                                       .post(Entity.entity("{\"rubbish\":\"test\"}",
                                                           MediaType.APPLICATION_JSON_TYPE))) {
            Assert.assertEquals(response.getStatus(), 200);
            String results = response.readEntity(String.class);
            Assert.assertEquals(results, "Created " + UNIQUE_ID);
        }
    }

}
