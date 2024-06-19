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

import io.telicent.smart.cache.search.model.SearchResults;

import java.util.Objects;

/**
 * Represents search options that can impact the results returned
 */
public final class SearchOptions {
    private final long limit;
    private final long offset;
    private final HighlightingOptions highlighting;
    private final TypeFilterOptions typeFilterOptions;
    private final SecurityOptions securityOptions;
    private final SortOptions sortOptions;
    private final FieldOptions fieldOptions;

    /**
     * Creates search options
     *
     * @param limit        Limit on results to apply, use {@link SearchResults#UNLIMITED} to request unlimited results
     * @param offset       Offset on results to apply, use {@link SearchResults#FIRST_OFFSET} to start from the first
     *                     result
     * @param highlighting Highlighting options (null for disabled)
     * @param typeFilter   Type filtering options (null for disabled)
     * @param security     Security options (null for disabled)
     * @param sort         Sort options (null for disabled)
     * @param boosts       Field boosts (null for disabled)
     */
    private SearchOptions(long limit, long offset, HighlightingOptions highlighting, TypeFilterOptions typeFilter,
                          SecurityOptions security, SortOptions sort, FieldOptions boosts) {
        // NB - The Builder class enforces the necessary constraints on all the parameters and as the constructor is
        // private we can guarantee all parameters are valid at this point!
        this.limit = limit;
        this.offset = offset;
        this.highlighting = highlighting;
        this.typeFilterOptions = typeFilter;
        this.securityOptions = security;
        this.sortOptions = sort;
        this.fieldOptions = boosts;
    }

    /**
     * Gets the limit
     *
     * @return Limit
     */
    public long getLimit() {
        return limit;
    }

    /**
     * Gets the offset
     *
     * @return Offset
     */
    public long getOffset() {
        return offset;
    }

    /**
     * Gets the highlighting options
     *
     * @return Highlighting options
     */
    public HighlightingOptions getHighlighting() {
        return this.highlighting;
    }

    /**
     * Gets the type filtering options
     *
     * @return Type filtering options
     */
    public TypeFilterOptions getTypeFilterOpts() {
        return this.typeFilterOptions;
    }

    /**
     * Gets the security options
     *
     * @return Security options
     */
    public SecurityOptions getSecurity() {
        return this.securityOptions;
    }

    /**
     * Gets the sort options
     *
     * @return sort options
     */
    public SortOptions getSortOptions() {
        return this.sortOptions;
    }

    /**
     * Gets the field options
     *
     * @return field options
     */
    public FieldOptions getFieldOptions() {
        return this.fieldOptions;
    }

    /**
     * Gets a builder for creating new search options
     *
     * @return Search Options builder
     */
    public static Builder create() {
        return new Builder();
    }

    /**
     * Creates a new set of default search options
     * <p>
     * For full control over options see {@link #create()} to access the {@link Builder} to configure the search options
     * as desired.
     * </p>
     *
     * @return Default search options
     */
    public static SearchOptions defaults() {
        return create().build();
    }

    /**
     * Creates a new set of search options with the given limit and offset, all other parameters use the defaults
     * <p>
     * For full control over options see {@link #create()} to access the {@link Builder} to configure the search options
     * as desired.
     * </p>
     *
     * @param limit  Limit, {@value SearchResults#UNLIMITED} for unlimited results
     * @param offset Offset, {@value SearchResults#FIRST_OFFSET} to start from the first result
     * @return Search options
     */
    public static SearchOptions of(long limit, long offset) {
        return create().limit(limit).offset(offset).build();
    }

    /**
     * Creates a new options builder using the provided search options as the initial values for the builder
     *
     * @param options options
     * @return Builder
     */
    public static Builder copy(SearchOptions options) {
        return create().limit(options.getLimit())
                       .offset(options.getOffset())
                       .withHighlighting(options.getHighlighting())
                       .withTypeFiltering(options.getTypeFilterOpts())
                       .withSecurity(options.getSecurity())
                       .withSorting(options.getSortOptions())
                       .withFields(options.getFieldOptions());
    }

    /**
     * A builder for search options
     */
    public static final class Builder {
        private long limit = SearchResults.UNLIMITED;
        private long offset = SearchResults.FIRST_OFFSET;
        private HighlightingOptions highlighting = HighlightingOptions.DISABLED;
        private TypeFilterOptions
                typeFilterOptions = TypeFilterOptions.DISABLED;
        private SecurityOptions securityOptions = SecurityOptions.DISABLED;
        private SortOptions
                sortOptions = SortOptions.NONE;
        private FieldOptions fieldOptions = FieldOptions.DEFAULT;

        /**
         * Sets the limit on search results
         *
         * @param limit Limit
         * @return Builder
         */
        public Builder limit(long limit) {
            if (limit < SearchResults.UNLIMITED) {
                throw new IllegalArgumentException("Limit must be >= -1 where -1 means unlimited");
            } else {
                this.limit = limit;
            }
            return this;
        }

        /**
         * Sets that search results should be unlimited
         *
         * @return Builder
         */
        public Builder unlimited() {
            return limit(SearchResults.UNLIMITED);
        }

        /**
         * Sets the 1 based offset from which to return search results
         *
         * @param offset Offset
         * @return Builder
         */
        public Builder offset(long offset) {
            if (offset < SearchResults.FIRST_OFFSET) {
                throw new IllegalArgumentException("Offset must be >= 1");
            } else {
                this.offset = offset;
            }
            return this;
        }

        /**
         * Sets that search results should be returned from the start
         *
         * @return Builder
         */
        public Builder fromStart() {
            return offset(SearchResults.FIRST_OFFSET);
        }

        /**
         * Sets the highlighting options for the search
         *
         * @param options Highlighting options
         * @return Builder
         */
        public Builder withHighlighting(HighlightingOptions options) {
            this.highlighting = Objects.requireNonNullElse(options, HighlightingOptions.DISABLED);
            return this;
        }

        /**
         * Disables highlighting for the search
         *
         * @return Builder
         */
        public Builder withoutHighlighting() {
            return withHighlighting(HighlightingOptions.DISABLED);
        }

        /**
         * Sets the type filtering options for the search
         *
         * @param options Type filtering options
         * @return Builder
         */
        public Builder withTypeFiltering(TypeFilterOptions options) {
            this.typeFilterOptions = Objects.requireNonNullElse(options, TypeFilterOptions.DISABLED);
            return this;
        }

        /**
         * Sets that type filtering is disabled for this search
         *
         * @return Builder
         */
        public Builder withoutTypeFiltering() {
            return withTypeFiltering(TypeFilterOptions.DISABLED);
        }

        /**
         * Sets the security options for the search
         *
         * @param options Security options
         * @return Builder
         */
        public Builder withSecurity(SecurityOptions options) {
            this.securityOptions = Objects.requireNonNullElse(options, SecurityOptions.DISABLED);
            return this;
        }

        /**
         * Sets that security is disabled for this search
         *
         * @return Builder
         */
        public Builder withoutSecurity() {
            return withSecurity(SecurityOptions.DISABLED);
        }

        /**
         * Sets that security is disabled but security labels are shown in search results
         *
         * @return Builder
         */
        public Builder withoutSecurityButShowLabels() {
            return withSecurity(SecurityOptions.DISABLED_BUT_SHOW);
        }

        /**
         * Sets the sort options for this search
         *
         * @param options Sort options
         * @return Builder
         */
        public Builder withSorting(SortOptions options) {
            this.sortOptions = Objects.requireNonNullElse(options, SortOptions.NONE);
            return this;
        }

        /**
         * Sets that default sort options are used for this search
         *
         * @return Builder
         */
        public Builder withDefaultSorting() {
            return withSorting(SortOptions.NONE);
        }

        /**
         * Sets the field options that are used for the search
         *
         * @param options Field options
         * @return Builder
         */
        public Builder withFields(FieldOptions options) {
            if (options == null) {
                this.fieldOptions = FieldOptions.DEFAULT;
            } else {
                this.fieldOptions = options;
            }
            return this;
        }

        /**
         * Sets that the default fields are used for the search
         *
         * @return Builder
         */
        public Builder withDefaultFields() {
            return withFields(FieldOptions.DEFAULT);
        }

        /**
         * Builds new search options
         *
         * @return Search Options
         */
        public SearchOptions build() {
            return new SearchOptions(this.limit, this.offset, this.highlighting, this.typeFilterOptions,
                                     this.securityOptions, this.sortOptions, this.fieldOptions);
        }
    }
}
