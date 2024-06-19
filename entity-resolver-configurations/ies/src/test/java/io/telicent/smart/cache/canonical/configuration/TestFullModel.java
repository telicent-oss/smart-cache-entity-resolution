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
package io.telicent.smart.cache.canonical.configuration;

import io.telicent.smart.cache.canonical.exception.ValidationException;
import org.apache.commons.lang3.StringUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestFullModel {

    static final String FULL_MODEL_HAPPY = """
            {
                "modelId": "testcase",
                "indexes": [
                    "canonical_index"
                ],
                "relations": [
                    {
                        "resolverId": "testcase",
                        "fields": [
                            "field_1",
                            "field_2"
                        ],
                        "weight": 4
                    }
                ],
                "scorers": [
                    {
                        "fieldScores": {
                            "name": 0.9,
                            "village": 0.8,
                            "city": 0.7,
                            "country": 0.6,
                            "location": 1.0
                        },
                        "scorerId": "testcase"
                    }
                ]
            }""";
    @Test
    public void test_toString_empty() {
        // given
        String expectedString = "{\"modelId\":null,\"indexes\":[],\"relations\":[],\"scorers\":[]}";
        FullModel fullModel = new FullModel();
        // when
        String actualString = fullModel.toString();
        FullModel fullModelReloaded = FullModel.loadFromString(actualString);
        // then
        Assert.assertNotNull(fullModelReloaded);
        Assert.assertEquals(actualString, expectedString);
    }

    @Test(expectedExceptions = ValidationException.class)
    public void test_loadFromString() {
        // given
        // when
        // then
        FullModel.loadFromString("Something else");
    }

    @Test
    public void test_loadFromString_happyPath() {
        // given
        // when
        FullModel fullModel = FullModel.loadFromString(FULL_MODEL_HAPPY);
        // then
        Assert.assertEquals(StringUtils.deleteWhitespace(fullModel.toString()), StringUtils.deleteWhitespace(FULL_MODEL_HAPPY));
    }
}
