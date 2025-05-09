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
package org.idnecology.idn.cli.subcommands.operator;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.idnecology.idn.cli.DefaultCommandValues.MANDATORY_LONG_FORMAT_HELP;
import static org.idnecology.idn.ethereum.api.query.cache.TransactionLogBloomCacher.BLOCKS_PER_BLOOM_CACHE;

import org.idnecology.idn.cli.util.VersionProvider;
import org.idnecology.idn.controller.IdnController;
import org.idnecology.idn.ethereum.api.query.cache.TransactionLogBloomCacher;
import org.idnecology.idn.ethereum.chain.MutableBlockchain;
import org.idnecology.idn.ethereum.eth.manager.EthScheduler;
import org.idnecology.idn.metrics.noop.NoOpMetricsSystem;

import java.nio.file.Path;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

/** The generate-log-bloom-cache CLI command. */
@Command(
    name = "generate-log-bloom-cache",
    description = "Generate cached values of block log bloom filters.",
    mixinStandardHelpOptions = true,
    versionProvider = VersionProvider.class)
public class GenerateLogBloomCache implements Runnable {

  @Option(
      names = "--start-block",
      paramLabel = MANDATORY_LONG_FORMAT_HELP,
      description =
          "The block to start generating the cache.  Must be an increment of "
              + BLOCKS_PER_BLOOM_CACHE
              + " (default: ${DEFAULT-VALUE})",
      arity = "1..1")
  private final Long startBlock = 0L;

  @Option(
      names = "--end-block",
      paramLabel = MANDATORY_LONG_FORMAT_HELP,
      description = "The block to stop generating the cache (default is last block of the chain).",
      arity = "1..1")
  private final Long endBlock = Long.MAX_VALUE;

  @ParentCommand private OperatorSubCommand parentCommand;

  /** Default constructor. */
  public GenerateLogBloomCache() {}

  @SuppressWarnings("ResultOfMethodCallIgnored")
  @Override
  public void run() {
    checkPreconditions();
    final Path cacheDir = parentCommand.parentCommand.dataDir().resolve(IdnController.CACHE_PATH);
    cacheDir.toFile().mkdirs();
    final MutableBlockchain blockchain =
        createIdnController().getProtocolContext().getBlockchain();
    final EthScheduler scheduler = new EthScheduler(1, 1, 1, 1, new NoOpMetricsSystem());
    try {
      final long finalBlock = Math.min(blockchain.getChainHeadBlockNumber(), endBlock);
      final TransactionLogBloomCacher cacher =
          new TransactionLogBloomCacher(blockchain, cacheDir, scheduler);
      cacher.generateLogBloomCache(startBlock, finalBlock);
    } finally {
      scheduler.stop();
      try {
        scheduler.awaitStop();
      } catch (final InterruptedException e) {
        // ignore
      }
    }
  }

  private void checkPreconditions() {
    checkNotNull(parentCommand.parentCommand.dataDir());
    checkState(
        startBlock % BLOCKS_PER_BLOOM_CACHE == 0,
        "Start block must be an even increment of %s",
        BLOCKS_PER_BLOOM_CACHE);
  }

  private IdnController createIdnController() {
    return parentCommand.parentCommand.buildController();
  }
}
