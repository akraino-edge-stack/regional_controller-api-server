/*
 * Copyright (c) 2019 AT&T Intellectual Property. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.akraino.regional_controller.workflow;

import java.io.InputStream;

import org.akraino.regional_controller.beans.POD;
import org.akraino.regional_controller.beans.PODWorkflow;

/**
 * The interface to the Workflow engine.
 */
public interface WorkFlow {
	public boolean initialize(POD pod, PODWorkflow pwf);
	public void start();
	public void cancel();
	public boolean isRunning();
	public InputStream getLogfiles(POD pod, PODWorkflow pwf);
}
