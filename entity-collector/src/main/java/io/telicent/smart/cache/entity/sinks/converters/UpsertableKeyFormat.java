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
package io.telicent.smart.cache.entity.sinks.converters;

import org.apache.commons.codec.digest.DigestUtils;

/**
 * Possible key formats for converters that generate upsertable maps instead of lists
 */
public enum UpsertableKeyFormat {
    /**
     * Use the values as keys as-is i.e. without any transformation
     */
    AS_IS,
    /**
     * Use the values as keys by compacting the URIs i.e. URIs are compacted to prefixed name form if possible.
     * <p>
     * This should be used when the values are known to be URIs and you expect to always have the same prefixes
     * available.  Keys <strong>MAY NOT</strong> be deterministic if the prefixes change over time so {@link #HASHED}
     * may be preferable.
     * </p>
     */
    COMPACTED,
    /**
     * Hash the values to produce the keys.  This should be used when the values may be large e.g. literals, or a mix of
     * URIs and literals.  It provides deterministic fixed length keys.
     */
    HASHED;

    /**
     * Hashes a key
     *
     * @param key Key value
     * @return Hexadecimal format SHA-256 hash of the key
     */
    public static String hashKey(String key) {
        return DigestUtils.sha256Hex(key);
    }
}
