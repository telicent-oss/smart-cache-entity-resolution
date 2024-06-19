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
package io.telicent.smart.cache.entity.resolver.elastic.similarity;

import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.json.JsonData;
import io.telicent.smart.cache.canonical.QueryVisitor;
import io.telicent.smart.cache.canonical.configuration.CanonicalTypeConfiguration.*;

import java.util.List;

/**
 * Making use of the visitor pattern in order to avoid importing unnecessary libs into the other module. This will be
 * passed into the relevant config class who in turn will execute the query generation.
 */
public class SimilarityQueryVisitor implements QueryVisitor {
    Query query;

    /**
     * Obtain the query (after being built)
     *
     * @return the query
     */
    public Query getQuery() {
        return query;
    }

    @Override
    public void buildQuery(KeywordField field, Object value) {
        query = Query.of(q -> q.term(qq -> qq.boost(field.boost).field(field.name).value(value.toString())));
    }

    @Override
    public void buildQuery(TextField field, Object value) {
        if (null != field.fuzziness) {
            String fuzzinessSetting = "AUTO";
            if (null != field.fuzziness.min && null != field.fuzziness.max) {
                fuzzinessSetting = "AUTO:" + field.fuzziness.min + "," + field.fuzziness.max;
            }
            String finalFuzzinessSetting = fuzzinessSetting;
            query = Query.of(
                    q -> q.match(qq -> qq.fuzziness(finalFuzzinessSetting)
                                         .field(field.name)
                                         .boost(field.boost)
                                         .query(value.toString())));
        } else {
            query = Query.of(q -> q.match(qq -> qq.field(field.name).boost(field.boost).query(value.toString())));
        }
    }

    @Override
    public void buildQuery(NumberField field, Object value) {
        if (null != field.decay) {
            DecayFunction decayFunction = DecayFunction.of(
                    f -> f.field(field.name).placement(
                            DecayPlacement.of(
                                    d -> d.decay(field.decay.decay)
                                          .origin(JsonData.of(value))
                                          .offset(JsonData.of(field.decay.offset))
                                          .scale(JsonData.of(field.decay.scale))
                            )
                    )
            );
            query = Query.of(q -> q.functionScore(
                    FunctionScoreQuery.of(
                            qq -> qq.functions(List.of(decayFunction._toFunctionScore())).boost(field.boost)
                    )));
        } else {
            query = Query.of(q -> q.match(qq -> qq.field(field.name).boost(field.boost).query(value.toString())));
        }
    }

    @Override
    public void buildQuery(DateField field, Object value) {
        String distanceVal;
        if (null != field.distance && null != field.distance.pivot) {
            distanceVal = field.distance.pivot;
        } else {
            distanceVal = "1s";
        }

        if (null != field.distance && null != field.distance.pivot) {
            query = Query.of(q -> q.distanceFeature(
                                     DistanceFeatureQuery.of(
                                             qq -> qq.field(field.name)
                                                     .boost(field.boost)
                                                     .origin(JsonData.of(value))
                                                      .pivot(JsonData.of(distanceVal))
                                     )
                             )
            );
        } else {
            query = Query.of(q -> q.match(qq -> qq.field(field.name).boost(field.boost).query(value.toString())));
        }
    }

    @Override
    public void buildQuery(LocationField field, Object value) {
        String distanceVal;
        if (null != field.distance && null != field.distance.pivot) {
            distanceVal = field.distance.pivot;
        } else {
            distanceVal = "1in";
        }
        query = Query.of(q -> q.distanceFeature(
                DistanceFeatureQuery.of(
                        qq -> qq.field(field.name)
                                .boost(field.boost)
                                .origin(JsonData.of(value))
                                .pivot(JsonData.of(distanceVal)))
        ));
    }

    @Override
    public void buildQuery(BooleanField field, Object value) {
        query = Query.of(q -> q.term(qq -> qq.boost(field.boost).field(field.name).value(value.toString())));
    }
}
