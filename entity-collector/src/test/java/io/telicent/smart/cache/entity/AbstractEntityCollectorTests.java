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

import io.telicent.smart.cache.entity.vocabulary.Rdf;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.RDFParserBuilder;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.testng.annotations.BeforeClass;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class AbstractEntityCollectorTests {
    public static final String DATA_NAMESPACE = "http://data.gov.uk/testdata#";
    public static final String FRED_URI = DATA_NAMESPACE + "Fred";
    public static final String IES_NAMESPACE = "http://ies.data.gov.uk/ontology/ies4#";
    public static final Node PERSON_TYPE = NodeFactory.createURI(IES_NAMESPACE + "Person");
    public static final Node PARTICULAR_PERIOD_TYPE = NodeFactory.createURI(IES_NAMESPACE + "ParticularPeriod");
    public static final Node EVENT_PARTICIPANT_TYPE = NodeFactory.createURI(IES_NAMESPACE + "EventParticipant");
    public static final Node FRED_AGE = NodeFactory.createLiteral("34", XSDDatatype.XSDinteger);
    public static final Node FRED_PHONE = NodeFactory.createLiteralString("01234 567 890");
    public static final Node FRED_PUBLIC_EMAIL = NodeFactory.createURI("mailto:fred@gmail.com");
    public static final Node FRED_WORK_EMAIL = NodeFactory.createURI("mailto:fred@work.com");
    public static final Node FRED_PRIVATE_EMAIL = NodeFactory.createURI("mailto:fred+banking@gmail.com");
    public static final Node FRED_NICKNAME1 = NodeFactory.createLiteralString("Freddie");
    public static final Node FRED_NICKNAME2 = NodeFactory.createLiteralString("The Fredster");
    public static final Node FRED_SHORT_NAME = NodeFactory.createLiteralString("Fred Test");
    public static final Node FRED_FULL_NAME = NodeFactory.createLiteralString("Frederick A. Test");
    public static final Node FRED_IMAGE_ID = NodeFactory.createLiteralString("aaaa-aaaa-9999-5555");
    public static final List<Node> SECURE_FRED_VALUES =
            List.of(PERSON_TYPE, EVENT_PARTICIPANT_TYPE, FRED_SHORT_NAME, FRED_FULL_NAME, FRED_AGE, FRED_PHONE,
                    FRED_PUBLIC_EMAIL, FRED_WORK_EMAIL,
                    FRED_PRIVATE_EMAIL, FRED_NICKNAME1, FRED_NICKNAME2);
    protected Graph testData;
    protected Graph testLabels;

    /**
     * Loads in a test graph from a classpath resource
     *
     * @param resource Classpath resource
     * @param lang     RDF language the test graph is serialized in
     * @return Test graph
     */
    protected final Graph loadTestGraph(String resource, Lang lang) {
        try (InputStream input = this.getClass().getResourceAsStream(resource)) {
            if (input == null) {
                throw new RuntimeException("Failed to load test data " + resource + "from classpath");
            }

            RDFParser parser = RDFParserBuilder.create().source(input).lang(Lang.TURTLE).build();
            Graph g = GraphFactory.createDefaultGraph();
            parser.parse(g);
            return g;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeClass
    public void setup() {
        this.testData = loadTestGraph("/hospital.ttl", Lang.TURTLE);
        this.testLabels = loadTestGraph("/hospital-security-labels.ttl", Lang.TURTLE);

    }

    protected Entity createFredWithTypes() {
        return createFredWithTypes(null);
    }

    protected Entity createFredWithTypes(PrefixMapping prefixes) {
        Entity entity = new Entity(NodeFactory.createURI(FRED_URI), prefixes);
        addTypes(entity);
        return entity;
    }

    private void addTypes(Entity entity) {
        entity.addData(Rdf.TYPE_GROUP, EntityData.of(Rdf.TYPE, PERSON_TYPE));
        entity.addData(Rdf.TYPE_GROUP, EntityData.of(Rdf.TYPE, EVENT_PARTICIPANT_TYPE));
    }

    protected Entity createFredWithNames() {
        PrefixMapping prefixes = PrefixMapping.Factory.create();
        prefixes.setNsPrefix("foaf", FOAF.getURI());
        Entity entity = new Entity(NodeFactory.createURI(FRED_URI), prefixes);
        addNames(entity);
        return entity;
    }

    protected void addNames(Entity entity) {
        entity.addData("names", EntityData.of(FOAF.name.asNode(), FRED_SHORT_NAME));
        entity.addData("names", EntityData.of(FOAF.name.asNode(), FRED_FULL_NAME));
    }

    protected Entity createSecureFred(PrefixMapping prefixes) {
        Entity entity = createFredWithTypes(prefixes);
        addNames(entity);
        entity.addData("age", EntityData.of(FOAF.age.asNode(), FRED_AGE,
                                            "gdpr"));
        entity.addData("phone",
                       EntityData.of(FOAF.phone.asNode(), FRED_PHONE, "public"));
        entity.addData("email", EntityData.of(FOAF.mbox.asNode(), FRED_PUBLIC_EMAIL));
        entity.addData("email",
                       EntityData.of(FOAF.mbox.asNode(), FRED_WORK_EMAIL, "work"));
        entity.addData("email",
                       EntityData.of(FOAF.mbox.asNode(), FRED_PRIVATE_EMAIL,
                                     "private"));
        entity.addData("nickname", EntityData.of(FOAF.nick.asNode(), FRED_NICKNAME1));
        entity.addData("nickname", EntityData.of(FOAF.nick.asNode(), FRED_NICKNAME2));
        return entity;
    }
}
