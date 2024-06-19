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
package io.telicent.smart.cache.canonical.utility;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.telicent.smart.cache.canonical.configuration.CanonicalTypeConfiguration;
import io.telicent.smart.cache.canonical.configuration.CanonicalTypeConfigurationDeserializer;
import io.telicent.smart.cache.canonical.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

/**
 * Utility class to write objects to JSON strings
 */
public final class Mapper {
    private Mapper() {
    }

    private static ObjectMapper jsonMapper;
    private static ObjectMapper yamlMapper;
    private static final Logger LOGGER = LoggerFactory.getLogger(Mapper.class);

    /**
     * Acquire an Object Mapper
     * @return the object mapper
     */
    public static ObjectMapper getJsonMapper() {
        if (null == jsonMapper) {
            jsonMapper = new ObjectMapper();
            SimpleModule simpleModule = new SimpleModule();
            simpleModule.addDeserializer(CanonicalTypeConfiguration.class, new CanonicalTypeConfigurationDeserializer());
            jsonMapper.registerModule(simpleModule);
        }
        return jsonMapper;
    }

    public static ObjectMapper getYamlMapper() {
        if (null == yamlMapper) {
            yamlMapper = new ObjectMapper(new YAMLFactory()).findAndRegisterModules();
        }
        return yamlMapper;
    }

    public static <T> ObjectReader getObjectReader(Class<T> clz) {
        return getYamlMapper().readerFor(clz);
    }

    /**
     * For testing purposes
     * @param mapper an explicit mapper to use
     */
    public static void setJsonMapper(ObjectMapper mapper) {
        jsonMapper = mapper;
    }

    /**
     * Writes an object to string or throws Validation Exception
     * (as everything should be able to)
     * @param object object to write
     * @return a String (JSON) representation of the class
     */
    public static String writeValueAsString(Object object) {
        try {
            return getJsonMapper().writeValueAsString(object);
        } catch (IOException e) {
            LOGGER.error("Failed to write object to string", e);
            throw new ValidationException(e);
        }
    }

    public static <T> T loadFromConfigFile(Class<T> clz, String fileName) {
        try {
            return getObjectReader(clz).readValue(new File(fileName));
        } catch (IllegalArgumentException | IOException e) {
            LOGGER.error("Failed to load configuration from file", e);
            throw new ValidationException(e);
        }
    }

    public static <T> T loadFromString(Class<T> clz, String representation) {
        try {
            return getObjectReader(clz).readValue(representation);
        } catch (IllegalArgumentException | IOException e) {
            LOGGER.error("Failed to load configuration from string", e);
            throw new ValidationException(e);
        }
    }

    public static <T> T loadFromString(TypeReference<T> typeReference, String representation) {
        try {
            return getJsonMapper().readValue(representation, typeReference);
        } catch (IllegalArgumentException | IOException e) {
            LOGGER.error("Failed to load configuration from string", e);
            throw new ValidationException(e);
        }
    }

    /**
     * Takes a potentially partial JSON String representation of class and only updates the fields presented
     * @param obj The instance of the class to be updated
     * @param jsonString The (partial) JSON string representation
     * @return the updated object
     * @param <T> The class in question
     */
    public static <T> T updateFieldsFromJSON(T obj, String jsonString) {
        try {
            JsonNode jsonNode = getJsonMapper().readTree(jsonString);

            // Iterate over all fields of the object's class
            for (Field field : obj.getClass().getDeclaredFields()) {
                // Make the field accessible if it is private
                field.setAccessible(true);

                // Check if the JSON contains this field
                if (jsonNode.has(field.getName())) {
                    JsonNode fieldValue = jsonNode.get(field.getName());
                    try {
                        // Update the field value in the object based on the JSON value
                        if (field.getType().equals(Long.class) || field.getType().getName().equalsIgnoreCase("long")) {
                            field.set(obj, jsonNode.get(field.getName()).asLong());
                        } else if (field.getType().equals(String.class)) {
                            field.set(obj, jsonNode.get(field.getName()).asText());
                        } else if (field.getType().equals(Integer.class) || field.getType().getName().equalsIgnoreCase("int")) {
                            field.set(obj, jsonNode.get(field.getName()).asInt());
                        } else if (field.getType().equals(Boolean.class) || field.getType().getName().equalsIgnoreCase("boolean")) {
                            field.set(obj, jsonNode.get(field.getName()).asBoolean());
                        } else if (List.class.isAssignableFrom(field.getType())) {
                            // Handle List types
                            Class<?> listType = getListType(field);
                            if (listType != null) {
                                List<?> list = jsonMapper.convertValue(fieldValue, jsonMapper.getTypeFactory()
                                                                                             .constructCollectionType(
                                                                                                     List.class,
                                                                                                     listType));
                                field.set(obj, list);
                            }
                        } else if (Map.class.isAssignableFrom(field.getType())) {
                            // Handle Map types
                            Class<?> keyType = getMapKeyType(field);
                            Class<?> valueType = getMapValueType(field);
                            if (keyType != null && valueType != null) {
                                Map<?, ?> map = jsonMapper.convertValue(fieldValue,
                                                                        jsonMapper.getTypeFactory().constructMapType(
                                                                                Map.class, keyType, valueType));
                                field.set(obj, map);
                            }
                        } else {
                            // Handle other object types
                            Object value = jsonMapper.treeToValue(fieldValue, field.getType());
                            field.set(obj, value);
                        }
                    } catch (IllegalAccessException e) {
                        throw new ValidationException("Error updating field: " + field.getName(), e);
                    }
                }
            }
        } catch (JsonProcessingException e) {
            throw new ValidationException(e);
        }
        return obj;
    }

    /**
     * Extract generic type information for a List
     * @param field a List
     * @return the Class (i.e. List<Class>)
     */
    private static Class<?> getListType(Field field) {
        return (Class<?>) ((java.lang.reflect.ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
    }
    /**
     * Extract generic type information for a Map
     * @param field a Map
     * @return the Class of the Key K (i.e. Map<K,V>)
     */
    private static Class<?> getMapKeyType(Field field) {
        return (Class<?>) ((java.lang.reflect.ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
    }
    /**
     * Extract generic type information for a Map
     * @param field a Map
     * @return the Class of the Value V (i.e. Map<K,V>)
     */

    private static Class<?> getMapValueType(Field field) {
            return (Class<?>) ((java.lang.reflect.ParameterizedType) field.getGenericType()).getActualTypeArguments()[1];
    }
}
