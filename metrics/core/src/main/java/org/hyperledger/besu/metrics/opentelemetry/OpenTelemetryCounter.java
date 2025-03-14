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
package org.idnecology.idn.metrics.opentelemetry;

import org.idnecology.idn.plugin.services.metrics.Counter;
import org.idnecology.idn.plugin.services.metrics.LabelledMetric;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.metrics.LongCounter;

/** The Open telemetry counter. */
public class OpenTelemetryCounter implements LabelledMetric<Counter> {

  private final LongCounter counter;
  private final String[] labelNames;

  /**
   * Instantiates a new Open telemetry counter.
   *
   * @param counter the counter
   * @param labelNames the label names
   */
  public OpenTelemetryCounter(final LongCounter counter, final String... labelNames) {
    this.counter = counter;
    this.labelNames = labelNames;
  }

  @Override
  public Counter labels(final String... labelValues) {
    final AttributesBuilder builder = Attributes.builder();
    for (int i = 0; i < labelNames.length; i++) {
      builder.put(labelNames[i], labelValues[i]);
    }
    final Attributes labels = builder.build();
    return new BoundLongCounter(counter, labels);
  }

  private static class BoundLongCounter implements Counter {
    private final LongCounter counter;
    private final Attributes labels;

    private BoundLongCounter(final LongCounter counter, final Attributes labels) {
      this.counter = counter;
      this.labels = labels;
    }

    @Override
    public void inc() {
      counter.add(1, labels);
    }

    @Override
    public void inc(final long amount) {
      counter.add(amount, labels);
    }
  }
}
