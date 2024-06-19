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
package io.telicent.smart.cache.entity.resolver.server;

import io.telicent.smart.cache.server.jaxrs.applications.AbstractAppEntrypoint;
import io.telicent.smart.cache.server.jaxrs.applications.CorsConfigurationBuilder;
import io.telicent.smart.cache.server.jaxrs.applications.ServerBuilder;

/**
 * An application entrypoint for the Entity Resolution API Server
 */
public class EntityResolutionApiEntrypoint extends AbstractAppEntrypoint {
    private final String hostname;
    final int port;
    private final String contextPath;

    /**
     * Creates a new entrypoint using default hostname ({@code 0.0.0.0}) and port ({@code 8081})
     */
    public EntityResolutionApiEntrypoint() {
        this("0.0.0.0", 8081, ServerBuilder.ROOT_CONTEXT);
    }

    /**
     * Creates a new entrypoint using a custom hostname and port
     *
     * @param hostname Hostname
     * @param port     Port
     */
    public EntityResolutionApiEntrypoint(String hostname, int port) {
        this(hostname, port, ServerBuilder.ROOT_CONTEXT);
    }

    /**
     * Creates a new entrypoint using a custom hostname and port
     *
     * @param hostname    Hostname
     * @param port        Port
     * @param contextPath The context path at which to serve the REST API
     */
    public EntityResolutionApiEntrypoint(String hostname, int port, String contextPath) {
        this.hostname = hostname;
        this.port = port;
        this.contextPath = contextPath;
    }

    /**
     * Runs the server entrypoint in a blocking fashion
     */
    public void run() {
        this.run(true);
    }

    /**
     * Allows running the server in non-blocking mode, intended for testing only
     */
    void runNonBlocking() {
        this.run(false);
    }

    /**
     * Stops the running server, intended for testing only
     * <p>
     * When in normal blocking mode the server stops when the JVM is interrupted
     * </p>
     */
    void stop() {
        this.server.shutdownNow();
    }

    @Override
    protected ServerBuilder buildServer() {
        return ServerBuilder.create()
                            .hostname(this.hostname)
                            .port(this.port)
                            .application(EntityResolutionApplication.class)
                            .withAutoConfigInitialisation()
                            .withAuthExclusions("/healthz", "/version-info")
                            .withVersionInfo("entity-resolver-api-server", "entity-resolver-api", "search-api", "search-index-elastic")
                            .displayName("Entity Resolution API")
                            .contextPath(this.contextPath)
                            .withCors(CorsConfigurationBuilder::withDefaults);
    }
}
