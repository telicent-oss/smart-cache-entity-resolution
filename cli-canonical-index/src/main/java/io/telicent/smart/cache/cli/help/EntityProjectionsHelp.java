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
package io.telicent.smart.cache.cli.help;

import com.github.rvesse.airline.help.sections.HelpFormat;
import com.github.rvesse.airline.help.sections.HelpSection;
import com.github.rvesse.airline.help.sections.common.CommonSections;
import io.telicent.smart.cache.entity.config.EntityProjectionConfigurations;
import io.telicent.smart.cache.entity.config.EntityProjectionProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * A help section that details available entity projections
 */
public class EntityProjectionsHelp implements HelpSection {

    private final String defaultConfigName;

    /**
     * Creates new help section
     *
     * @param defaultConfigName Default configuration name
     */
    public EntityProjectionsHelp(String defaultConfigName) {
        this.defaultConfigName = defaultConfigName;
    }

    @Override
    public String getTitle() {
        return "Available Entity Projection Configurations";
    }

    @Override
    public String getPostamble() {
        return "If a specific configuration is not selected then this command defaults to "
                + this.defaultConfigName
                + ".  "
                + "Note that selecting an entity projection configuration that is inappropriate for the data "
                + "being indexed will result in an index that does not contain some/all entities.  Therefore in "
                + "most cases using the default configuration for the command you are running is advised.";
    }

    @Override
    public int suggestedOrder() {
        return CommonSections.ORDER_DISCUSSION - 3;
    }

    @Override
    public String getPreamble() {
        return "This command can project entities from the incoming RDF knowledge using one of the following entity projection configurations:";
    }

    @Override
    public HelpFormat getFormat() {
        return HelpFormat.LIST;
    }

    @Override
    public int numContentBlocks() {
        return 1;
    }

    @Override
    public String[] getContentBlock(int blockNumber) {
        if (blockNumber != 0) {
            throw new IndexOutOfBoundsException("Not a valid content block");
        }
        List<String> configs = new ArrayList<>();
        for (EntityProjectionProvider provider : EntityProjectionConfigurations.available()) {
            configs.add(String.format("%s: %s", provider.name(), provider.description()));
        }
        return configs.toArray(new String[0]);
    }
}
