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
import io.telicent.smart.cache.entity.sinks.converters.documents.EntityDocumentFormatProvider;
import io.telicent.smart.cache.entity.sinks.converters.documents.EntityDocumentFormats;

import java.util.ArrayList;
import java.util.List;

/**
 * A help section that details available document formats
 */
public class DocumentFormatsHelp implements HelpSection {

    private final String defaultFormatName;

    /**
     * Creates new help section
     *
     * @param defaultFormatName Default format name
     */
    public DocumentFormatsHelp(String defaultFormatName) {
        this.defaultFormatName = defaultFormatName;
    }

    @Override
    public String getTitle() {
        return "Available Document Formats";
    }

    @Override
    public String getPostamble() {
        return "If a specific format is not selected then this command defaults to "
                + this.defaultFormatName
                + ".  "
                + "Note that selecting a document format that is inappropriate for the data being indexed "
                + "will result in an index being built that cannot answer the desired queries.  Therefore in "
                + "most cases using the default format for the command you are running is advised.  "
                + "If you change the document format for an index that has already been partially built then "
                + "any updates and deletes may not apply correctly.  Therefore changing the document format "
                + "should be done very carefully and ideally in conjunction with use of the --recreate-index "
                + "option.";
    }

    @Override
    public int suggestedOrder() {
        return CommonSections.ORDER_DISCUSSION - 1;
    }

    @Override
    public String getPreamble() {
        return "This command can generate documents using one of following document formats:";
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
        List<String> formats = new ArrayList<>();
        for (EntityDocumentFormatProvider format : EntityDocumentFormats.availableFormats()) {
            formats.add(String.format("%s: %s", format.name(), format.description()));
        }
        return formats.toArray(new String[0]);
    }
}
