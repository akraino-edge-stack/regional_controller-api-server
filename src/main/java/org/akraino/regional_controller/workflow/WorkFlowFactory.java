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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.akraino.regional_controller.db.DBFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WorkFlowFactory {
	private static final String PROPERTIES_FILE = "api.properties";
	private static final Logger logger = LogManager.getLogger();

	/**
	 * Build a Workflow object.  For now, there is only the "Airflow" workflow engine in the ARC,
	 * but in future there may be others.
	 * @return a new instance of a Workflow (an interface to the Workflow Engine)
	 */
	public static Airflow getWorkFlow() {
		Properties props = new Properties();
		InputStream is = null;
		try {
			is = DBFactory.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE);
			props.load(is);
			return new Airflow(props);
		} catch (IOException e) {
			logger.error(e);
			return null;
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					logger.error(e);
				}
			}
		}
	}

	private WorkFlowFactory() {}
}
