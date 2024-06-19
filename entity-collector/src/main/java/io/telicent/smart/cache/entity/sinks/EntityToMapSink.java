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
package io.telicent.smart.cache.entity.sinks;

import io.telicent.smart.cache.entity.Entity;
import io.telicent.smart.cache.entity.sinks.converters.EntityToMapOutputConverter;
import io.telicent.smart.cache.projectors.Sink;
import io.telicent.smart.cache.projectors.sinks.AbstractTransformingSink;
import io.telicent.smart.cache.projectors.sinks.builder.AbstractForwardingSinkBuilder;
import io.telicent.smart.cache.projectors.sinks.builder.SinkBuilder;
import io.telicent.smart.cache.projectors.sinks.events.EventKeySink;
import io.telicent.smart.cache.projectors.sinks.events.EventValueSink;
import io.telicent.smart.cache.sources.Event;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;
import java.util.function.Function;

/**
 * A Sink that converts {@link Entity} into a {@link Map} structure that can then be further processed by another sink
 * <p>
 * Maps are used because serialization libraries like Jackson can easily convert Maps into JSON (and other)
 * representations naturally without needing to have a full blown POJO with suitable annotations to control desired
 * serializer behaviour.
 * </p>
 */
public class EntityToMapSink<TKey>
        extends AbstractTransformingSink<Event<TKey, Entity>, Event<Entity, Map<String, Object>>> {

    private final List<EntityToMapOutputConverter> converters = new ArrayList<>();

    /**
     * Creates a new sink with custom mapping options
     *
     * @param destination Optional forwarding destination
     * @param converters  Output converters
     */
    public EntityToMapSink(Sink<Event<Entity, Map<String, Object>>> destination,
                           Collection<EntityToMapOutputConverter> converters) {
        super(destination);
        Objects.requireNonNull(converters, "Converters cannot be null");
        if (CollectionUtils.isEmpty(converters)) {
            throw new IllegalArgumentException("Must specify at least one output converter");
        }
        this.converters.addAll(converters);
    }

    @Override
    protected Event<Entity, Map<String, Object>> transform(Event<TKey, Entity> item) {
        Objects.requireNonNull(item, "Entity cannot be null");
        Map<String, Object> map = new LinkedHashMap<>();
        this.converters.forEach(c -> c.output(item.value(), map));
        return item.replace(item.value(), map);
    }

    /**
     * Creates a new entity to map sink builder
     *
     * @param <TKey> Key type
     * @return Entity to map sink builder
     */
    public static <TKey> Builder<TKey> create() {
        return new Builder<>();
    }

    /**
     * A builder for entity to map sinks
     *
     * @param <TKey> Key type
     */
    public static class Builder<TKey> extends
            AbstractForwardingSinkBuilder<Event<TKey, Entity>, Event<Entity, Map<String, Object>>, EntityToMapSink<TKey>, Builder<TKey>> {
        private List<EntityToMapOutputConverter> converters = new ArrayList<>();

        /**
         * Specifies an output converter to use
         *
         * @param converter Output converter
         * @return Builder
         */
        public Builder<TKey> withConverter(EntityToMapOutputConverter converter) {
            Objects.requireNonNull(converter, "Converter cannot be null");
            this.converters.add(converter);
            return this;
        }

        /**
         * Specifies multiple output converters to use
         *
         * @param converters Output converters
         * @return Builder
         */
        public Builder<TKey> withConverters(EntityToMapOutputConverter... converters) {
            Objects.requireNonNull(converters, "Converters cannot be null");
            this.converters.addAll(Arrays.asList(converters));
            return this;
        }

        /**
         * Specifies multiple output converters to use
         *
         * @param converters Output converters
         * @return Builder
         */
        public Builder<TKey> withConverters(Collection<EntityToMapOutputConverter> converters) {
            Objects.requireNonNull(converters, "Converters cannot be null");
            this.converters.addAll(converters);
            return this;
        }

        /**
         * Sets the destination for this sink to be an event key sink
         *
         * @param f Builder function that builds the event key sink
         * @return Builder
         */
        public SinkBuilder<Event<TKey, Entity>, EntityToMapSink<TKey>> toKeys(
                Function<EventKeySink.Builder<Entity, Map<String, Object>>, SinkBuilder<Event<Entity, Map<String, Object>>, EventKeySink<Entity, Map<String, Object>>>> f) {
            return this.destination(f.apply(EventKeySink.create()).build());
        }

        /**
         * Sets the destination for this sink to be an event value sink
         *
         * @param f Builder function that builds the event value sink
         * @return Builder
         */
        public SinkBuilder<Event<TKey, Entity>, EntityToMapSink<TKey>> toValues(
                Function<EventValueSink.Builder<Entity, Map<String, Object>>, SinkBuilder<Event<Entity, Map<String, Object>>, EventValueSink<Entity, Map<String, Object>>>> f) {
            return this.destination(f.apply(EventValueSink.create()).build());
        }

        /**
         * Builds an entity to map sink
         *
         * @return Entity to map sink
         */
        @Override
        public EntityToMapSink<TKey> build() {
            return new EntityToMapSink<>(this.getDestination(), this.converters);
        }
    }
}
