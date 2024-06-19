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
package io.telicent.smart.cache.search.clusters.test.elastic;

import io.telicent.smart.cache.search.clusters.test.AbstractSearchCluster;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.ComparableVersion;
import org.testcontainers.utility.DockerImageName;
import org.testng.Assert;
import org.testng.SkipException;

import java.io.InputStream;
import java.time.Duration;

/**
 * Provides a test ElasticSearch cluster created using Docker Test Containers
 */
public abstract class AbstractElasticTestCluster extends AbstractSearchCluster<ElasticsearchContainer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractElasticTestCluster.class);

    /**
     * Default image to use for ElasticSearch clusters
     */
    public static final DockerImageName DEFAULT_IMAGE;

    /**
     * Default version used for the ElasticSearch image
     */
    public static final String DEFAULT_ELASTIC_IMAGE_VERSION = "7.17.5.2";

    /**
     * The Telicent built ElasticSearch image that includes our synonyms plugin
     */
    public static final String TELICENT_ELASTICSEARCH = "telicent/elasticsearch";

    /**
     * The official ElasticSearch image
     */
    public static final String OFFICIAL_ELASTICSEARCH = "docker.elastic.co/elasticsearch/elasticsearch";

    static {
        // Get the image tag to use for the ElasticSearch test image
        // We build our own images with our synonyms plugin added, this just prepends an extra version section onto the
        // official release version so strip that last version segment off to get the official tag
        String imageTag = System.getProperty("image.elasticsearch", DEFAULT_ELASTIC_IMAGE_VERSION);
        if (StringUtils.isBlank(imageTag)) {
            imageTag = DEFAULT_ELASTIC_IMAGE_VERSION;
        }
        String officialImageTag = imageTag.substring(0, imageTag.lastIndexOf('.'));
        LOGGER.info("Detected ElasticSearch image version is " + imageTag);

        DEFAULT_IMAGE = DockerImageName.parse(TELICENT_ELASTICSEARCH + ":" + imageTag)
                                       .asCompatibleSubstituteFor(OFFICIAL_ELASTICSEARCH + ":" + officialImageTag);
    }

    /**
     * Creates a new Elastic test cluster
     *
     * @param port     Port, or {@code -1} to pick an arbitrary port
     * @param username Username, or {@code null} for no/default username
     * @param password Password, or {@code null} for no password
     */
    public AbstractElasticTestCluster(int port, String username, String password) {
        super(port, username, password);
    }

    @Override
    public void setup() {
        this.container = new FixedPortElasticsearchContainer(DEFAULT_IMAGE, this.port).withStartupTimeout(
                Duration.ofSeconds(300));
        final boolean isElastic8OrNewer = new ComparableVersion(DEFAULT_IMAGE.getVersionPart()).isGreaterThanOrEqualTo("8.0.0");
        if (password != null) {
            this.container.withPassword(password);
        } else {
            if (isElastic8OrNewer) {
                throw new SkipException("ES 8.x requires user authentication with password");
            }
        }
        if (username != null) {
            this.container.withEnv("ELASTIC_USER", username);
        }
        if (StringUtils.isAllBlank(username, password)) {
            this.container.withEnv("xpack.security.enabled", "false");
        }

        this.container.start();

        elasticTlsCaCertString = isElastic8OrNewer ? Base64.encodeBase64String(container.copyFileFromContainer(
                "/usr/share/elasticsearch/config/certs/http_ca.crt",
                InputStream::readAllBytes)) : null;
        Assert.assertTrue(this.container.isRunning(), "ElasticSearch failed to start up in time");

        super.setup();
    }

}
