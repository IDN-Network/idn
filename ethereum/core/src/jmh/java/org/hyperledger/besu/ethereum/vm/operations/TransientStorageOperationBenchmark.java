/*
 * Copyright contributors to Idn ecology Idn.
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
package org.idnecology.idn.ethereum.vm.operations;

import static org.idnecology.idn.ethereum.core.InMemoryKeyValueStorageProvider.createInMemoryWorldStateArchive;
import static org.mockito.Mockito.mock;

import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.Wei;
import org.idnecology.idn.ethereum.chain.Blockchain;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.BlockHeaderTestFixture;
import org.idnecology.idn.ethereum.core.MessageFrameTestFixture;
import org.idnecology.idn.ethereum.worldstate.WorldStateArchive;
import org.idnecology.idn.evm.frame.MessageFrame;
import org.idnecology.idn.evm.gascalculator.CancunGasCalculator;
import org.idnecology.idn.evm.operation.TLoadOperation;
import org.idnecology.idn.evm.operation.TStoreOperation;
import org.idnecology.idn.evm.worldstate.WorldUpdater;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt256;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

@State(Scope.Thread)
public class TransientStorageOperationBenchmark {

  private OperationBenchmarkHelper operationBenchmarkHelper;
  private TStoreOperation tstore;
  private TLoadOperation tload;
  private MessageFrame frame;

  private MessageFrame createMessageFrame(final Address address) {
    final Blockchain blockchain = mock(Blockchain.class);

    final WorldStateArchive worldStateArchive = createInMemoryWorldStateArchive();
    final WorldUpdater worldStateUpdater = worldStateArchive.getWorldState().updater();
    final BlockHeader blockHeader = new BlockHeaderTestFixture().buildHeader();
    final MessageFrame benchmarkFrame =
        new MessageFrameTestFixture()
            .address(address)
            .worldUpdater(worldStateUpdater)
            .blockHeader(blockHeader)
            .blockchain(blockchain)
            .build();
    worldStateUpdater.getOrCreate(address).setBalance(Wei.of(1));
    worldStateUpdater.commit();

    return benchmarkFrame;
  }

  @Setup
  public void prepare() throws Exception {
    operationBenchmarkHelper = OperationBenchmarkHelper.create();
    CancunGasCalculator gasCalculator = new CancunGasCalculator();
    tstore = new TStoreOperation(gasCalculator);
    tload = new TLoadOperation(gasCalculator);
    frame = createMessageFrame(Address.fromHexString("0x18675309"));
  }

  @TearDown
  public void cleanUp() throws Exception {
    operationBenchmarkHelper.cleanUp();
  }

  @Benchmark
  public Bytes executeOperation() {
    frame.pushStackItem(UInt256.ONE);
    frame.pushStackItem(UInt256.fromHexString("0x01"));
    tstore.execute(frame, null);
    frame.pushStackItem(UInt256.fromHexString("0x01"));
    tload.execute(frame, null);
    return frame.popStackItem();
  }
}
