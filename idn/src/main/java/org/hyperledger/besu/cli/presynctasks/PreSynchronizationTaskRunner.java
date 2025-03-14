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
package org.idnecology.idn.cli.presynctasks;

import org.idnecology.idn.controller.IdnController;

import java.util.ArrayList;
import java.util.List;

/** The Pre synchronization task runner. */
public class PreSynchronizationTaskRunner {

  private final List<PreSynchronizationTask> tasks = new ArrayList<>();

  /** Default Constructor. */
  public PreSynchronizationTaskRunner() {}

  /**
   * Add task.
   *
   * @param task the task
   */
  public void addTask(final PreSynchronizationTask task) {
    tasks.add(task);
  }

  /**
   * Run tasks.
   *
   * @param idnController the idn controller
   */
  public void runTasks(final IdnController idnController) {
    tasks.forEach(t -> t.run(idnController));
  }
}
