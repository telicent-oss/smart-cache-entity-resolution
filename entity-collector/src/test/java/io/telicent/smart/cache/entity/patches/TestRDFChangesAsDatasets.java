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
package io.telicent.smart.cache.entity.patches;

import io.telicent.smart.cache.entity.vocabulary.Rdf;
import io.telicent.smart.cache.entity.vocabulary.Telicent;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.atlas.iterator.Iter;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.Quad;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

public class TestRDFChangesAsDatasets {

    public static final Node TEST_NODE = NodeFactory.createURI("http://test");

    @Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = "No active transaction.*")
    public void transactions_bad_01() {
        RDFChangesAsDatasets changes = new RDFChangesAsDatasets();
        changes.txnCommit();
    }

    @Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = "No active transaction.*")
    public void transactions_bad_02() {
        RDFChangesAsDatasets changes = new RDFChangesAsDatasets();
        changes.txnAbort();
    }

    @Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = "Nested transactions.*")
    public void transactions_bad_03() {
        RDFChangesAsDatasets changes = new RDFChangesAsDatasets();
        changes.txnBegin();
        changes.txnBegin();
    }

    @Test
    public void transactions_01() {
        RDFChangesAsDatasets changes = new RDFChangesAsDatasets();
        changes.txnBegin();
        changes.add(Quad.defaultGraphIRI, NodeFactory.createBlankNode(), Rdf.TYPE,
                    TEST_NODE);
        changes.txnCommit();

        List<Pair<PatchOperation, DatasetGraph>> operations = changes.getOperations().toList();
        Assert.assertEquals(operations.size(), 1);

        verifyOperation(operations, 0, PatchOperation.ADD, 1);
    }

    @Test
    public void transactions_02() {
        RDFChangesAsDatasets changes = new RDFChangesAsDatasets();
        changes.txnBegin();
        changes.add(Quad.defaultGraphIRI, NodeFactory.createBlankNode(), Rdf.TYPE,
                    TEST_NODE);
        changes.delete(Quad.defaultGraphIRI, NodeFactory.createBlankNode(), Rdf.TYPE,
                       TEST_NODE);
        changes.txnCommit();

        List<Pair<PatchOperation, DatasetGraph>> operations = changes.getOperations().toList();
        Assert.assertEquals(operations.size(), 2);

        verifyOperation(operations, 0, PatchOperation.ADD, 1);
        verifyOperation(operations, 1, PatchOperation.DELETE, 1);
    }

    @Test
    public void transactions_03() {
        RDFChangesAsDatasets changes = new RDFChangesAsDatasets();
        changes.txnBegin();
        changes.delete(Quad.defaultGraphIRI, NodeFactory.createBlankNode(), Rdf.TYPE,
                       TEST_NODE);
        changes.add(Quad.defaultGraphIRI, NodeFactory.createBlankNode(), Rdf.TYPE,
                    TEST_NODE);
        changes.txnCommit();

        List<Pair<PatchOperation, DatasetGraph>> operations = changes.getOperations().toList();
        Assert.assertEquals(operations.size(), 2);

        verifyOperation(operations, 0, PatchOperation.DELETE, 1);
        verifyOperation(operations, 1, PatchOperation.ADD, 1);
    }

    private static void verifyOperation(List<Pair<PatchOperation, DatasetGraph>> operations, int operationIndex,
                                        PatchOperation delete, int datasetSize) {
        Pair<PatchOperation, DatasetGraph> op = operations.get(operationIndex);
        Assert.assertEquals(op.getKey(), delete);
        Assert.assertEquals(Iter.count(op.getValue().find()), datasetSize);
    }

    @Test
    public void transactions_04() {
        RDFChangesAsDatasets changes = new RDFChangesAsDatasets();
        changes.txnBegin();
        changes.delete(Quad.defaultGraphIRI, NodeFactory.createBlankNode(), Rdf.TYPE,
                       TEST_NODE);
        changes.add(Quad.defaultGraphIRI, NodeFactory.createBlankNode(), Rdf.TYPE,
                    TEST_NODE);
        changes.txnAbort();

        verifyNoOperations(changes);
    }

    @Test
    public void transactions_05() {
        RDFChangesAsDatasets changes = new RDFChangesAsDatasets();
        changes.txnBegin();
        changes.txnCommit();

        verifyNoOperations(changes);
    }

    private static void verifyNoOperations(RDFChangesAsDatasets changes) {
        List<Pair<PatchOperation, DatasetGraph>> operations = changes.getOperations().toList();
        Assert.assertEquals(operations.size(), 0);
    }

    @Test
    public void start_finish_01() {
        RDFChangesAsDatasets changes = new RDFChangesAsDatasets();
        changes.start();
        changes.finish();

        verifyNoOperations(changes);
    }

    @Test
    public void start_finish_02() {
        RDFChangesAsDatasets changes = new RDFChangesAsDatasets();
        changes.start();
        changes.add(Quad.defaultGraphIRI, NodeFactory.createBlankNode(), Rdf.TYPE, TEST_NODE);

        // Operation won't be recorded until finish() is called
        verifyNoOperations(changes);

        changes.finish();
        List<Pair<PatchOperation, DatasetGraph>> operations = changes.getOperations().toList();
        Assert.assertEquals(operations.size(), 1);
        verifyOperation(operations, 0, PatchOperation.ADD, 1);

        // Calling start() again resets any previous state
        changes.start();
        verifyNoOperations(changes);
    }

    @Test
    public void start_finish_03() {
        RDFChangesAsDatasets changes = new RDFChangesAsDatasets();
        changes.start();
        changes.txnBegin();
        changes.add(Quad.defaultGraphIRI, NodeFactory.createBlankNode(), Rdf.TYPE, TEST_NODE);

        // Operation won't be recorded until finish() is called
        verifyNoOperations(changes);

        // Calling finish() commits any open transaction
        changes.finish();
        List<Pair<PatchOperation, DatasetGraph>> operations = changes.getOperations().toList();
        Assert.assertEquals(operations.size(), 1);
        verifyOperation(operations, 0, PatchOperation.ADD, 1);

        // Calling start() again resets any previous state
        changes.start();
        verifyNoOperations(changes);
    }

    @Test
    public void security_labels_01() {
        RDFChangesAsDatasets changes = new RDFChangesAsDatasets();
        changes.add(Telicent.SECURITY_LABELS_GRAPH_URI, NodeFactory.createBlankNode(), Rdf.TYPE, TEST_NODE);

        Graph securityLabels = changes.getSecurityLabels();
        Assert.assertEquals(securityLabels.stream().count(), 1);
    }

    @Test
    public void security_labels_02() {
        RDFChangesAsDatasets changes = new RDFChangesAsDatasets();
        changes.txnBegin();
        changes.add(Telicent.SECURITY_LABELS_GRAPH_URI, NodeFactory.createBlankNode(), Rdf.TYPE, TEST_NODE);

        Graph securityLabels = changes.getSecurityLabels();
        Assert.assertEquals(securityLabels.stream().count(), 1);

        // Aborting a transaction should include aborting any changes that have happened to the security labels within
        // the context of that transaction
        changes.txnAbort();
        securityLabels = changes.getSecurityLabels();
        Assert.assertEquals(securityLabels.stream().count(), 0);
    }

    @Test
    public void security_labels_03() {
        RDFChangesAsDatasets changes = new RDFChangesAsDatasets();
        Node subject = NodeFactory.createBlankNode();
        changes.add(Telicent.SECURITY_LABELS_GRAPH_URI, subject, Rdf.TYPE, TEST_NODE);

        Graph securityLabels = changes.getSecurityLabels();
        Assert.assertEquals(securityLabels.stream().count(), 1);

        changes.delete(Telicent.SECURITY_LABELS_GRAPH_URI, subject, Rdf.TYPE, TEST_NODE);
        securityLabels = changes.getSecurityLabels();
        Assert.assertEquals(securityLabels.stream().count(), 0);
    }

    @Test
    public void prefixes_01() {
        RDFChangesAsDatasets changes = new RDFChangesAsDatasets();
        changes.start();
        changes.addPrefix(Quad.defaultGraphIRI, "telicent", Telicent.NAMESPACE);
        changes.add(Quad.defaultGraphIRI, NodeFactory.createBlankNode(), Rdf.TYPE, TEST_NODE);
        changes.finish();

        List<Pair<PatchOperation, DatasetGraph>> operations = changes.getOperations().toList();
        verifyOperation(operations, 0, PatchOperation.ADD, 1);

        Graph g = operations.get(0).getValue().getDefaultGraph();
        Assert.assertTrue(g.getPrefixMapping().getNsPrefixMap().containsKey("telicent"));
    }

    @Test
    public void prefixes_02() {
        RDFChangesAsDatasets changes = new RDFChangesAsDatasets();
        changes.start();
        changes.addPrefix(Quad.defaultGraphIRI, "telicent", Telicent.NAMESPACE);
        changes.finish();

        verifyNoOperations(changes);
    }

    @Test
    public void prefixes_03() {
        RDFChangesAsDatasets changes = new RDFChangesAsDatasets();
        changes.txnBegin();
        changes.addPrefix(Quad.defaultGraphIRI, "telicent", Telicent.NAMESPACE);
        changes.add(NodeFactory.createURI("http://graphs/1"), NodeFactory.createBlankNode(), Rdf.TYPE, TEST_NODE);
        changes.txnCommit();

        List<Pair<PatchOperation, DatasetGraph>> operations = changes.getOperations().toList();
        verifyOperation(operations, 0, PatchOperation.ADD, 1);

        Graph g = operations.get(0).getValue().getDefaultGraph();
        Assert.assertTrue(g.getPrefixMapping().getNsPrefixMap().containsKey("telicent"));

        // If we then delete a prefix it should affect the next operation but not previous ones
        changes.txnBegin();
        changes.add(NodeFactory.createURI("http://graphs/2"), NodeFactory.createBlankNode(), Rdf.TYPE, TEST_NODE);
        changes.deletePrefix(Quad.defaultGraphIRI, "telicent");
        changes.txnCommit();

        operations = changes.getOperations().toList();
        verifyOperation(operations, 1, PatchOperation.ADD, 1);
        Assert.assertTrue(g.getPrefixMapping().getNsPrefixMap().containsKey("telicent"));
        g = operations.get(1).getValue().getDefaultGraph();
        Assert.assertFalse(g.getPrefixMapping().getNsPrefixMap().containsKey("telicent"));
    }

    @Test
    public void prefixes_04() {
        RDFChangesAsDatasets changes = new RDFChangesAsDatasets();
        changes.start();
        Node graphNode = NodeFactory.createURI("http://graphs/1");
        changes.addPrefix(graphNode, "telicent", Telicent.NAMESPACE);
        changes.add(Quad.defaultGraphIRI, NodeFactory.createBlankNode(), Rdf.TYPE, TEST_NODE);
        changes.finish();

        List<Pair<PatchOperation, DatasetGraph>> operations = changes.getOperations().toList();
        verifyOperation(operations, 0, PatchOperation.ADD, 1);

        Graph g = operations.get(0).getValue().getGraph(graphNode);
        Assert.assertTrue(g.getPrefixMapping().getNsPrefixMap().containsKey("telicent"));
    }
}
