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

import io.telicent.smart.cache.configuration.Configurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * Provides configuration variables and helper methods around configuring a {@link RedactedDocumentsCache}
 */
public final class RedactedDocumentsConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedactedDocumentsConfiguration.class);

    /**
     * Default maximum users
     */
    public static final int DEFAULT_MAX_USERS = 100;
    /**
     * Default maximum documents per user.
     * <p>
     * This can be quite a large value, currently defaulting to {@value #DEFAULT_MAX_DOCUMENTS_PER_USER}, because the
     * cache entries consist of a fixed length string key and a boolean value indicating whether or not the document is
     * visible to a given user.  So the documents themselves are not cached, rather a simple indicator of their
     * visibility allowing us to short-circuit evaluating the security labels for documents we know the user cannot
     * see.
     * </p>
     */
    public static final int DEFAULT_MAX_DOCUMENTS_PER_USER = 50_000;
    /**
     * Default cache entry expiry time in minutes
     */
    public static final int DEFAULT_EXPIRES_AFTER_MINUTES = 5;

    /**
     * Private constructor prevents instantiation
     */
    private RedactedDocumentsConfiguration() {
    }

    /**
     * Environment variable used to configure the maximum number of users that will have their document visibility
     * entries cached in the redacted documents cache
     */
    public static final String ENV_MAX_USERS = "REDACTED_CACHE_MAX_USERS";
    /**
     * Environment variable used to configure the maximum number of document visibility entries that may be cached for a
     * single user in the redacted documents cache
     */
    public static final String ENV_MAX_DOCUMENTS_PER_USER = "REDACTED_CACHE_MAX_DOCUMENT_PER_USER";
    /**
     * Environment variable used to configure the number of minutes after which entries in the redacted documents cache
     * should expire
     */
    public static final String ENV_EXPIRES_AFTER_MINUTES = "REDACTED_CACHE_EXPIRES_AFTER";

    /**
     * Creates a default implementation of a redacted documents cache based upon the configuration variables obtained
     * via {@link Configurator}
     *
     * @return Default redacted documents cache implementation
     * @throws IllegalArgumentException If any of the available configuration is invalid
     */
    public static RedactedDocumentsCache createFromConfiguration() {
        return create(Configurator.get(ENV_MAX_USERS, Integer::parseInt, DEFAULT_MAX_USERS),
                      Configurator.get(ENV_MAX_DOCUMENTS_PER_USER, Integer::parseInt, DEFAULT_MAX_DOCUMENTS_PER_USER),
                      Duration.ofMinutes(Configurator.get(ENV_EXPIRES_AFTER_MINUTES, Integer::parseInt,
                                                          DEFAULT_EXPIRES_AFTER_MINUTES)));
    }

    /**
     * Creates a default implementation of a redacted documents cache based upon the configuration values given
     *
     * @param maxUsers            Maximum users
     * @param maxDocumentsPerUser Maximum documents per user
     * @param expiresAfter        Duration after which cached document visibility results expire
     * @return Default redacted documents cache implementation
     * @throws IllegalArgumentException Thrown if the provided configuration is invalid
     */
    public static RedactedDocumentsCache create(int maxUsers, int maxDocumentsPerUser, Duration expiresAfter) {
        return new CaffeineRedactedDocumentsCache(maxUsers, maxDocumentsPerUser, expiresAfter);
    }

    /**
     * Tries to create a default implementation of a redacted documents cache using {@link #createFromConfiguration()}.
     * Unlike the other method this one catches and logs any errors returning {@code null} in the scenario where the
     * configuration is invalid.
     * <p>
     * A deployment environment can thus force the cache to be disabled by explicitly setting one of the variables to an
     * invalid value e.g. {@value ENV_MAX_USERS} set to {@code 0} would effectively disable the cache.
     * </p>
     *
     * @return A redacted documents cache, or {@code null} if the configuration is invalid
     */
    public static RedactedDocumentsCache tryCreateFromConfiguration() {
        try {
            return createFromConfiguration();
        } catch (Throwable e) {
            LOGGER.warn("Failed to create redacted documents cache from configuration:", e);
            return null;
        }
    }
}
