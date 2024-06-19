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

import io.telicent.smart.cache.entity.AbstractEntityCollectorTests;
import io.telicent.smart.cache.entity.Entity;
import io.telicent.smart.cache.entity.sinks.converters.*;
import io.telicent.smart.cache.entity.vocabulary.Rdf;
import io.telicent.smart.cache.projectors.sinks.CollectorSink;
import io.telicent.smart.cache.projectors.sinks.builder.AbstractForwardingSinkBuilder;
import io.telicent.smart.cache.sources.Event;
import io.telicent.smart.cache.sources.memory.SimpleEvent;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.*;

public class TestEntityToMapSink extends AbstractEntityCollectorTests {

    @SuppressWarnings("resource")
    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = ".*cannot be null")
    public void entity_to_map_sink_bad_01() {
        new EntityToMapSink<>(null, null);
    }

    @SuppressWarnings("resource")
    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*at least one.*")
    public void entity_to_map_sink_bad_02() {
        new EntityToMapSink<>(null, Collections.emptyList());
    }

    @SuppressWarnings("resource")
    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = ".*cannot be null")
    public void entity_to_map_sink_bad_03() {
        EntityToMapSink<Object> sink =
                new EntityToMapSink<>(null, Collections.singletonList(new UriConverter()));
        sink.send(null);
    }

    private void verifyOutputUri(Entity entity, Map<String, Object> output) {
        Assert.assertNotNull(output);
        Assert.assertEquals(output.get(DefaultOutputFields.URI), entity.getUri().getURI());
    }

    private <TKey> Map<String, Object> verifyOutput(CollectorSink<Event<TKey, Map<String, Object>>> collector) {
        Assert.assertEquals(collector.get().size(), 1);
        return collector.get().get(0).value();
    }


    @SuppressWarnings("unchecked")
    private void verifyTypes(Map<String, Object> output) {
        Assert.assertNotNull(output);
        List<String> types = (List<String>) output.get(DefaultOutputFields.TYPES);
        Assert.assertNotNull(types);
        Assert.assertEquals(types.size(), 2);
        Assert.assertEquals(types.get(0), PERSON_TYPE.getURI());
        Assert.assertEquals(types.get(1), EVENT_PARTICIPANT_TYPE.getURI());
    }

    @Test
    public void entity_to_map_sink_01() {
        CollectorSink<Event<Entity, Map<String, Object>>> collector = CollectorSink.of();
        EntityToMapSink<Integer> sink =
                new EntityToMapSink<>(collector, Collections.singletonList(new UriConverter()));
        Entity entity = createFredWithTypes();
        sink.send(new SimpleEvent<>(null, 1, entity));
        Map<String, Object> output = verifyOutput(collector);
        verifyOutputUri(entity, output);
    }

    @Test
    public void entity_to_map_sink_02() {
        CollectorSink<Event<Entity, Map<String, Object>>> collector = CollectorSink.of();
        EntityToMapSink<Double> sink = new EntityToMapSink<>(collector, Arrays.asList(new UriConverter(),
                                                                                      new DataToSimpleList(
                                                                                              DefaultOutputFields.TYPES,
                                                                                              Rdf.TYPE_GROUP, false))
        );
        Entity entity = createFredWithTypes();
        sink.send(new SimpleEvent<>(null, 1.23, entity));
        Map<String, Object> output = verifyOutput(collector);
        verifyOutputUri(entity, output);
        verifyTypes(output);
    }

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = ".*cannot be null")
    public void entity_to_map_sink_builder_bad_01() {
        EntityToMapSink.create().withConverters((Collection<EntityToMapOutputConverter>) null);
    }

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = ".*cannot be null")
    public void entity_to_map_sink_builder_bad_02() {
        EntityToMapSink.create().withConverter(null);
    }

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = ".*cannot be null")
    public void entity_to_map_sink_builder_bad_03() {
        EntityToMapSink.create().withConverters((EntityToMapOutputConverter[]) null);
    }

    @Test
    public void entity_to_map_sink_builder_01() {
        EntityToMapSink.create().withConverter(new UriConverter())
                       .withConverters(new PrimaryNameConverter(),
                                       new DataToSimpleMap(DefaultOutputFields.LITERALS, true, false,
                                                           DefaultOutputFields.LITERALS))
                       .withConverters(List.of(new DataToSimpleList("types", Rdf.TYPE_GROUP, false)))
                       .build();
    }

    @Test
    public void entity_to_map_sink_builder_02() {
        EntityToMapSink.create().withConverter(new UriConverter()).toKeys(AbstractForwardingSinkBuilder::collect).build();
    }

    @Test
    public void entity_to_map_sink_builder_03() {
        EntityToMapSink.create().withConverter(new UriConverter()).toValues(AbstractForwardingSinkBuilder::collect).build();
    }
}
