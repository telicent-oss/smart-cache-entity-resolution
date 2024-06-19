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
package io.telicent.smart.cache.search.elastic.utils;

import co.elastic.clients.elasticsearch._types.Script;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * A helper for programmatically generated Elastic Painless scripts
 */
public class PainlessScriptBuilder {
    private final StringBuilder builder = new StringBuilder();
    private final Map<String, JsonData> parameters = new LinkedHashMap<>();

    // Important that this be a linked hash map so functions are output in the order they are defined
    private final Map<String, String> definedFunctions = new LinkedHashMap<>();
    private String nextParameterPrefix = "";
    private char nextParameterChar = 'a';

    /**
     * Gets the parameters allocated/assigned on this script builder
     *
     * @return Parameters
     */
    public Map<String, JsonData> getParameters() {
        return this.parameters;
    }

    /**
     * Adds a parameter
     *
     * @param name  Parameter name
     * @param value Parameter value
     * @return Builder
     */
    public PainlessScriptBuilder addParameter(String name, JsonData value) {
        if (this.parameters.containsKey(name)) {
            throw new IllegalStateException(
                    String.format("Parameter %s is already defined on this builder", name));
        }
        this.parameters.put(name, value);
        return this;
    }

    /**
     * Uses a parameter
     *
     * @param name Parameter name
     * @return Builder
     * @throws IllegalStateException Thrown if the given parameter name has not been defined on this builder
     */
    public PainlessScriptBuilder useParameter(String name) {
        if (!this.parameters.containsKey(name)) {
            throw new IllegalStateException(String.format("Parameter %s has not been defined on this builder", name));
        }

        this.builder.append("params.").append(name);
        return this;
    }

    /**
     * Assigning a new parameter with a generated name
     *
     * @param value Value
     * @return Generated parameter name
     */
    public String assignNewParameter(JsonData value) {
        String name = this.nextParameterPrefix + this.nextParameterChar;
        this.nextParameterChar++;
        if (this.nextParameterChar > 'z') {
            this.nextParameterPrefix = this.nextParameterPrefix + "a";
            this.nextParameterChar = 'a';
        }
        this.parameters.put(name, value);
        return name;
    }

    /**
     * Defines a reusable function within the script
     *
     * @param name               Name
     * @param functionDefinition Function definition
     * @throws IllegalStateException Thrown if a function with the given name is already defined with a different method
     *                               body
     */
    public void defineFunction(String name, String functionDefinition) {
        if (this.definedFunctions.containsKey(name)) {
            if (!StringUtils.equals(this.definedFunctions.get(name), functionDefinition)) {
                throw new IllegalStateException(
                        String.format("Function %s is already defined with a different function definition", name));
            }
        } else {
            this.definedFunctions.put(name, functionDefinition);
        }
    }

    /**
     * Invokes a reusable function previously defined within the script.  This is invoked as a statement within the
     * generated script i.e. it will have {@code ;} appended to the end of the invocation.  If you want to invoke a
     * function as an argument to an invoked function then you need to use
     * {@link #invokeFunctionNested(String, Consumer[])} instead.
     *
     * @param name        Name of the function to invoke
     * @param argBuilders Functions that generate the arguments used to call the function with
     * @throws IllegalStateException Thrown if a function with the given name is not yet defined on this builder
     */
    public void invokeFunction(String name, Consumer<PainlessScriptBuilder>... argBuilders) {
        this.invokeFunctionInternal(name, false, argBuilders);
    }

    /**
     * Invokes a reusable function previously defined within the script in a nested context i.e. does not add a
     * {@code ;} to the end of the invocation
     *
     * @param name        Name of the function to invoke
     * @param argBuilders Functions that generate the arguments used to call the function with
     * @throws IllegalStateException Thrown if a function with the given name is not yet defined on this builder
     */
    public void invokeFunctionNested(String name, Consumer<PainlessScriptBuilder>... argBuilders) {
        this.invokeFunctionInternal(name, true, argBuilders);
    }

    private void invokeFunctionInternal(String name, boolean nested, Consumer<PainlessScriptBuilder>... argBuilders) {
        if (!this.definedFunctions.containsKey(name)) {
            throw new IllegalStateException(
                    String.format("Attempting to invoke function %s which has not been defined on this builder", name));
        }
        this.builder.append(name).append('(');
        for (int i = 0; i < argBuilders.length; i++) {
            argBuilders[i].accept(this);
            if (i < argBuilders.length - 1) {
                this.builder.append(", ");
            }
        }
        if (nested) {
            this.builder.append(")");
        } else {
            this.builder.append(");\n");
        }
    }

    /**
     * Generates a script statement that will add/update the field reached via the given series of keys
     *
     * @param item Item to be added
     * @param keys A series of keys that identify the field to be deleted which may be deeply nested within the document
     *             tree
     * @return Script builder
     */
    public PainlessScriptBuilder addField(Object item, String... keys) {
        if (keys == null || keys.length == 0) {
            throw noPathToField();
        }

        String param = this.assignNewParameter(JsonData.of(item));

        if (keys.length > 1) {
            defineFunction(PainlessFunctions.ENSURE_MAP_FIELD_NAME, PainlessFunctions.ENSURE_MAP_FIELD);
            invokeFunction(PainlessFunctions.ENSURE_MAP_FIELD_NAME, PainlessScriptBuilder::accessSource,
                           k -> k.stringList(ArrayUtils.subarray(keys, 0, keys.length - 1)));
        }
        accessSource();
        accessField(keys);
        builder.append(" = ");
        useParameter(param);
        builder.append(";\n");
        return this;
    }

    private static IllegalArgumentException noPathToField() {
        return new IllegalArgumentException("No path to field to provided");
    }

    /**
     * Generates a script statement that will delete the field reached via the given series of keys
     *
     * @param keys A series of keys that identify the field to be deleted which may be deeply nested within the document
     *             tree
     * @return Script builder
     */
    public PainlessScriptBuilder deleteField(String... keys) {
        if (keys == null || keys.length == 0) {
            throw noPathToField();
        }

        if (keys.length == 1) {
            accessSource();
            removeField(keys[0]);
        } else {
            hasFieldForDeletion(ArrayUtils.subarray(keys, 0, keys.length - 1));

            accessSource();
            for (int i = 0; i < keys.length; i++) {
                if (i < keys.length - 1) {
                    accessField(keys[i]);
                } else {
                    removeField(keys[i]);
                }
            }

            this.builder.append("}\n");
        }
        return this;
    }

    /**
     * Accesses a field identified by the given key
     *
     * @param key Key
     * @return Builder
     */
    private PainlessScriptBuilder accessField(String key) {
        builder.append("['").append(possiblyEscapedString(key)).append("']");
        return this;
    }

    private String possiblyEscapedString(String value) {
        return value.contains("'") ? StringUtils.replace(value, "'", "\\'") : value;
    }

    /**
     * Accesses the document source
     *
     * @return Builder
     */
    public PainlessScriptBuilder accessSource() {
        builder.append("ctx._source");
        return this;
    }

    /**
     * Inserts a string value into the script
     *
     * @param value Value
     * @return Builder
     */
    public PainlessScriptBuilder stringValue(String value) {
        builder.append('\'').append(possiblyEscapedString(value)).append('\'');
        return this;
    }

    /**
     * Inserts a list of string values into the script using the Painless list initializer operator
     *
     * @param items List items
     * @return Builder
     */
    public PainlessScriptBuilder stringList(String[] items) {
        builder.append("[ ");
        for (int i = 0; i < items.length; i++) {
            builder.append('\'').append(possiblyEscapedString(items[i])).append('\'');
            if (i < items.length - 1) {
                builder.append(", ");
            }
        }
        builder.append(" ]");
        return this;
    }

    /**
     * Removes a field
     *
     * @param key Key
     */
    private void removeField(String key) {
        builder.append(".remove('").append(possiblyEscapedString(key)).append("');").append('\n');
    }

    /**
     * Gets the script as a string
     *
     * @return Script string
     */
    @Override
    public String toString() {
        return asScript().inline().source();
    }

    /**
     * Deletes a list item from the list identified by the series of keys
     *
     * @param item Item
     * @param keys A series of keys that identify the field whose list value is to be modified
     */
    public void deleteListItem(Object item, String... keys) {
        hasFieldForDeletion(keys);
        this.defineFunction(PainlessFunctions.REMOVE_FROM_LIST_NAME, PainlessFunctions.REMOVE_FROM_LIST);
        String param = this.assignNewParameter(JsonData.of(item));
        this.invokeFunction(PainlessFunctions.REMOVE_FROM_LIST_NAME, l -> l.accessSource().accessField(keys),
                            i -> i.useParameter(param));
        builder.append("}\n");
    }

    /**
     * Adds an {@code if} check whether the field identified by the series of keys actually exists
     *
     * @param keys A series of keys that identify the field whose existence should be checked
     */
    private void hasFieldForDeletion(String[] keys) {
        defineFunction(PainlessFunctions.HAS_FIELD_NAME, PainlessFunctions.HAS_FIELD);
        this.builder.append("if (");
        this.invokeFunctionNested(PainlessFunctions.HAS_FIELD_NAME, PainlessScriptBuilder::accessSource,
                                  l -> l.stringList(keys));
        this.builder.append(") {\n  ");
    }

    /**
     * Deletes a complex item from the list identified by the given keys
     *
     * @param item     Complex item
     * @param keyField A field used to compare complex items with existing list members
     * @param keys     A series of keys that indicate the field whose list value is to be modified
     */
    public void deleteComplexListItem(Object item, String keyField, String... keys) {
        hasFieldForDeletion(keys);
        defineFunction(PainlessFunctions.REMOVE_FROM_COMPLEX_LIST_NAME,
                       PainlessFunctions.REMOVE_FROM_COMPLEX_LIST);
        String paramName = assignNewParameter(JsonData.of(item));
        invokeFunction(PainlessFunctions.REMOVE_FROM_COMPLEX_LIST_NAME,
                       l -> l.accessSource().accessField(keys), i -> i.useParameter(paramName),
                       k -> k.stringValue(keyField));
        builder.append("}\n");
    }

    /**
     * Adds a list item to the list identified by the series of keys
     *
     * @param item Item
     * @param keys A series of keys that identify the field whose list value is to be modified
     */
    public void addListItem(Object item, String... keys) {
        this.defineFunction(PainlessFunctions.ENSURE_LIST_FIELD_NAME, PainlessFunctions.ENSURE_LIST_FIELD);
        this.defineFunction(PainlessFunctions.ADD_TO_LIST_NAME, PainlessFunctions.ADD_TO_LIST);
        String param = this.assignNewParameter(JsonData.of(item));
        this.invokeFunction(PainlessFunctions.ADD_TO_LIST_NAME,
                            list -> this.invokeFunctionNested(PainlessFunctions.ENSURE_LIST_FIELD_NAME,
                                                              PainlessScriptBuilder::accessSource,
                                                              k -> k.stringList(keys)), i -> i.useParameter(param));
    }

    /**
     * Adds/update a complex item from the list identified by the given keys
     *
     * @param item     Complex item
     * @param keyField A field used to compare complex items with existing list members
     * @param keys     A series of keys that indicate the field whose list value is to be modified
     */
    public void addOrUpdateComplexListItem(Object item, String keyField, String... keys) {
        this.defineFunction(PainlessFunctions.ENSURE_LIST_FIELD_NAME, PainlessFunctions.ENSURE_LIST_FIELD);
        defineFunction(PainlessFunctions.ADD_TO_COMPLEX_LIST_NAME,
                       PainlessFunctions.ADD_TO_COMPLEX_LIST);
        String paramName = assignNewParameter(JsonData.of(item));
        invokeFunction(PainlessFunctions.ADD_TO_COMPLEX_LIST_NAME,
                       list -> list.invokeFunctionNested(PainlessFunctions.ENSURE_LIST_FIELD_NAME,
                                                         PainlessScriptBuilder::accessSource, k -> k.stringList(keys)),
                       i -> i.useParameter(paramName),
                       k -> k.stringValue(keyField));
    }

    /**
     * Access the field identified by the series of keys
     *
     * @param keys Keys
     * @return Builder
     */
    public PainlessScriptBuilder accessField(String[] keys) {
        if (keys == null || keys.length == 0) {
            throw noPathToField();
        }

        for (String key : keys) {
            accessField(key);
        }
        return this;
    }

    /**
     * Produces an ElasticSearch {@link Script} instance based on the current state of the builder
     * <p>
     * Any changes to this builder will not be reflected in the resulting script object.
     * </p>
     *
     * @return Script instance
     */
    public Script asScript() {
        return asScript(false);
    }

    /**
     * Produces an ElasticSearch {@link Script} instance based on the current state of the builder
     * <p>
     * Any changes to this builder will not be reflected in the resulting script object.
     * </p>
     *
     * @param dump Whether to dump the generated script to standard out for debugging purposes
     * @return Script instance
     */
    public Script asScript(boolean dump) {
        StringBuilder scriptBuilder = new StringBuilder();
        for (String functionDefinition : this.definedFunctions.values()) {
            scriptBuilder.append(functionDefinition).append("\n");
        }
        scriptBuilder.append(this.builder);

        if (dump) {
            try {
                System.out.println("---");
                System.out.println(scriptBuilder);
                System.out.println("---");
                if (!this.parameters.isEmpty()) {
                    JacksonJsonpMapper mapper = new JacksonJsonpMapper();
                    mapper.objectMapper().enable(SerializationFeature.INDENT_OUTPUT);
                    for (Map.Entry<String, JsonData> param : this.parameters.entrySet()) {
                        System.out.println(
                                "\"" + param.getKey() + "\": " + param.getValue().toJson(mapper));
                    }
                    System.out.println("---");
                }
            } catch (Throwable e) {
                // Ignore
            }
        }

        return Script.of(
                s -> s.inline(i -> i.lang("painless")
                                    .params(this.getParameters())
                                    .source(scriptBuilder.toString())));
    }
}
