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
package io.telicent.smart.cache.search.security;

import org.apache.commons.codec.digest.DigestUtils;

import java.nio.charset.StandardCharsets;

/**
 * Provides a minimalist cache interface for querying and storing whether a document is visible to a given user allowing
 * {@link SecureSearchContext} to short circuit security label filtering for documents we already know are not visible
 * to the current user
 */
public interface RedactedDocumentsCache extends AutoCloseable {

    /**
     * Creates the key for a document for looking up in a cache
     * <p>
     * The created key value takes into account the Document ID, Version and any Type Filtering options since a document
     * may be visible without type filtering but not visible with it.
     * </p>
     * <p>
     * The resultant key is SHA256 hashed to provide a consistent length key that guarantees a predictable memory
     * footprint for the cache regardless of the size of the contributing parameters to the key.
     * </p>
     *
     * @param context Search context
     * @param id      Document ID
     * @param version Document Version
     * @return Cache Key
     */
    static String keyForDocument(SecureSearchContext context, String id, String version) {
        StringBuilder builder = new StringBuilder();
        builder.append(id)
               .append('_')
               .append(version)
               .append('_')
               .append(context.getSearchOptions().getTypeFilterOpts().getTypeFilter());
        return DigestUtils.sha256Hex(builder.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Invalidates the entire cache
     */
    void invalidate();

    /**
     * Gets whether a document with the given ID and version is visible in the given search context
     *
     * @param context Search context
     * @param id      Document ID
     * @param version Document Version
     * @return {@code true} if visible, {@code false} if redacted, {@code null} if visibility is not cached
     */
    Boolean isVisible(SecureSearchContext context, String id, String version);

    /**
     * Sets whether a document with the given ID and version should be visible in the given search context
     * <p>
     * This is used to cache the results of freshly calculated visibility decisions so those can be reused to speed up
     * document filtering on future requests.
     * </p>
     *
     * @param context    Search context
     * @param id         Document ID
     * @param version    Document Version
     * @param visibility Whether the document is visible in the given context
     */
    void setVisible(SecureSearchContext context, String id, String version, boolean visibility);

    @Override
    void close();
}
