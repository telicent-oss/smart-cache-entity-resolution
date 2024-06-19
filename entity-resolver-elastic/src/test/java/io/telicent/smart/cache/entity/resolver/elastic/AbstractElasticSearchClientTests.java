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
package io.telicent.smart.cache.entity.resolver.elastic;

import io.telicent.jena.abac.attributes.AttributeExpr;
import org.apache.jena.atlas.lib.Cache;
import org.apache.jena.atlas.lib.CacheFactory;

import java.util.List;

public abstract class AbstractElasticSearchClientTests extends AbstractDockerElasticSearchTests {

    static final Cache<String, List<AttributeExpr>> labelsToExpressions = CacheFactory.createCache(1000);

    @Override
    public void elasticCleanIndex() {
        // Do nothing, don't want to clean the index on every single test because then we waste a lot of time
        // repopulating the index with our test data
    }
}
