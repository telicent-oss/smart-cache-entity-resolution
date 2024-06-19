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
import org.apache.jena.graph.Graph;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * An entity output converter that includes the security labels (if any) for the entity.  This outputs a map under this
 * field that itself has up to two fields, one containing the default security labels for the data and one containing a
 * security labels graph for the data.  Either field may be omitted if the relevant labels are not present on the
 * entity.
 */
public class SecurityLabelsConverter extends AbstractSingleFieldOutputConverter {

    /**
     * Creates a new converter that outputs security labels using the default field name
     * {@value DefaultOutputFields#SECURITY_LABELS}
     */
    public SecurityLabelsConverter() {
        this(DefaultOutputFields.SECURITY_LABELS);
    }

    /**
     * Creates a new converter that outputs security labels using the given field name
     *
     * @param outputField Output field name
     */
    public SecurityLabelsConverter(String outputField) {
        super(outputField);
    }

    @Override
    protected Object getOutput(Entity entity) {
        if (!entity.hasSecurityLabels()) {
            return null;
        }

        Map<String, String> output = new HashMap<>();

        Graph securityLabels = entity.getSecurityLabels();
        if (securityLabels != null) {
            StringWriter writer = new StringWriter();
            RDFDataMgr.write(writer, securityLabels, Lang.TURTLE);
            output.put(DefaultOutputFields.SECURITY_LABELS_SUB_FIELD_GRAPH, writer.toString());
        }
        if (StringUtils.isNotBlank(entity.getDefaultSecurityLabels())) {
            output.put(DefaultOutputFields.SECURITY_LABELS_SUB_FIELD_DEFAULTS, entity.getDefaultSecurityLabels());
        }

        return output;
    }
}
