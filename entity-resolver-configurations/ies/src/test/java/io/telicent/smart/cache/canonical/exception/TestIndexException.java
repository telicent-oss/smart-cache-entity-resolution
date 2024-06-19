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
package io.telicent.smart.cache.canonical.exception;

import org.testng.Assert;
import org.testng.annotations.Test;

public class TestIndexException {
    @Test
    public void test_exception_message() {
        // given
        // when
        IndexException indexException = new IndexException("Error Message");
        // then
        Assert.assertEquals(indexException.getMessage(), "Error Message");
        Assert.assertNull(indexException.getCause());
    }

    @Test
    public void test_exception_cause() {
        // given
        // when
        IndexException indexException = new IndexException(new RuntimeException());
        // then
        Assert.assertEquals(indexException.getMessage(), "java.lang.RuntimeException");
        Assert.assertNotNull(indexException.getCause());
    }

    @Test
    public void test_exception_message_and_cause() {
        // given
        // when
        IndexException indexException = new IndexException("Error Message", new RuntimeException());
        // then
        Assert.assertEquals(indexException.getMessage(), "Error Message");
        Assert.assertNotNull(indexException.getCause());
    }
}
