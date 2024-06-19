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
package io.telicent.smart.cache.search.model;

/**
 * Represents the ID for a results and the associated score
 */
public class Hit {

    private String id;
    private double score;
    private Document document;

    /**
     * Creates a new empty hit
     */
    public Hit() {
    }

    /**
     * Creates a new search hit
     *
     * @param id       Document ID
     * @param score    Document Score
     * @param document Document
     */
    public Hit(String id, double score, Document document) {
        this.id = id;
        this.score = score;
        this.document = document;
    }

    /**
     * Gets the ID of the document
     *
     * @return ID
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the ID of the document
     *
     * @param id ID
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the score for the document
     *
     * @return Score
     */
    public double getScore() {
        return score;
    }

    /**
     * Sets the score for the document
     *
     * @param score Score
     */
    public void setScore(double score) {
        this.score = score;
    }

    /**
     * Gets the document
     *
     * @return Document
     */
    public Document getDocument() {
        return document;
    }

    /**
     * Sets the document
     *
     * @param document Document
     */
    public void setDocument(Document document) {
        this.document = document;
    }

    @Override
    public String toString() {
        return "{\n"
                + "\"id\": \""
                + this.id
                + "\n, \"score\": "
                + this.score
                + ",\n \"documentPresent\": "
                + (this.document != null)
                + "\n}";
    }
}
