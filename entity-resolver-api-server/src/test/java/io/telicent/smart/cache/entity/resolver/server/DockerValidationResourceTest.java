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

import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

import static io.telicent.smart.cache.search.clusters.test.SearchTestClusters.DEFAULT_TEST_SIMILARITY_INDEX;

public class DockerValidationResourceTest extends AbstractEntityResolutionApiDockerTests {

    @Test
    public void test_validate_missingIndex() {
        WebTarget target = forApiServer("validate/unknown_type/missing_id/invalid_index");
        try (Response response = target.request().get()) {
            Assert.assertEquals(response.getStatus(), 200);
            String results = response.readEntity(String.class);
            Assert.assertEquals(results, "");
        }
    }

    @Test
    public void test_validate_missingID() {
        WebTarget target = forApiServer("validate/unknown_type/missing_id/" + DEFAULT_TEST_SIMILARITY_INDEX);
        try (Response response = target.request().get()) {
            Assert.assertEquals(response.getStatus(), 200);
            String results = response.readEntity(String.class);
            Assert.assertEquals(results, "");
        }
    }
}
