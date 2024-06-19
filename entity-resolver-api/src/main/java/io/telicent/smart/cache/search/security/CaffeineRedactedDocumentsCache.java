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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.telicent.jena.abac.AttributeValueSet;

import java.time.Duration;
import java.util.Objects;

/**
 * An implementation of the {@link RedactedDocumentsCache} interface backed by a {@link Caffeine} based {@link Cache}
 * <p>
 * This is in fact a cache of cache's.  A top level cache is created keyed by users, the value for each user is another
 * cache keyed by document identifiers (using
 * {@link RedactedDocumentsCache#keyForDocument(SecureSearchContext, String, String)}), this contains the actual
 * document visibility values for documents as visible to that user.  If a users security attributes change then their
 * cache is automatically invalidated.
 * </p>
 */
public class CaffeineRedactedDocumentsCache implements RedactedDocumentsCache {

    private final Cache<String, PerUserCache> documentsByUser;
    private final int maxUsers;
    private final int maxDocumentsPerUser;
    private final Duration expiryAfterAccess;

    /**
     * Creates a new cache
     *
     * @param maxUsers            Maximum number of users to cache
     * @param maxDocumentsPerUser Maximum number of document visibility results to cache per user
     * @param expiryAfterAccess   Specifies how long after access to a user/document entry it will expire
     */
    public CaffeineRedactedDocumentsCache(int maxUsers, int maxDocumentsPerUser, Duration expiryAfterAccess) {
        if (maxUsers < 1) {
            throw new IllegalArgumentException("maxUsers must be >= 1");
        }
        if (maxDocumentsPerUser < 1) {
            throw new IllegalArgumentException("maxDocumentsPerUser must be >= 1");
        }
        Objects.requireNonNull(expiryAfterAccess, "expiryAfterAccess cannot be null");
        if (Duration.ZERO.compareTo(expiryAfterAccess) >= 0) {
            throw new IllegalArgumentException("expiryAfterAccess must be a duration greater than zero");
        }

        this.documentsByUser = Caffeine.newBuilder()
                                       .maximumSize(maxUsers)
                                       .initialCapacity(maxUsers / 4)
                                       .expireAfterAccess(expiryAfterAccess)
                                       .build();
        this.maxUsers = maxUsers;
        this.maxDocumentsPerUser = maxDocumentsPerUser;
        this.expiryAfterAccess = Objects.requireNonNull(expiryAfterAccess);
    }

    @Override
    public void invalidate() {
        this.documentsByUser.invalidateAll();
    }

    @Override
    public Boolean isVisible(SecureSearchContext context, String id, String version) {
        PerUserCache userCache = cacheForUser(context);
        return userCache.get(
                RedactedDocumentsCache.keyForDocument(context, id, version));
    }

    @Override
    public void setVisible(SecureSearchContext context, String id, String version, boolean visibility) {
        PerUserCache userCache = cacheForUser(context);
        userCache.set(RedactedDocumentsCache.keyForDocument(context, id, version), visibility);
    }

    @Override
    public void close() {
        this.documentsByUser.invalidateAll();
    }

    /**
     * Gets/creates a cache for the user identified by this context
     * <p>
     * If the cache exists, but the users attributes have changed since they were last cached then this also invalidates
     * the existing cache and creates a fresh one for the user.
     * </p>
     *
     * @param context Context
     * @return Per-user cache
     */
    private synchronized PerUserCache cacheForUser(SecureSearchContext context) {
        PerUserCache cache = this.documentsByUser.get(context.getUsername(),
                                                      k -> new PerUserCache(k, context.getUserAttributes(),
                                                                            this.maxDocumentsPerUser,
                                                                            this.expiryAfterAccess));
        // If the users attributes have changed since they last were used with this cache then invalidate it
        if (!cache.isValid(context.getUserAttributes())) {
            cache.invalidate();
            this.documentsByUser.invalidate(context.getUsername());

            // Then recurse to generate a fresh cache based on the users current attributes
            return cacheForUser(context);
        } else {
            return cache;
        }
    }

    @Override
    public String toString() {
        return "CaffeineRedactedDocumentsCache(maxUsers=" + this.maxUsers + ", maxDocumentsPerUser=" + this.maxDocumentsPerUser + ", expiresAfter=" + this.expiryAfterAccess + ")";
    }

    /**
     * A per-user cache, a basic wrapper around a Caffeine {@link Cache} exposing simplified methods for our use case
     */
    private static final class PerUserCache {
        private final String user;
        private final AttributeValueSet attributes;

        private final Cache<String, Boolean> documents;

        public PerUserCache(String user, AttributeValueSet attributes, int maxDocumentsPerUser,
                            Duration expiryAfterAccess) {
            this.user = Objects.requireNonNull(user);
            this.attributes = Objects.requireNonNull(attributes);
            this.documents = Caffeine.newBuilder()
                                     .maximumSize(maxDocumentsPerUser)
                                     .initialCapacity(maxDocumentsPerUser / 2)
                                     .expireAfterAccess(expiryAfterAccess)
                                     .build();
        }

        /**
         * Is this cache still valid given the users current attributes?
         * <p>
         * Compares the users current attributes against their previously known attributes with which this cache was
         * created, if their attributes have changed then this cache is no longer considered valid.
         * </p>
         *
         * @param current Users current attributes
         * @return True if still valid, false otherwise
         */
        private boolean isValid(AttributeValueSet current) {
            return this.attributes.equals(current);
        }

        /**
         * Gets the visibility for a given document
         *
         * @param docKey Document Key
         * @return Visibility
         */
        public Boolean get(String docKey) {
            return this.documents.getIfPresent(docKey);
        }

        /**
         * Sets the visibility for a given document
         *
         * @param docKey     Document Key
         * @param visibility Visibility
         */
        public void set(String docKey, Boolean visibility) {
            this.documents.put(docKey, visibility);
        }

        /**
         * Invalidates this cache, used to proactively clean up when the parent class detects an instance is no longer
         * valid and will be thrown away
         */
        public void invalidate() {
            this.documents.invalidateAll();
        }
    }
}
