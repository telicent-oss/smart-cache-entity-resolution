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
package io.telicent.smart.cache.search.model.utils;

import java.util.*;
import java.util.function.BiConsumer;

/**
 * A content visitor that looks for sections of the document that match one/more path expressions and invoke a callback
 * on each matched section.
 * <p>
 * A path expression is given in dotted style where {@code *} indicates a wildcard, for example {@code *.types} would
 * match any section of the document where the leaf field name is {@code types}.  Note that each path segment given in
 * the expression is mandatory so the same expression <strong>DOES NOT</strong> match a top level {@code types} field.
 * </p>
 * <p>
 * Path expressions are evaluated against the leaf nodes of the document, though these leaf nodes may themselves be
 * complex objects.  Where an expression could match the entirety of the leaf, or a subset of it, the larger section of
 * the document is always returned to the callback function and the visitor does not recurse further.
 * </p>
 */
public class PathMatchingVisitor extends ContentLeafVisitor {

    private final BiConsumer<String[], Object> onMatchedPath;
    private final List<FieldNameExpression> matchExpressions = new ArrayList<>();

    /**
     * Compiles path match expressions supplied as a list of strings into their compiled {@link FieldNameExpression}
     * form.
     * <p>
     * Since expressions are internally implemented via regular expressions it is advantageous to compile the
     * expressions once if they are going to be repeatedly used.
     * </p>
     *
     * @param expressions Path match expressions
     * @return Compiled path match expressions
     */
    public static List<FieldNameExpression> compileExpressions(List<String> expressions) {
        Objects.requireNonNull(expressions, "Expressions to compile cannot be null");
        if (expressions.isEmpty()) {
            throw new IllegalArgumentException("Expressions to compile cannot be empty");
        }
        List<FieldNameExpression> compiled = new ArrayList<>();
        for (String expression : expressions) {
            compiled.add(new FieldNameExpression(expression));
        }
        return compiled;
    }

    /**
     * Creates a new path matching visitor
     *
     * @param onMatchedPath        Consumer function that is called when a path is matched
     * @param pathMatchExpressions Compiled path match expressions
     */
    public PathMatchingVisitor(BiConsumer<String[], Object> onMatchedPath,
                               List<FieldNameExpression> pathMatchExpressions) {
        super(new String[0]);
        Objects.requireNonNull(onMatchedPath, "onMatchedPath function cannot be null");
        Objects.requireNonNull(pathMatchExpressions, "Path Match Expressions cannot be null");
        if (pathMatchExpressions.isEmpty()) {
            throw new IllegalArgumentException("Must supply at least one path match expression");
        }
        this.onMatchedPath = onMatchedPath;
        this.matchExpressions.addAll(pathMatchExpressions);
    }

    private boolean isPathMatch(String[] path) {
        for (FieldNameExpression matchExpression : this.matchExpressions) {
            if (matchExpression.matches(path)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void visitLeafField(String[] path, Object item) {
        if (isPathMatch(path)) {
            this.onMatchedPath.accept(path, item);
        }
    }

    @Override
    public void visitComplexListItem(String[] path, Object item) {
        if (isPathMatch(path)) {
            this.onMatchedPath.accept(path, item);
        } else if (item instanceof Map<?, ?>) {
            Stack<String> keys = new Stack<>();
            keys.addAll(Arrays.asList(path));
            this.visitInternal((Map<String, Object>) item, keys);
        }
    }

    @Override
    public void visitNestedListItem(String[] path, List<Object> item) {
        if (isPathMatch(path)) {
            this.onMatchedPath.accept(path, item);
        } else {
            Stack<String> keys = new Stack<>();
            keys.addAll(Arrays.asList(path));
            this.visitInternal(item, keys);
        }
    }

    @Override
    public void visitListItem(String[] path, Object item) {
        if (isPathMatch(path)) {
            this.onMatchedPath.accept(path, item);
        }
    }

}
