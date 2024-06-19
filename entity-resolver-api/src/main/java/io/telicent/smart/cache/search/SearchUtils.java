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
package io.telicent.smart.cache.search;

import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.MaxRetriesExceededException;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Search related utility functions
 */
public final class SearchUtils {

    /**
     * Retry registry for search utilities
     */
    private static final RetryRegistry RETRY_REGISTRY = RetryRegistry.ofDefaults();
    private static final Logger LOGGER = LoggerFactory.getLogger(SearchUtils.class);

    static final AtomicBoolean SHUTTING_DOWN = new AtomicBoolean(false);

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> SHUTTING_DOWN.set(true)));
    }

    private SearchUtils() {
    }

    /**
     * Waits for a search backend to become ready
     * <p>
     * This uses resilience4j's {@link Retry} API to perform an exponential backoff retry, the parameters for the number
     * of attempts and the min and max connection intervals can be customised as desired.
     * </p>
     *
     * @param backend            Search backend
     * @param maxConnectAttempts Maximum connection attempts
     * @param minConnectInterval Minimum connection attempt interval in seconds
     * @param maxConnectInterval Maximum connection attempt interval in seconds
     */
    public static void waitForReady(SearchBackend backend, int maxConnectAttempts, long minConnectInterval,
                                    long maxConnectInterval) {
        Objects.requireNonNull(backend, "Search backend cannot be null");

        // Backend might not be up yet so wait for it to come up
        Duration initialInterval = Duration.ofSeconds(minConnectInterval);
        Duration maxInterval = Duration.ofSeconds(maxConnectInterval);
        SearchUtils.waitForReady(backend, maxConnectAttempts, initialInterval, maxInterval);
    }

    /**
     * Waits for a search backend to become ready
     * <p>
     * This uses resilience4j's {@link Retry} API to perform an exponential backoff retry, the parameters for the number
     * of attempts and the min and max connection intervals can be customised as desired.
     * </p>
     *
     * @param backend            Search backend
     * @param maxConnectAttempts Maximum connection attempts
     * @param minConnectInterval Minimum connection attempt
     * @param maxConnectInterval Maximum connection attempt
     * @throws SearchException Thrown if the backend does not become ready with the given retry settings
     */
    public static void waitForReady(SearchBackend backend, int maxConnectAttempts, Duration minConnectInterval,
                                    Duration maxConnectInterval) {
        Objects.requireNonNull(backend, "Search backend cannot be null");

        if (SHUTTING_DOWN.get()) {
            notWaitingDueToShutdown(backend);
        }

        RetryConfig config =
                SearchUtils.<Boolean>prepareRetry(maxConnectAttempts, minConnectInterval, maxConnectInterval)
                           .retryOnResult(r -> !Boolean.TRUE.equals(r) && !Boolean.TRUE.equals(SHUTTING_DOWN.get()))
                           .build();
        // IMPORTANT - We have to create a unique name here based upon the parameters of the retry otherwise if we are
        //             called multiple times with different parameters we'd reuse the retry configuration from the first
        //             time we were called and potentially not retry as expected.
        Retry retry = getRetry(
                String.format("is-%s-up-%d-%d-%d", backend.name(), maxConnectAttempts, minConnectInterval.toMillis(),
                              maxConnectInterval.toMillis()), config);
        AtomicInteger attempts = new AtomicInteger();
        try {
            LOGGER.info("Waiting to see if {} is ready ({} attempts maximum)...", backend.name(), maxConnectAttempts);
            Boolean ready = retry.executeCallable(() -> {
                LOGGER.info("Seeing if {} is ready yet (Attempt #{})", backend.name(), attempts.incrementAndGet());
                return backend.isReady();
            });
            if (!Boolean.TRUE.equals(ready)) {
                if (SHUTTING_DOWN.get()) {
                    notWaitingDueToShutdown(backend);
                }

                LOGGER.error("{} failed to become ready!", backend.name());
                throw new SearchException(String.format("%s failed to become ready after %d attempts", backend.name(),
                                                        maxConnectAttempts));
            }
            LOGGER.info("{} is now ready", backend.name());
        } catch (SearchException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("Unexpected error waiting for {} to become ready: {}", backend.name(), e.getMessage());
            throw new SearchException(String.format("Unexpected error waiting for %s to become ready", backend.name()),
                                      e);
        }
    }

    private static void notWaitingDueToShutdown(SearchBackend backend) {
        LOGGER.warn("Stopped waiting for {} to become ready due to JVM Shutdown", backend.name());
        throw new SearchException(
                String.format("JVM shutting down, aborted waiting for %s to become ready", backend.name()));
    }


    /**
     * Prepares a basic retry configuration returning a builder for further customisation
     *
     * @param maxAttempts Maximum attempts
     * @param minInterval Minimum interval
     * @param maxInterval Maximum interval
     * @param <T>         Return type of the callable that will be retried with this configuration
     * @return Retry configuration builder
     */
    public static <T> RetryConfig.Builder<T> prepareRetry(int maxAttempts, Duration minInterval, Duration maxInterval) {
        return RetryConfig.<T>custom()
                          .maxAttempts(maxAttempts)
                          .intervalFunction(IntervalFunction.ofExponentialBackoff(minInterval, 2, maxInterval));
    }

    /**
     * Gets or creates a Retry from the registry
     *
     * @param name   Retry name
     * @param config Retry configuration
     * @return Retry
     */
    public static Retry getRetry(String name, RetryConfig config) {
        return RETRY_REGISTRY.retry(name, config);
    }

    /**
     * Retries an index related operation logging the number of attempts that were required
     * <p>
     * The return value will be the result of a successful attempt if the operation succeeds, or if all attempts fail
     * the result of the final attempt.  If the final attempt throws an error then that error is thrown, wrapped into a
     * {@link SearchException} if necessary.
     * </p>
     *
     * @param backend   Search backend
     * @param index     Index being operated upon
     * @param operation Operation descriptor, used in logging
     * @param config    Retry configuration
     * @param callable  Callable that performs the actual operation
     * @param <T>       Return type of the callable
     * @return Callable result
     * @throws SearchException Thrown if the operation does not succeed with the configured retry settings
     */
    public static <T> T retryIndexOperation(SearchBackend backend, String index, String operation, RetryConfig config,
                                            Callable<T> callable) {
        Retry retry = SearchUtils.getRetry(generateRetryName(backend, operation, config), config);
        AtomicInteger attempts = new AtomicInteger(1);
        try {
            return retry.executeCallable(() -> {
                logAttempt(backend, index, operation, retry, attempts);
                return callable.call();
            });
        } catch (SearchException e) {
            throw e;
        } catch (MaxRetriesExceededException e) {
            throw maxRetriesExceeded(backend, index, operation, retry);
        } catch (Exception e) {
            throw unexpectedError(backend, index, operation, e);
        }
    }

    /**
     * Generates a retry name
     *
     * @param backend   Search backend name
     * @param operation Search operation
     * @param config    Retry configuration
     * @return Retry name
     */
    static String generateRetryName(SearchBackend backend, String operation, RetryConfig config) {
        Long minRetryInterval = config.getIntervalBiFunction().apply(1, null);
        Long maxRetryInterval = config.getIntervalBiFunction().apply(config.getMaxAttempts(), null);
        return String.format("%s-%s-%s-%s-%s", backend.name(), operation, config.getMaxAttempts(), minRetryInterval,
                             maxRetryInterval);
    }

    private static void logAttempt(SearchBackend backend, String index, String operation, Retry retry,
                                   AtomicInteger attempts) {
        LOGGER.debug("Attempting to {} {} index {} (Attempt {} of {})...", operation, backend.name(), index,
                     attempts.getAndIncrement(), retry.getRetryConfig().getMaxAttempts());
    }

    private static SearchException maxRetriesExceeded(SearchBackend backend, String index, String operation,
                                                      Retry retry) {
        LOGGER.error("Attempt to {} {} index {} exceeded maximum retries ({})", operation, backend.name(), index,
                     retry.getRetryConfig().getMaxAttempts());
        return new SearchException(
                String.format("Attempt to %s %s index %s exceeded maximum retries (%d)", operation, backend.name(),
                              index, retry.getRetryConfig().getMaxAttempts()));
    }

    /**
     * Retries an index related operation logging the number of attempts that were required
     * <p>
     * If the operation fails then the error from the last attempt will be thrown, wrapped into a
     * {@link SearchException} where necessary.
     * </p>
     *
     * @param backend   Search backend
     * @param index     Index being operated upon
     * @param operation Operation descriptor, used in logging
     * @param config    Retry configuration
     * @param runnable  Runnable operation
     * @throws SearchException Thrown if the operation does not succeed with the configured retry settings
     */
    public static void retryIndexOperation(SearchBackend backend, String index, String operation, RetryConfig config,
                                           Runnable runnable) {
        Retry retry = SearchUtils.getRetry(generateRetryName(backend, operation, config), config);
        AtomicInteger attempts = new AtomicInteger(1);
        try {
            retry.executeRunnable(() -> {
                logAttempt(backend, index, operation, retry, attempts);
                runnable.run();
            });
            // NB - We don't bother trying to catch MaxRetriesException here because it can never occur when executing
            // a runnable.  It can only be thrown if retrying a callable and the configured retryOnResult() predicate
            // still returns false on the final attempt
        } catch (SearchException e) {
            throw e;
        } catch (Exception e) {
            throw unexpectedError(backend, index, operation, e);
        }
    }

    private static SearchException unexpectedError(SearchBackend backend, String index, String operation, Exception e) {
        LOGGER.error("Unexpected error ({}) attempting to {} {} index {}: {}", e.getClass().getSimpleName(), operation,
                     backend.name(), index,
                     e.getMessage());
        throw new SearchException(
                String.format("Unexpected error attempting to %s %s index %s", operation, backend.name(), index),
                e);
    }
}
