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

import com.github.rvesse.airline.help.sections.HelpSection;
import com.github.rvesse.airline.help.sections.factories.HelpSectionFactory;

import java.lang.annotation.Annotation;
import java.util.List;

/**
 * An Airline help section factory that handles our configuration provider help sections
 */
public class IndexingHelpFactory implements HelpSectionFactory {
    @Override
    public HelpSection createSection(Annotation annotation) {
        if (annotation instanceof AvailableEntityProjections availableEntityProjections) {
            return new EntityProjectionsHelp(availableEntityProjections.value());
        } else if (annotation instanceof AvailableIndexConfigurations availableIndexConfigurations) {
            return new IndexConfigurationsHelp(availableIndexConfigurations.value());
        } else if (annotation instanceof AvailableDocumentFormats availableDocumentFormats) {
            return new DocumentFormatsHelp(availableDocumentFormats.value());
        }
        return null;
    }

    @Override
    public List<Class<? extends Annotation>> supportedAnnotations() {
        //@formatter:off
        return List.of(AvailableEntityProjections.class,
                       AvailableIndexConfigurations.class,
                       AvailableDocumentFormats.class);
        //@formatter:on
    }
}
