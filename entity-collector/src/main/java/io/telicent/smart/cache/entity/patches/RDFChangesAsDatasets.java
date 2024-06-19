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

import io.telicent.smart.cache.entity.vocabulary.Telicent;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.rdfpatch.RDFChanges;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Transforms an RDF Patch into a sequence of adjacent Add/Delete operations
 */
public class RDFChangesAsDatasets implements RDFChanges {

    private final List<Pair<PatchOperation, DatasetGraph>> datasets = new ArrayList<>();
    private final Graph labelsGraph = DatasetGraphFactory.createTxnMem().getDefaultGraph();
    private PatchOperation currentOperation = PatchOperation.NONE;
    private DatasetGraph current;
    private boolean inTransaction;
    private int lastTransactionStart;

    /**
     * Creates a new patch transformer
     */
    public RDFChangesAsDatasets() {
        this.resetTransactionState();
    }

    /**
     * Gets the stream of operations and their datasets
     *
     * @return Operations and datasets
     */
    public Stream<Pair<PatchOperation, DatasetGraph>> getOperations() {
        return this.datasets.stream();
    }

    /**
     * Gets the security labels graph that results from this patch
     *
     * @return Security labels graph
     */
    public Graph getSecurityLabels() {
        return this.labelsGraph;
    }

    @Override
    public void header(String field, Node value) {
        // Ignored
    }

    @Override
    public void add(Node g, Node s, Node p, Node o) {
        // Special handling for labels graph
        if (Telicent.SECURITY_LABELS_GRAPH_URI.equals(g)) {
            this.labelsGraph.add(s, p, o);
            return;
        }

        switch (this.currentOperation) {
            case DELETE:
                switchOperation(PatchOperation.ADD);
                break;
            case NONE:
            case ADD:
            default:
                this.currentOperation = PatchOperation.ADD;
                break;
        }
        this.current.add(g, s, p, o);
    }

    private void switchOperation(PatchOperation operation) {
        this.collect();
        this.current = DatasetGraphFactory.create();
        this.currentOperation = operation;
    }

    @Override
    public void delete(Node g, Node s, Node p, Node o) {
        // Special handling for labels graph
        if (Telicent.SECURITY_LABELS_GRAPH_URI.equals(g)) {
            this.labelsGraph.delete(s, p, o);
            return;
        }

        switch (this.currentOperation) {
            case ADD:
                switchOperation(PatchOperation.DELETE);
                break;
            case NONE:
            case DELETE:
            default:
                this.currentOperation = PatchOperation.DELETE;
                break;
        }
        this.current.add(g, s, p, o);
    }

    @Override
    public void addPrefix(Node gn, String prefix, String uriStr) {
        this.current.getGraph(gn).getPrefixMapping().setNsPrefix(prefix, uriStr);
    }

    @Override
    public void deletePrefix(Node gn, String prefix) {
        this.current.getGraph(gn).getPrefixMapping().removeNsPrefix(prefix);
    }

    @Override
    public void txnBegin() {
        if (this.inTransaction) {
            throw new IllegalStateException("Nested transactions are not permitted");
        }
        this.resetTransactionState();
        this.inTransaction = true;
        this.labelsGraph.getTransactionHandler().begin();
    }

    @Override
    public void txnCommit() {
        if (!this.inTransaction) {
            throw new IllegalStateException("No active transaction to commit");
        }
        this.collect();
        this.labelsGraph.getTransactionHandler().commit();
        this.resetTransactionState();
    }

    private void collect() {
        if (this.currentOperation != PatchOperation.NONE) {
            this.datasets.add(Pair.of(this.currentOperation, this.current));
        }
    }

    @Override
    public void txnAbort() {
        if (!this.inTransaction) {
            throw new IllegalStateException("No active transaction to abort");
        }

        // Clear out any datasets generated from the current transaction
        while (this.lastTransactionStart < this.datasets.size()) {
            this.datasets.remove(this.lastTransactionStart);
        }
        this.labelsGraph.getTransactionHandler().abort();
        resetTransactionState();
    }

    private void resetTransactionState() {
        this.inTransaction = false;
        this.current = DatasetGraphFactory.create();
        this.currentOperation = PatchOperation.NONE;
        this.lastTransactionStart = this.datasets.size();
    }

    @Override
    public void segment() {
        // Ignored
    }

    @Override
    public void start() {
        this.datasets.clear();
        this.resetTransactionState();
    }

    @Override
    public void finish() {
        if (this.inTransaction) {
            this.txnCommit();
        } else {
            this.collect();
        }
        this.resetTransactionState();
    }
}
