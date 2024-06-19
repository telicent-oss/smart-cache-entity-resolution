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
package io.telicent.smart.cache.search.elastic.utils;

import co.elastic.clients.elasticsearch._types.Script;
import co.elastic.clients.json.JsonData;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.shaded.org.apache.commons.lang3.StringUtils;
import org.testng.Assert;

import java.util.Map;

public class AbstractContentModificationTests {
    public static final String SOURCE_ACCESSED = "ctx._source";

    @NotNull
    protected static String verifyScriptWasGenerated(Script script) {
        String generatedScript = script.inline().source();
        Assert.assertNotNull(generatedScript);
        Assert.assertFalse(StringUtils.isBlank(generatedScript));
        return generatedScript;
    }

    protected static void verifyEmptyScriptGenerated(Script script) {
        Assert.assertTrue(StringUtils.isBlank(script.inline().source()),
                          "Expected an empty script to be generated but found a script of " + script.inline()
                                                                                                    .source()
                                                                                                    .length() + "characters instead");
    }

    protected void verifyScriptHasParameters(Script script, String... expectedParameters) {
        Map<String, JsonData> params = script.inline().params();
        for (String expectedParam : expectedParameters) {
            Assert.assertTrue(params.containsKey(expectedParam));
        }
    }

    protected void verifyScriptContent(String generatedScript, String... expectedContents) {
        for (String expectedContent : expectedContents) {
            Assert.assertTrue(StringUtils.contains(generatedScript, expectedContent),
                              "Generated script missing expected content: " + expectedContent + "\nActual script:\n" + generatedScript);
        }
    }
}
