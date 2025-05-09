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
package org.idnecology.idn.ethereum.trie;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.idnecology.idn.ethereum.trie.patricia.BranchNode;
import org.idnecology.idn.ethereum.trie.patricia.ExtensionNode;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AllNodesVisitorTest {

  @Mock private StoredNode<String> storedNode;
  @Mock private ExtensionNode<String> extensionNode;
  @Mock private BranchNode<String> branchNode;

  @Test
  public void visitExtension() {
    when(extensionNode.getChild()).thenReturn(storedNode);
    new AllNodesVisitor<String>(x -> {}).visit(extensionNode);
    verify(storedNode).unload();
  }

  @Test
  public void visitBranch() {
    when(branchNode.getChildren()).thenReturn(Collections.singletonList(storedNode));
    new AllNodesVisitor<String>(x -> {}).visit(branchNode);
    verify(storedNode).unload();
  }
}
