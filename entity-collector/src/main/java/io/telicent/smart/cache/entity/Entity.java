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
package io.telicent.smart.cache.entity;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.shared.PrefixMapping;

import java.util.*;
import java.util.stream.Stream;

/**
 * Represents an entity i.e. something identified by a URI and data about the entity
 */
public class Entity {

    private final Node uri;
    private final PrefixMapping prefixes;
    private final Map<String, List<EntityData>> data = new HashMap<>();
    private final String defaultSecurityLabels;
    private final Graph securityLabels;

    private final boolean isDeletion;

    /**
     * Creates a new entity with no security labels applied
     *
     * @param uri      URI for the entity
     * @param prefixes Prefixes (optional).  Where present these may be used elsewhere in the pipeline to compact URIs
     *                 included in the entity data.
     */
    public Entity(Node uri, PrefixMapping prefixes) {
        this(uri, prefixes, null, null);
    }

    /**
     * Creates a new entity
     *
     * @param uri                   URI for the entity
     * @param prefixes              Prefixes (optional).  Where present these may be used elsewhere in the pipeline to
     *                              compact URIs included in the entity data.
     * @param defaultSecurityLabels Default security labels that apply to the data (optional).  These apply to any data
     *                              that is not covered by a more specific label via the security labels graph.
     * @param securityLabels        Security Labels graph (optional).  Where present these may be used elsewhere to
     *                              enforce security on access to this entity.
     */
    public Entity(Node uri, PrefixMapping prefixes, String defaultSecurityLabels, Graph securityLabels) {
        this(uri, false, prefixes, defaultSecurityLabels, securityLabels);
    }

    /**
     * Creates a new entity
     *
     * @param uri                   URI for the entity
     * @param isDeletion            Whether this entity is for deletion i.e. the data collected here should be deleted
     *                              rather than added.
     * @param prefixes              Prefixes (optional).  Where present these may be used elsewhere in the pipeline to
     *                              compact URIs included in the entity data.
     * @param defaultSecurityLabels Default security labels that apply to the data (optional).  These apply to any data
     *                              that is not covered by a more specific label via the security labels graph.
     * @param securityLabels        Security Labels graph (optional).  Where present these may be used elsewhere to
     *                              enforce security on access to this entity.
     */
    public Entity(Node uri, boolean isDeletion, PrefixMapping prefixes, String defaultSecurityLabels,
                  Graph securityLabels) {
        Objects.requireNonNull(uri, "Entity uri cannot be null");
        this.uri = uri;
        this.isDeletion = isDeletion;
        this.prefixes = prefixes;
        this.defaultSecurityLabels = defaultSecurityLabels;
        this.securityLabels = securityLabels;
    }

    /**
     * Gets whether this Entity is for deletion i.e. the data on this entity should be deleted rather than added
     *
     * @return True if for deletion, false if for addition
     */
    public boolean isDeletion() {
        return this.isDeletion;
    }

    /**
     * Adds data for the entity
     *
     * @param group Group
     * @param data  Data
     */
    public void addData(String group, EntityData data) {
        List<EntityData> list = this.data.computeIfAbsent(group, key -> new ArrayList<>());
        list.add(data);
    }

    /**
     * Gets the URI for the entity
     *
     * @return URI
     */
    public Node getUri() {
        return this.uri;
    }

    /**
     * Gets whether any prefixes are available for this entity
     *
     * @return True if available, false otherwise
     */
    public boolean hasPrefixes() {
        return this.prefixes != null;
    }

    /**
     * Gets the Prefixes for the entity (if any)
     *
     * @return Prefixes
     */
    public PrefixMapping getPrefixes() {
        return this.prefixes;
    }

    /**
     * Gets whether this entity has any security labels, whether in the form of defaults or a labels graph
     *
     * @return True if there are security labels, false otherwise
     */
    public boolean hasSecurityLabels() {
        return StringUtils.isNotBlank(
                this.defaultSecurityLabels) || (this.securityLabels != null && !this.securityLabels.isEmpty());
    }

    /**
     * Gets the default security labels (if any)
     * <p>
     * These apply to any data that is not covered by a more specific rule in the labels graph.
     * </p>
     *
     * @return Default security labels, or {@code null} if none
     */
    public String getDefaultSecurityLabels() {
        return this.defaultSecurityLabels;
    }

    /**
     * Gets the security labels graph (if any)
     *
     * @return Security labels graph, or {@code null} if none
     */
    public Graph getSecurityLabels() {
        return this.securityLabels;
    }

    /**
     * Gets data for the entity
     *
     * @param group Group
     * @return Data in the group
     */
    public Stream<EntityData> getData(String group) {
        return this.data.getOrDefault(group, Collections.emptyList()).stream();
    }

    /**
     * Gets whether the entity has any data in the given group
     *
     * @param group Group
     * @return True if data for the group present, false otherwise
     */
    public boolean hasData(String group) {
        return CollectionUtils.isNotEmpty(this.data.getOrDefault(group, null));
    }

    /**
     * Gets whether the entity has any data collected for it
     *
     * @return True if any data available, false otherwise
     */
    public boolean hasAnyData() {
        return !this.data.isEmpty();
    }

    /**
     * Gets the data groups defined on this entity
     *
     * @return Data groups
     */
    public Stream<String> getDataGroups() {
        return this.data.keySet().stream();
    }

    /**
     * Gets whether this Entity has any literals collected for it in any data group
     *
     * @return True if any literals are present in any data group, false otherwise
     */
    public boolean hasAnyLiterals() {
        return this.data.values()
                        .stream()
                        .flatMap(Collection::stream)
                        .flatMap(d -> d.keys().flatMap(k -> d.get(k).stream()))
                        .anyMatch(n -> n.getNode().isLiteral());
    }

    /**
     * Converts this entity into an entity for deletion (if it wasn't already)
     *
     * @return Entity for deletion, returns the existing instance if it was already for deletion
     */
    public Entity asDeletion() {
        if (this.isDeletion) {
            return this;
        }

        Entity forDeletion = new Entity(this.uri, true, this.prefixes, this.defaultSecurityLabels, this.securityLabels);
        forDeletion.data.putAll(this.data);
        return forDeletion;
    }
}
