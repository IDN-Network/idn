/*
 * Copyright ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.idnecology.idn.metrics;

import org.idnecology.idn.metrics.opentelemetry.MetricsOtelPushService;
import org.idnecology.idn.metrics.opentelemetry.OpenTelemetrySystem;
import org.idnecology.idn.metrics.prometheus.MetricsConfiguration;
import org.idnecology.idn.metrics.prometheus.MetricsHttpService;
import org.idnecology.idn.metrics.prometheus.MetricsPushGatewayService;
import org.idnecology.idn.metrics.prometheus.PrometheusMetricsSystem;
import org.idnecology.idn.plugin.services.MetricsSystem;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.slf4j.LoggerFactory;

/**
 * Service responsible for exposing metrics to the outside, either through a port and network
 * interface or pushing.
 */
public interface MetricsService {

  /**
   * Create Metrics Service.
   *
   * @param configuration the configuration
   * @param metricsSystem the metrics system
   * @return the optional Metrics Service
   */
  static Optional<MetricsService> create(
      final MetricsConfiguration configuration, final MetricsSystem metricsSystem) {
    LoggerFactory.getLogger(MetricsService.class)
        .trace("Creating metrics service {}", configuration.getProtocol());
    if (configuration.getProtocol() == MetricsProtocol.PROMETHEUS) {
      if (configuration.isEnabled()) {
        return Optional.of(
            new MetricsHttpService(configuration, (PrometheusMetricsSystem) metricsSystem));
      } else if (configuration.isPushEnabled()) {
        return Optional.of(
            new MetricsPushGatewayService(configuration, (PrometheusMetricsSystem) metricsSystem));
      } else {
        return Optional.empty();
      }
    } else if (configuration.getProtocol() == MetricsProtocol.OPENTELEMETRY) {
      if (configuration.isEnabled()) {
        return Optional.of(new MetricsOtelPushService((OpenTelemetrySystem) metricsSystem));
      } else {
        return Optional.empty();
      }
    } else {
      return Optional.empty();
    }
  }

  /**
   * Starts the Metrics Service and all needed backend systems.
   *
   * @return completion state
   */
  CompletableFuture<?> start();

  /**
   * Stops the Metrics Service and performs needed cleanup.
   *
   * @return completion state
   */
  CompletableFuture<?> stop();

  /**
   * If serving to a port on the client, what the port number is.
   *
   * @return Port number optional, serving if present.
   */
  Optional<Integer> getPort();
}
