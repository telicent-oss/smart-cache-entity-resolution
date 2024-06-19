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
package io.telicent.smart.cache.search.options;

import io.telicent.jena.abac.AttributeValueSet;
import io.telicent.jena.abac.attributes.AttributeExpr;
import io.telicent.jena.abac.core.AttributesStore;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.atlas.lib.Cache;
import org.apache.jena.atlas.lib.CacheFactory;

import java.util.List;
import java.util.Objects;

/**
 * Represents security options for a search
 */
public class SecurityOptions {

    /**
     * Constant security options for when you want security disabled
     */
    public static final SecurityOptions DISABLED = new SecurityOptions(false);

    /**
     * Constant security options for when you want security disabled but be able to show the security labels
     */
    public static final SecurityOptions DISABLED_BUT_SHOW = new SecurityOptions(true);

    private final boolean enabled;
    private final String username;
    private final AttributeValueSet attributes;
    private final AttributesStore store;
    private final Cache<String, List<AttributeExpr>> labelsToExpressions;
    private final boolean showSecurityLabels;

    /**
     * Creates new security options where security is disabled, thus this is a private constructor to prevent casual use
     * of this. If a user wants security disabled they can explicitly use the {@link SecurityOptions#DISABLED}
     * constant.
     */
    private SecurityOptions(boolean showSecurityLabels) {
        this.enabled = false;
        this.username = null;
        this.attributes = null;
        this.store = null;
        // For disabled security use a null cache, this avoids SecureSearchContext issuing spurious warnings
        this.labelsToExpressions = CacheFactory.createNullCache();
        this.showSecurityLabels = showSecurityLabels;
    }

    /**
     * Creates new security options for the given user (security labels hidden)
     *
     * @param username            Username
     * @param attributes          User attributes
     * @param store               Attributes store
     * @param labelsToExpressions Label expressions
     */
    public SecurityOptions(String username, AttributeValueSet attributes, AttributesStore store,
                           Cache<String, List<AttributeExpr>> labelsToExpressions) {
        this(username, attributes, store, labelsToExpressions, false);
    }

    /**
     * Creates new security options for the given user
     *
     * @param username            Username
     * @param attributes          User attributes
     * @param store               Attributes store
     * @param labelsToExpressions Label expressions
     * @param showSecurityLabels  Show security labels flag
     */
    public SecurityOptions(String username, AttributeValueSet attributes, AttributesStore store,
                           Cache<String, List<AttributeExpr>> labelsToExpressions, boolean showSecurityLabels) {
        Objects.requireNonNull(username);
        Objects.requireNonNull(attributes);
        this.enabled = true;
        this.username = username;
        this.attributes = attributes;
        this.store = store;
        this.labelsToExpressions = labelsToExpressions;
        this.showSecurityLabels = showSecurityLabels;
    }

    /**
     * Gets whether security is enabled
     * <p>
     * When security is enabled then we expect that search results will be filtered taking into account the username and
     * attributes provided by these options.
     * </p>
     *
     * @return True if enabled, false otherwise
     */
    public boolean isEnabled() {
        return this.enabled;
    }

    /**
     * Gets the username for the request
     *
     * @return Username
     */
    public String getUsername() {
        return this.username;
    }

    /**
     * Gets the user attributes for the request
     *
     * @return User attributes
     */
    public AttributeValueSet getAttributes() {
        return this.attributes;
    }

    /**
     * Gets the user attributes store in-use
     *
     * @return User attributes store
     */
    public AttributesStore getAttributesStore() {
        return this.store;
    }

    /**
     * Gets label expressions
     *
     * @return Label expressions
     */
    public Cache<String, List<AttributeExpr>> getLabelsToExpressions() {
        return this.labelsToExpressions;
    }

    /**
     * Gets whether show security labels is enabled
     *
     * @return True if enabled, false otherwise
     */
    public boolean getShowSecurityLabels() {
        return this.showSecurityLabels;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof SecurityOptions other)) {
            return false;
        }

        if (this.enabled != other.enabled) {
            return false;
        }
        if (this.showSecurityLabels != other.showSecurityLabels) {
            return false;
        }
        if (!StringUtils.equals(this.username, other.username)) {
            return false;
        }
        if (!Objects.equals(this.store, other.store)) {
            return false;
        }
        return Objects.equals(this.attributes, other.attributes);
    }

    @Override
    public String toString() {
        return this.username;
    }
}
