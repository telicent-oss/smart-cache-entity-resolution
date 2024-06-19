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

import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import com.github.rvesse.airline.annotations.restrictions.MutuallyExclusiveWith;
import com.github.rvesse.airline.annotations.restrictions.Port;
import com.github.rvesse.airline.annotations.restrictions.PortType;
import com.github.rvesse.airline.model.CommandMetadata;
import io.telicent.smart.cache.cli.commands.SmartCacheCommand;
import io.telicent.smart.cache.live.model.IODescriptor;
import io.telicent.smart.cache.server.jaxrs.applications.ServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A CLI command for starting the Entity Resolution API Server
 */
@Command(name = "entity-resolution-api-server", description = "Runs the Entity Resolution API Server")
public class EntityResolutionApiCommand extends SmartCacheCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityResolutionApiCommand.class);

    @Option(name = {"--localhost"}, description = "When specified only listen for incoming requests on localhost.  Defaults to listening to incoming requests on all interfaces.")
    @MutuallyExclusiveWith(tag = "host-selection")
    private boolean localhost;

    @Option(name = {
            "--host",
            "--hostname"
    }, description = "Specifies the Hostname or IP Address to listen for incoming requests on.  Defaults to 0.0.0.0, i.e., all interfaces.")
    @MutuallyExclusiveWith(tag = "host-selection")
    private String hostname = "0.0.0.0";

    @Option(name = {
            "-p",
            "--port"
    }, title = "Port", description = "Specifies the HTTP Port on which the Entity Resolution API Server will listen.")
    @Port(acceptablePorts = {PortType.ANY})
    private int port = 8081;

    @Option(name = "--base-path", title = "BasePath", description = "Specifies the Base URL Path upon which the server should expect to receive requests.  By default this is /, i.e., the server expects URLs to contain no prefix and appear exactly as defined in its Open API specification.  If the server is behind a load balancer or other routing layer then requests may arrive via /api/search or similar in which case the server must be aware of this base URL path in order to route requests correctly.")
    private String basePath = ServerBuilder.ROOT_CONTEXT;

    @Override
    public int run() {
        EntityResolutionApiEntrypoint entrypoint =
                new EntityResolutionApiEntrypoint(this.localhost ? "localhost" : this.hostname, this.port, this.basePath);
        entrypoint.run();
        return 0;
    }

    /**
     * Entrypoint for the CLI
     *
     * @param args CLI arguments passed to the JVM
     */
    public static void main(String[] args) {
        SmartCacheCommand.runAsSingleCommand(EntityResolutionApiCommand.class, args);
    }

    @Override
    protected void setupLiveReporter(CommandMetadata metadata) {
        //@formatter:off
        this.liveReporter.setupLiveReporter(null,
                                            "Entity Resolution API Server",
                                            "entity-resolution-api-server",
                                            "smartcache",
                                            new IODescriptor("elasticsearch", "smartcache"),
                                            new IODescriptor("entity-resolution", "rest-api"));
        //@formatter:on
    }
}
