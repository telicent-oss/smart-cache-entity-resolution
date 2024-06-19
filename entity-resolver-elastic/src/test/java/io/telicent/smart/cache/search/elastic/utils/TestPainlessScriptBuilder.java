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

import co.elastic.clients.json.JsonData;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.Set;

public class TestPainlessScriptBuilder {

    @Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = "Parameter a.*not been defined.*")
    public void painless_builder_bad_01() {
        PainlessScriptBuilder builder = new PainlessScriptBuilder();
        builder.useParameter("a");
    }

    @Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = "Function test.*is already defined.*")
    public void painless_builder_bad_02() {
        PainlessScriptBuilder builder = new PainlessScriptBuilder();
        builder.defineFunction("test", "test");
        builder.defineFunction("test", "other");
    }

    @Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = ".*function test.*not been defined.*")
    public void painless_builder_bad_03() {
        PainlessScriptBuilder builder = new PainlessScriptBuilder();
        builder.invokeFunction("test");
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "No path to field.*")
    public void painless_builder_bad_04() {
        PainlessScriptBuilder builder = new PainlessScriptBuilder();
        builder.addField("test");
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "No path to field.*")
    public void painless_builder_bad_04b() {
        PainlessScriptBuilder builder = new PainlessScriptBuilder();
        builder.addField("test");
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "No path to field.*")
    public void painless_builder_bad_04c() {
        PainlessScriptBuilder builder = new PainlessScriptBuilder();
        builder.addField("test", (String[]) null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "No path to field.*")
    public void painless_builder_bad_05() {
        PainlessScriptBuilder builder = new PainlessScriptBuilder();
        builder.deleteField();
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "No path to field.*")
    public void painless_builder_bad_05b() {
        PainlessScriptBuilder builder = new PainlessScriptBuilder();
        builder.deleteField((String[]) null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "No path to field.*")
    public void painless_builder_bad_05c() {
        PainlessScriptBuilder builder = new PainlessScriptBuilder();
        builder.deleteField();
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "No path to field.*")
    public void painless_builder_bad_06() {
        PainlessScriptBuilder builder = new PainlessScriptBuilder();
        builder.accessField(new String[0]);
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "No path to field.*")
    public void painless_builder_bad_06b() {
        PainlessScriptBuilder builder = new PainlessScriptBuilder();
        builder.accessField(null);
    }

    @Test
    public void painless_parameter_assignment_01() {
        PainlessScriptBuilder builder = new PainlessScriptBuilder();
        String param1 = builder.assignNewParameter(JsonData.of(1));
        String param2 = builder.assignNewParameter(JsonData.of(2));
        String param3 = builder.assignNewParameter(JsonData.of(3));
        Assert.assertEquals(param1, "a");
        Assert.assertEquals(param2, "b");
        Assert.assertEquals(param3, "c");
    }

    @Test
    public void painless_parameter_assignment_02() {
        PainlessScriptBuilder builder = new PainlessScriptBuilder();
        Set<String> assigned = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            Assert.assertTrue(assigned.add(builder.assignNewParameter(JsonData.of(i))),
                              "Every call to assignNewParameter should yield a unique parameter name");
        }
    }

    @Test
    public void painless_parameter_assignment_03() {
        PainlessScriptBuilder builder = new PainlessScriptBuilder();
        JsonData value = JsonData.of(1234);
        builder.addParameter("test", value);
        Assert.assertTrue(builder.getParameters().containsKey("test"));
        Assert.assertEquals(builder.getParameters().get("test"), value);
    }

    @Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = "Parameter test.*already defined.*")
    public void painless_parameter_assignment_04() {
        PainlessScriptBuilder builder = new PainlessScriptBuilder();
        JsonData value = JsonData.of(1234);
        builder.addParameter("test", value);
        Assert.assertTrue(builder.getParameters().containsKey("test"));
        Assert.assertEquals(builder.getParameters().get("test"), value);

        // Should be illegal to set the parameter again regardless of value
        builder.addParameter("test", JsonData.of(5678));
    }
}
