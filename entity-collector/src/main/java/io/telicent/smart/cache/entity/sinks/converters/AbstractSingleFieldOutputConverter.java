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

import io.telicent.smart.cache.entity.Entity;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An abstract output converter that creates a single output field on the output map
 */
public abstract class AbstractSingleFieldOutputConverter implements EntityToMapOutputConverter {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSingleFieldOutputConverter.class);

    /**
     * The output field name
     */
    protected final String outputField;

    /**
     * Creates a new converter
     *
     * @param outputField Output field name
     */
    public AbstractSingleFieldOutputConverter(String outputField) {
        if (StringUtils.isBlank(outputField)) {
            throw new IllegalArgumentException("outputField cannot be null/blank");
        }
        this.outputField = outputField;
    }

    @Override
    public final void output(Entity entity, Map<String, Object> map) {
        Object output = getOutput(entity);
        if (output != null) {
            // If the output field already exists potentially merge the outputs together
            if (map.containsKey(this.outputField)) {
                Object existingOutput = map.get(this.outputField);
                if (existingOutput instanceof Map<?, ?> && output instanceof Map<?, ?>) {
                    mergeMaps((Map<String, Object>) output, (Map<String, Object>) existingOutput);
                } else if (existingOutput instanceof List<?> && output instanceof List<?>) {
                    ((List<Object>) existingOutput).addAll((List<Object>) existingOutput);
                } else {
                    LOGGER.warn(
                            "Multiple output converters produced conflicting values for field {}, only last output will be preserved",
                            this.outputField);
                    map.put(this.outputField, output);
                }
            } else {
                map.put(this.outputField, output);
            }
            if (shouldOutputFineGrainedSecurityLabels(entity)) {
                populateSecurityLabels(entity,
                                       (Map<String, Object>) map.computeIfAbsent(DefaultOutputFields.SECURITY_LABELS,
                                                                                 x -> new HashMap<String, Object>()));
            }
        }
    }

    /**
     * Merges output maps together being careful to preserve any previously output security labels
     *
     * @param output         New output
     * @param existingOutput Existing output
     */
    protected final void mergeMaps(Map<String, Object> output, Map<String, Object> existingOutput) {
        for (Map.Entry<String, Object> entry : output.entrySet()) {
            if (StringUtils.equals(entry.getKey(), DefaultOutputFields.SECURITY_LABELS)) {
                // For the security labels field (which is itself a map) we don't want to overwrite any existing labels
                // that have been output.  Therefore, we grab the labels map (if any) and copy the newly output security
                // labels into it
                ((Map<String, Object>) existingOutput.computeIfAbsent(DefaultOutputFields.SECURITY_LABELS,
                                                                      x -> new HashMap<String, Object>())).putAll(
                        (Map<String, Object>) entry.getValue());
            } else {
                existingOutput.put(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Indicates whether the output converter produces separate fine-grained security labels to add to the document
     * structure being built.
     * <p>
     * Defaults to {@code false}.  Derived implementations should override this if, and only if, they need to populate a
     * separate security labels object to their output object.  If they can incorporate security labels directly into
     * their own output, i.e. that produced by their {@link #getOutput(Entity)} method then they <strong>SHOULD</strong>
     * do that instead.
     * </p>
     *
     * @param entity Entity
     * @return True if a separate security labels output object will be populated, false otherwise e.g. there are no
     * fine-grained security labels, or they are already incorporated into the main output of this converter
     */
    protected boolean shouldOutputFineGrainedSecurityLabels(Entity entity) {
        return false;
    }

    /**
     * Gets the actual output object that will be used as the value for the output field this converter produces.
     * <p>
     * If {@code null} is returned then no output field will be added.
     * </p>
     *
     * @param entity Entity
     * @return Output object
     */
    protected abstract Object getOutput(Entity entity);

    /**
     * Populates the fine-grained security labels for the output (if any)
     * <p>
     * Defaults to a no-op so only needs to be overridden by derived implementations if they have already overridden
     * {{@link #shouldOutputFineGrainedSecurityLabels(Entity)}} to return {@code true}.
     * </p>
     *
     * @param entity         Entity
     * @param securityLabels Security Labels map into which additional security labels should be populated
     */
    protected void populateSecurityLabels(Entity entity, Map<String, Object> securityLabels) {
        // No-op by default, should be overridden where needed
    }
}
