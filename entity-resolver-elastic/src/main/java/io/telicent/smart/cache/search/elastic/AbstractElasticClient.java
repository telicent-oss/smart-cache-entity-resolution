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
package io.telicent.smart.cache.search.elastic;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.*;
import co.elastic.clients.elasticsearch.cluster.HealthRequest;
import co.elastic.clients.elasticsearch.cluster.HealthResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.json.JsonpDeserializer;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import io.telicent.smart.cache.search.SearchBackend;
import io.telicent.smart.cache.search.SearchException;
import lombok.Getter;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.*;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Abstract base class for components that interact with ElasticSearch
 */
public class AbstractElasticClient implements AutoCloseable, SearchBackend {
    /**
     * An Elastic JSONP deserializer so we can extract out detailed shard failure reasons to surface in our error
     * messages where necessary
     */
    protected static final JsonpDeserializer<List<ShardFailure>> SHARD_FAILURES_DESERIALIZER =
            JsonpDeserializer.arrayDeserializer(ShardFailure._DESERIALIZER);
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractElasticClient.class);
    /**
     * Error reason used when ElasticSearch did not provide a reason
     */
    static final String NO_REASON_PROVIDED = "No error reason provided by ElasticSearch";

    /**
     * Default username for authentication with Elasticsearch
     **/
    public static final String DEFAULT_ELASTICSEARCH_USERNAME = "elastic";
    /**
     * The custom ElasticSearch header in which ElasticSearch Client APIs expect to find the Elastic product name
     * declared
     */
    protected static final String X_ELASTIC_PRODUCT_HEADER = "X-Elastic-Product";

    /**
     * ElasticSearch connection hostname.
     */
    protected final String elasticHost;

    /**
     * ElasticSearch connection port.
     */
    protected final int elasticPort;

    /**
     * The underlying ElasticSearch client
     */
    protected final ElasticsearchClient client;

    /**
     * ElasticSearch username, can be null
     */
    protected String username;

    /**
     * ElasticSearch es_password, can be null
     */
    protected String password;

    /**
     * Creates a new client
     *
     * @param elasticHost              ElasticSearch host
     * @param elasticPort              ElasticSearch port
     * @param username                 ElasticSearch username
     * @param password                 ElasticSearch password
     * @param makeOpenSearchCompatible If {@code true} then the HTTP Client configuration will be customised to make the
     *                                 client "compatible", in so far as is possible, with OpenSearch servers
     */
    protected AbstractElasticClient(String elasticHost, int elasticPort, String username,
                                    String password, boolean makeOpenSearchCompatible) {
        this(elasticHost, elasticPort, username, password, null, makeOpenSearchCompatible);
    }

    /**
     * Creates a new abstract client
     *
     * @param elasticHost              ElasticSearch host
     * @param elasticPort              ElasticSearch port
     * @param username                 ElasticSearch username
     * @param password                 ElasticSearch password
     * @param elasticTlsCaCert         the TLS/SSL trust base-64 encoded certificate of the Elasticsearch service for
     *                                 secure communication, which may be null if security features are disabled
     *                                 (i.e. plain text HTTP only).
     * @param makeOpenSearchCompatible If {@code true} then the HTTP Client configuration will be customised to make the
     *                                 client "compatible", in so far as is possible, with OpenSearch servers
     */
    protected AbstractElasticClient(String elasticHost, int elasticPort, String username,
                                    String password, String elasticTlsCaCert, boolean makeOpenSearchCompatible) {
        this.elasticHost = elasticHost;
        this.elasticPort = elasticPort;
        this.username = username;
        this.password = password;
        this.client = buildElasticClient(elasticHost, elasticPort, username, password, elasticTlsCaCert, makeOpenSearchCompatible);
    }

    /**
     * Builds an ElasticSearch client
     *
     * @param elasticHost              ElasticSearch host
     * @param elasticPort              ElasticSearch port
     * @param userName                 ElasticSearch username - uses 'elastic' by default
     * @param password                 ElasticSearch password
     * @param makeOpenSearchCompatible If {@code true} then the HTTP Client configuration will be customised to make the
     *                                 client "compatible", in so far as is possible, with OpenSearch servers
     * @return ElasticSearch client
     */
    public static ElasticsearchClient buildElasticClient(String elasticHost, int elasticPort, String userName,
                                                         String password, boolean makeOpenSearchCompatible) {
        return buildElasticClient(elasticHost, elasticPort, userName, password, null, makeOpenSearchCompatible);
    }

    /**
     * Builds an ElasticSearch client
     *
     * @param elasticHost              ElasticSearch host
     * @param elasticPort              ElasticSearch port
     * @param userName                 ElasticSearch username - uses 'elastic' by default
     * @param password                 ElasticSearch password
     * @param elasticTlsCaCert         the TLS/SSL trust base-64 encoded certificate of the Elasticsearch service for
     *                                 secure communication, which may be null if security features are disabled
     *                                 (i.e. plain text HTTP only) and/or if trust keystore supplied to the VM.
     * @param makeOpenSearchCompatible If {@code true} then the HTTP Client configuration will be customised to make the
     *                                 client "compatible", in so far as is possible, with OpenSearch servers
     * @return ElasticSearch client
     */
    public static ElasticsearchClient buildElasticClient(String elasticHost, int elasticPort, String userName,
                                                         String password, String elasticTlsCaCert,
                                                         boolean makeOpenSearchCompatible) {
        if (isBlank(elasticHost)) {
            throw new NullPointerException("ElasticSearch Host cannot be null/empty");
        }

        // Translate the provided host and port into a HttpHost that the underlying Elastic Client APIs use to
        // communicate with Elastic
        HttpHost parsedElasticHost = HttpHost.create(elasticHost);
        if (parsedElasticHost.getPort() != -1 && parsedElasticHost.getPort() != elasticPort) {
            throw new IllegalArgumentException(
                    "Elastic port provided in elasticPort differs from port declared in elasticHost variable, unclear which port should be used");
        }
        final RestClientBuilder builder = RestClient.builder(new HttpHost(parsedElasticHost.getHostName(),
                                                                          parsedElasticHost.getPort() != -1 ?
                                                                          parsedElasticHost.getPort() : elasticPort,
                                                                          isNotBlank(elasticTlsCaCert) ? "https" : parsedElasticHost.getSchemeName()));

        List<Function<HttpAsyncClientBuilder, HttpAsyncClientBuilder>> httpCustomisers = new ArrayList<>();

        // set the credentials
        if (isNotBlank(password)) {
            String user = userName;
            if (isBlank(user)) {
                user = DEFAULT_ELASTICSEARCH_USERNAME;
            }
            final CredentialsProvider credentialsProvider =
                    new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY,
                                               new UsernamePasswordCredentials(user, password));
            httpCustomisers.add(c -> c.setDefaultCredentialsProvider(credentialsProvider));
        }
        if (isNotBlank(elasticTlsCaCert)) {
            httpCustomisers.add(c -> c.setSSLContext(createContextFromCaCert(Base64.decodeBase64(elasticTlsCaCert))));
        }
        if (makeOpenSearchCompatible) {
            // OpenSearch compatibility hacks
            //
            // The ElasticSearch client code is cheeky in that it tries to force you to only talk to ElasticSearch
            // servers in a couple of ways.
            // Firstly it requires that responses contain a special X-Elastic-Product which we can inject a suitable
            // value for.
            // Secondly it uses application/vnd.elasticsearch+json as the Content-Type rather than plain
            // application/json which OpenSearch expects.
            httpCustomisers.add(c -> c.addInterceptorLast((HttpResponseInterceptor) (response, context) -> {
                                          if (!response.containsHeader(X_ELASTIC_PRODUCT_HEADER)) {
                                              response.addHeader(X_ELASTIC_PRODUCT_HEADER, "Elasticsearch");
                                          }
                                          response.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
                                      })
                                      .addInterceptorFirst(
                                              (HttpRequestInterceptor) (request, context) -> request.setHeader(
                                                      HttpHeaders.CONTENT_TYPE, "application/json")));
        }
        builder.setHttpClientConfigCallback(clientBuilder -> {
            for (Function<HttpAsyncClientBuilder, HttpAsyncClientBuilder> customiser : httpCustomisers) {
                clientBuilder = customiser.apply(clientBuilder);
            }
            return clientBuilder;
        });

        ElasticsearchTransport transport = new RestClientTransport(builder.build(), new JacksonJsonpMapper());
        return new ElasticsearchClient(transport);
    }

    private static SSLContext createContextFromCaCert(byte[] certAsBytes) {
        try {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            Certificate trustedCa = factory.generateCertificate(
                    new ByteArrayInputStream(certAsBytes)
            );
            KeyStore trustStore = KeyStore.getInstance("pkcs12");
            trustStore.load(null, null);
            trustStore.setCertificateEntry("ca", trustedCa);
            SSLContextBuilder sslContextBuilder =
                    SSLContexts.custom().loadTrustMaterial(trustStore, null);
            return sslContextBuilder.build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    /**
     * Finds the error reason for a bulk response item
     *
     * @param responseItem Bulk response item
     * @return Error reason
     */
    protected static String findErrorReason(BulkResponseItem responseItem) {
        if (responseItem.error() != null) {
            return AbstractElasticClient.findErrorReason(responseItem.error());
        }
        return NO_REASON_PROVIDED;
    }

    /**
     * Finds the error reason in a write response
     *
     * @param response Write response
     * @return Error reason
     */
    protected static String findErrorReason(WriteResponseBase response) {
        if (response.shards().failures() != null) {
            for (ShardFailure failure : response.shards().failures()) {
                if (failure.reason() != null) {
                    return AbstractElasticClient.findErrorReason(failure.reason());
                }
            }
        }
        return NO_REASON_PROVIDED;
    }

    /**
     * Finds the error reason encoded in a cause
     *
     * @param failure Error Cause
     * @return Error reason
     */
    protected static String findErrorReason(ErrorCause failure) {
        StringBuilder builder = new StringBuilder();
        builder.append(failure.reason());
        ErrorCause cause = failure.causedBy();
        while (cause != null) {
            builder.append(" caused by: ").append(cause.reason());
            cause = cause.causedBy();
        }
        return builder.toString();
    }

    /**
     * Converts an ElasticSearch supplied exception into our {@link SearchException}
     *
     * @param e      ElasticSearch exception
     * @param action Attempted action that led to the exception
     * @return Search exception
     */
    protected static SearchException fromElasticException(ElasticsearchException e, String action) {
        StringBuilder error = new StringBuilder();
        error.append("ElasticSearch reported error while attempting to ").append(action);
        error.append(": ").append(e.getMessage());

        // Access shard specific reasons (if present)
        JsonData shardFailures = e.error().metadata().get("failed_shards");
        if (shardFailures != null) {
            List<ShardFailure> failures = shardFailures.deserialize(SHARD_FAILURES_DESERIALIZER);

            // Build a set of shard reasons here because for some failure conditions (e.g. query parse exception) the
            // error message will be identical across all the shards
            Set<String> shardReasons = new LinkedHashSet<>();
            failures.forEach(f -> {
                StringBuilder shardReason = new StringBuilder();
                extractCauses(f.reason(), shardReason);
                shardReasons.add(shardReason.toString());
            });
            shardReasons.forEach(error::append);
        }

        // Also extract any inner causes
        if (e.error() != null) {
            // NB - The top level cause (e.error()) is used to set the message on the ElasticsearchException which we
            //      already added to our error message. Therefore, we skip straight to its cause (if any).
            ErrorCause cause = e.error().causedBy();
            extractCauses(cause, error);
        }

        return new SearchException(error.toString(), e);
    }

    /**
     * Generates a string indicating the causal chain of an ElasticSearch error
     *
     * @param cause Error Cause
     * @param error String builder where error is to be printed
     */
    protected static void extractCauses(ErrorCause cause, StringBuilder error) {
        while (cause != null) {
            // This accounts for the fact that sometimes the ES cause trees will already have copied inner cause reasons
            // up into the cause reasons on a parent cause.  Otherwise, we end up with duplicated output in the eventual
            // error message surfaced to API consumers (and thus users)
            if (!StringUtils.contains(error.toString(), cause.reason())) {
                error.append(" caused by: ").append(cause.reason());
            }
            cause = cause.causedBy();
        }
    }

    @Override
    public void close() throws Exception {
        this.client._transport().close();
    }

    @Override
    public final Boolean isReady() {
        try {
            HealthResponse response =
                    this.client.cluster()
                               .health(HealthRequest.of(h -> h.waitForStatus(HealthStatus.Yellow)
                                                              .timeout(
                                                                      Time.of(t -> t.time("10s")))));
            LOGGER.info("ElasticSearch reported cluster status {}", response.status());

            return switch (response.status()) {
                case Red -> false;
                default -> true;
            };

        } catch (ElasticsearchException e) {
            LOGGER.warn("Failed to determine if ElasticSearch is ready: {}", e.getMessage());
            return false;
        } catch (ConnectionClosedException e) {
            // One of the major causes of Elastic closing the connection are insufficient/incorrect security credentials
            LOGGER.warn("Connection closed by ElasticSearch. Are the security credential correct?: {}", e.getMessage());
            return false;
        } catch (IOException e) {
            LOGGER.warn("Unable to determine if ElasticSearch is ready: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public String name() {
        return "ElasticSearch";
    }

    /**
     * An abstract ElasticSearch client builder, suitable for extending.
     *
     * @param <C> the client subclass being built.
     * @param <B> the corresponding builder subclass.
     */
    public static abstract class AbstractElasticClientBuilder<C extends AbstractElasticClient, B extends AbstractElasticClientBuilder<C, B>> {
        /**
         * The ElasticSearch connection host.
         */
        protected String elasticHost;
        /**
         * The ElasticSearch connection port.
         */
        protected int elasticPort;
        /**
         * The ElasticSearch user.
         */
        protected String username;
        /**
         * The ElasticSearch password.
         */
        protected String password;
        /** The TLS/SSL trust base-64 encoded certificate of the Elasticsearch service for secure communication,
         * which may be null if security features are disabled (i.e. plain text HTTP only). */
        @Getter
        protected String elasticTlsCaCert;
        /**
         * Whether to make the client "compatible" with OpenSearch servers.
         */
        protected boolean makeOpenSearchCompatible;

        /**
         * @param elasticHost configures the ElasticSearch host on the builder, used to construct the client.
         * @return the builder for chaining.
         */
        public B host(String elasticHost) {
            this.elasticHost = elasticHost;
            return self();
        }

        /**
         * @param elasticPort configures the ElasticSearch port on the builder, used to construct the client.
         * @return the builder for chaining.
         */
        public B port(int elasticPort) {
            this.elasticPort = elasticPort;
            return self();
        }

        /**
         * @param username configures the ElasticSearch user on the builder, used to construct the client.
         * @return the builder for chaining.
         */
        public B username(String username) {
            this.username = username;
            return self();
        }

        /**
         * @param password configures the ElasticSearch password on the builder, used to construct the client.
         * @return the builder for chaining.
         */
        public B password(String password) {
            this.password = password;
            return self();
        }

        /**
         * Sets the CA trusted certificate used to verify the SSL connection to Elasticsearch service.
         *
         * @param elasticTlsCaCert the TLS/SSL trust base-64 encoded certificate of the Elasticsearch service for secure
         *                         communication, which may be null if security features are disabled (i.e. plain text
         *                         HTTP only).
         * @return the builder for chaining.
         */
        public B elasticTlsCaCert(String elasticTlsCaCert) {
            this.elasticTlsCaCert = elasticTlsCaCert;
            return self();
        }

        /**
         * @param makeOpenSearchCompatible Configures on the builder whether to make the client "compatible", in so far
         *                                 as is possible, with OpenSearch servers, used to construct the client.
         * @return the builder for chaining.
         */
        public B makeOpenSearchCompatible(boolean makeOpenSearchCompatible) {
            this.makeOpenSearchCompatible = makeOpenSearchCompatible;
            return self();
        }

        /**
         * Returns the builder instance in use.
         *
         * @return this builder, or a subclass thereof.
         */
        protected abstract B self();

        /**
         * Builds the instance.
         *
         * @return the elastic search client subclass instance.
         */
        public abstract C build();

        /**
         * Returns useful state information about the builder.
         *
         * @return the builder state.
         */
        @Override
        public String toString() {
            return "AbstractElasticClient.AbstractElasticClientBuilder(elasticHost=" + this.elasticHost + ", elasticPort=" + this.elasticPort + ", username=" + this.username + ", password=" + this.password + ", makeOpenSearchCompatible=" + this.makeOpenSearchCompatible + ")";
        }
    }

    private static final class AbstractElasticClientBuilderImpl
            extends AbstractElasticClientBuilder<AbstractElasticClient, AbstractElasticClientBuilderImpl> {
        private AbstractElasticClientBuilderImpl() {
        }

        @Override
        protected AbstractElasticClientBuilderImpl self() {
            return this;
        }

        /**
         * Builds the instance.
         *
         * @return the elastic search client subclass instance.
         */
        @Override
        public AbstractElasticClient build() {
            return new AbstractElasticClient(elasticHost, elasticPort, username, password, makeOpenSearchCompatible);
        }
    }
}
