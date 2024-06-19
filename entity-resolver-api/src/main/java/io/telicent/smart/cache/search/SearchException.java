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
package io.telicent.smart.cache.search;

/**
 * Represents an exception occurring during search related operations
 */
public class SearchException extends RuntimeException {

    /**
     * Creates a new search exception
     *
     * @param message Message
     */
    public SearchException(String message) {
        super(message);
    }

    /**
     * Creates a new search exception
     *
     * @param message Message
     * @param cause   Cause
     */
    public SearchException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new search exception
     *
     * @param cause Cause
     */
    public SearchException(Throwable cause) {
        super(cause);
    }
}
