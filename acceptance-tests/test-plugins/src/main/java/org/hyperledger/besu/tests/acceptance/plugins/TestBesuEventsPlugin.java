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
package org.idnecology.idn.tests.acceptance.plugins;

import org.idnecology.idn.plugin.IdnPlugin;
import org.idnecology.idn.plugin.ServiceManager;
import org.idnecology.idn.plugin.data.BlockHeader;
import org.idnecology.idn.plugin.data.PropagatedBlockContext;
import org.idnecology.idn.plugin.services.IdnEvents;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.auto.service.AutoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AutoService(IdnPlugin.class)
public class TestIdnEventsPlugin implements IdnPlugin {
  private static final Logger LOG = LoggerFactory.getLogger(TestIdnEventsPlugin.class);

  private ServiceManager context;

  private Optional<Long> subscriptionId;
  private final AtomicInteger blockCounter = new AtomicInteger();
  private File callbackDir;

  @Override
  public void register(final ServiceManager context) {
    this.context = context;
    LOG.info("Registered");
    callbackDir = new File(System.getProperty("idn.plugins.dir", "plugins"));
  }

  @Override
  public void start() {
    subscriptionId =
        context
            .getService(IdnEvents.class)
            .map(events -> events.addBlockPropagatedListener(this::onBlockAnnounce));
    LOG.info("Listening with ID#" + subscriptionId);
  }

  @Override
  public void stop() {
    subscriptionId.ifPresent(
        id ->
            context
                .getService(IdnEvents.class)
                .ifPresent(idnEvents -> idnEvents.removeBlockPropagatedListener(id)));
    LOG.info("No longer listening with ID#" + subscriptionId);
  }

  private void onBlockAnnounce(final PropagatedBlockContext propagatedBlockContext) {
    final BlockHeader header = propagatedBlockContext.getBlockHeader();
    final int blockCount = blockCounter.incrementAndGet();
    LOG.info("I got a new block! (I've seen {}) - {}", blockCount, header);
    try {
      final File callbackFile = new File(callbackDir, "newBlock." + blockCount);
      if (!callbackFile.getParentFile().exists()) {
        callbackFile.getParentFile().mkdirs();
        callbackFile.getParentFile().deleteOnExit();
      }
      Files.write(callbackFile.toPath(), Collections.singletonList(header.toString()));
      callbackFile.deleteOnExit();
    } catch (final IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }
}
