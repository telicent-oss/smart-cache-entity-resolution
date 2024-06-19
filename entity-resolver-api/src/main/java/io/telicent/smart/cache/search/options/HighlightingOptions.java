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

import org.apache.commons.lang3.StringUtils;

/**
 * Highlighting options for search
 */
public class HighlightingOptions {

    /**
     * Constant highlighting options for when you want highlighting disabled
     */
    public static final HighlightingOptions DISABLED = new HighlightingOptions();

    private final boolean enabled;
    private final String preTag;
    private final String postTag;

    /**
     * Creates default highlighting options which disables highlighting
     */
    public HighlightingOptions() {
        this(false, null, null);
    }

    /**
     * Creates highlighting options that use the underlying search index's default highlighting tags
     *
     * @param enabled True to enable highlighting, false to disable it
     */
    public HighlightingOptions(boolean enabled) {
        this(enabled, null, null);
    }

    /**
     * Creates highlighting options
     *
     * @param enabled Whether highlighting is enabled
     * @param preTag  Pre-tag for highlighting, if {@code null} then the underlying search index's default highlighting
     *                tags are used
     * @param postTag Post-tag for highlighting, if {@code null} then the underlying search index's default highlighting
     *                tags are used
     */
    public HighlightingOptions(boolean enabled, String preTag, String postTag) {
        this.enabled = enabled;
        this.preTag = StringUtils.isNotBlank(preTag) ? preTag : null;
        this.postTag = StringUtils.isNotBlank(postTag) ? postTag : null;
    }

    /**
     * Gets whether highlighting is enabled
     *
     * @return True if enabled, false if disabled
     */
    public boolean isEnabled() {
        return this.enabled;
    }

    /**
     * Gets the pre-tag used for highlighting, if {@code null} then the underlying search index's default highlighting
     * tags are used
     *
     * @return Pre-tag or {@code null}
     */
    public String getPreTag() {
        return this.preTag;
    }

    /**
     * Gets the post-tag used for highlighting, if {@code null} then the underlying search index's default highlighting
     * tags are used
     *
     * @return Post-tag or {@code null}
     */
    public String getPostTag() {
        return this.postTag;
    }

    /**
     * Gets whether both pre-tags and post-tags are set
     *
     * @return True if both are set, false otherwise
     */
    public boolean bothTagsSet() {
        return StringUtils.isNotBlank(this.preTag) && StringUtils.isNotBlank(this.postTag);
    }

    /**
     * Gets whether any custom tags are used i.e. either pre-tags or post-tags are set
     *
     * @return True if either is set, false if neither is set
     */
    public boolean usesCustomTags() {
        return StringUtils.isNotBlank(this.preTag) || StringUtils.isNotBlank(this.postTag);
    }

    /**
     * Gets whether default tags are used i.e. no custom pre-tags/post-tags have been set
     *
     * @return True if neither tags are set, false otherwise
     */
    public boolean usesDefaultTags() {
        return StringUtils.isBlank(this.preTag) && StringUtils.isBlank(this.postTag);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof HighlightingOptions other)) {
            return false;
        }

        if (this.enabled != other.enabled) {
            return false;
        }

        if (!StringUtils.equals(this.preTag, other.preTag)) {
            return false;
        }

        return !!StringUtils.equals(this.postTag, other.postTag);
    }
}
