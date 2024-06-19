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
package io.telicent.smart.cache.canonical.utility;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.telicent.smart.cache.canonical.exception.ValidationException;
import org.mockito.Mockito;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static io.telicent.smart.cache.canonical.utility.Mapper.*;
import static org.mockito.ArgumentMatchers.any;
import static org.testng.Assert.*;

public class TestMapper {

    @Test
    public void test_writeToString_null() {
        // given
        String expected = "null";
        setJsonMapper(new ObjectMapper());
        // when
        String actual = writeValueAsString(null);
        // then
        assertEquals(actual, expected);
    }

    @Test(expectedExceptions = ValidationException.class)
    public void test_handleException() throws JsonProcessingException {
        // given
        ObjectMapper mockMapper = Mockito.mock(ObjectMapper.class);
        setJsonMapper(mockMapper);
        Mockito.when(mockMapper.writeValueAsString(any())).thenThrow(JsonProcessingException.class);
        // when
        // then
        writeValueAsString("doesn't matter");
    }

    @Test
    public void testUpdateFields() {
        String jsonString = "{\"longField\": 123, \"stringField\": \"test\", \"intField\": 42, \"booleanField\": true, " +
                "\"listField\": [\"a\", \"b\", \"c\"], \"mapField\": {\"key1\": \"value1\", \"key2\": \"value2\"}}";

        TestClass obj = updateFieldsFromJSON(new TestClass(), jsonString);

        assertEquals(123L, obj.longField);
        assertEquals("test", obj.stringField);
        assertEquals(42, obj.intField);
        assertTrue(obj.booleanField);
        assertEquals(Arrays.asList("a", "b", "c"), obj.listField);
        assertEquals("value1", obj.mapField.get("key1"));
        assertEquals("value2", obj.mapField.get("key2"));
    }

    @Test(expectedExceptions = ValidationException.class)
    public void testUpdateFieldsWithInvalidJSON() {
        String invalidJsonString = "{\"invalidJson\": }";

        TestClass obj = new TestClass();
        updateFieldsFromJSON(obj, invalidJsonString);
    }

    @Test
    public void testUpdateFieldsWithNonExistentField() {
        String jsonString = "{\"nonExistentField\": \"value\"}";

        TestClass obj = updateFieldsFromJSON( new TestClass(), jsonString);

        // Ensure that non-existent fields do not cause any update
        assertNull(obj.stringField);
    }


    // Example class for testing
    public static class TestClass {
        public Long longField;
        public String stringField;
        public int intField;
        public boolean booleanField;
        public List<String> listField;
        public Map<String, String> mapField;
        private String privateField;
    }


    @Test
    public void test_updateFieldsFromJSON() {
        // given
        setJsonMapper(null);
        TestFieldsClass testFieldsClass = new TestFieldsClass();
        // when
        String JSON = "{\"field_1\":\"value_1\",\"field_2\":42,\"field_3\":3.14,\"field_4\":true,\"field_5\":10000000,\"field_6\":32767,\"field_7\":\"a\",\"field_8\":123,\"field_9\":3.14159,\"field_10\":false,\"field_11\":9999999999,\"field_12\":-1}\n";
        // then
        TestFieldsClass result = updateFieldsFromJSON(testFieldsClass, JSON);
        String actual = writeValueAsString(result);
        String expected = "{\"field_1\":\"value_1\",\"field_2\":42,\"field_3\":3.14,\"field_4\":true,\"field_5\":10000000,\"field_6\":32767,\"field_7\":\"a\",\"field_8\":123,\"field_9\":3.14159,\"field_10\":false,\"field_11\":9999999999,\"field_12\":-1}";
        // then
        assertEquals(actual, expected);

    }

    static class TestFieldsClass {
        public String field_1;
        public int field_2;
        public double field_3;
        public boolean field_4;
        public long field_5;
        public short field_6;
        public char field_7;
        public Integer field_8;
        public Double field_9;
        public Boolean field_10;
        public Long field_11;
        public Short field_12;
    }
}
