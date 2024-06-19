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

import org.apache.jena.atlas.iterator.Iter;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.TxnType;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.shared.Lock;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.util.Context;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

class DatasetGraphNullGraphs implements DatasetGraph {
    final Set<Node> graphNames = new LinkedHashSet<>();

    public DatasetGraphNullGraphs() {

    }

    public DatasetGraphNullGraphs(Collection<Node> graphNames) {
        this.graphNames.addAll(graphNames);
    }

    @Override
    public Graph getDefaultGraph() {
        return null;
    }

    @Override
    public Graph getGraph(Node node) {
        return null;
    }

    @Override
    public Graph getUnionGraph() {
        return null;
    }

    @Override
    public boolean containsGraph(Node node) {
        return this.graphNames.contains(node);
    }

    @Override
    public void addGraph(Node node, Graph graph) {

    }

    @Override
    public void removeGraph(Node node) {

    }

    @Override
    public Iterator<Node> listGraphNodes() {
        return this.graphNames.iterator();
    }

    @Override
    public void add(Quad quad) {

    }

    @Override
    public void delete(Quad quad) {

    }

    @Override
    public void add(Node node, Node node1, Node node2, Node node3) {

    }

    @Override
    public void delete(Node node, Node node1, Node node2, Node node3) {

    }

    @Override
    public void deleteAny(Node node, Node node1, Node node2, Node node3) {

    }

    @Override
    public Iterator<Quad> find(Quad quad) {
        return Iter.empty();
    }

    @Override
    public Iterator<Quad> find(Node node, Node node1, Node node2, Node node3) {
        return Iter.empty();
    }

    @Override
    public Iterator<Quad> findNG(Node node, Node node1, Node node2, Node node3) {
        return Iter.empty();
    }

    @Override
    public boolean contains(Node node, Node node1, Node node2, Node node3) {
        return false;
    }

    @Override
    public boolean contains(Quad quad) {
        return false;
    }

    @Override
    public void clear() {

    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public Lock getLock() {
        return null;
    }

    @Override
    public Context getContext() {
        return null;
    }

    @Override
    public long size() {
        return 0;
    }

    @Override
    public void close() {

    }

    @Override
    public PrefixMap prefixes() {
        return null;
    }

    @Override
    public boolean supportsTransactions() {
        return false;
    }

    @Override
    public void begin(TxnType txnType) {

    }

    @Override
    public boolean promote(Promote promote) {
        return false;
    }

    @Override
    public void commit() {

    }

    @Override
    public void abort() {

    }

    @Override
    public void end() {

    }

    @Override
    public ReadWrite transactionMode() {
        return null;
    }

    @Override
    public TxnType transactionType() {
        return null;
    }

    @Override
    public boolean isInTransaction() {
        return false;
    }
}
