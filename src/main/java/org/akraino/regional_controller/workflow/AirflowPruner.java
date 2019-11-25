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

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The class, which runs as a singleton, will start a periodic task to clean up Airflow DAGs from the
 * Apache Airflow Engine.  It is started when the first Airflow class instance is created.  DAGs older
 * than workflow.airflow.prunetime are removed.
 */
public class AirflowPruner extends TimerTask {
	public static final long ONE_HOUR_IN_MS      = 60L * 60L * 1000L;	// 1 hour in ms
	public static final String DEFAULT_PRUNETIME = "168";				// default - 1 week (168 hours)

	private final Logger logger;
	private final Timer  rolex;
	private final String airflow_url;
	private final String workspace_directory;
	private final String dags_directory;
	private final long   period;

	private static AirflowPruner singleton = null;

	public static synchronized AirflowPruner getPruner(Properties props, Logger logger) {
		if (singleton == null) {
			singleton = new AirflowPruner(props, logger);
		}
		return singleton;
	}

	private AirflowPruner(Properties props) {
		this(props, LogManager.getLogger());
	}

	private AirflowPruner(Properties props, Logger logger) {
		this.logger              = logger;
		this.rolex               = new Timer("AirflowPruner Timer");
		this.airflow_url         = props.getProperty("workflow.airflow.url",  Airflow.DEFAULT_URL);
		this.workspace_directory = props.getProperty("workflow.airflow.root", Airflow.DEFAULT_ROOT);
		this.dags_directory      = props.getProperty("workflow.airflow.dags", Airflow.DEFAULT_DAGS);
		String s                 = props.getProperty("workflow.airflow.prunetime", DEFAULT_PRUNETIME);
		this.period              = Long.parseLong(s) * ONE_HOUR_IN_MS;		// convert to milliseconds
		this.rolex.scheduleAtFixedRate(this, 0L, ONE_HOUR_IN_MS);			// Run task once per hour
		this.logger.info("Started AirflowPruner.");
		this.logger.info("AirflowPruner: prunetime is "+(this.period / ONE_HOUR_IN_MS)+" hours.");
	}
	public void run() {
		logger.info("Running periodic pruning task.");
		File dagdir = new File(dags_directory);
		File[] files = dagdir.listFiles();
		if (files.length == 0) {
			logger.info("  ..nothing to prune");
		} else {
			long cutoff = System.currentTimeMillis() - period;
			String cookie = null;		// session cookie
			for (File dag : files) {
				if (dag.getName().endsWith(".py") && dag.lastModified() < cutoff) {
					// Prune this workflow -- first remove the DAG file
					logger.info("  ..removing "+dag.getAbsolutePath());
					if (!dag.delete()) {
						logger.warn("  ..could not remove "+dag.getAbsolutePath());
					}

					// Next, remove the workspace directory
 					String dag_name = dag.getName();
					dag_name = dag_name.replaceAll(".py$", "");
					String wdir = String.format("%s/%s", workspace_directory, dag_name);
					logger.info("  ..removing "+wdir);
					try {
						FileUtils.deleteDirectory(new File(wdir));
					} catch (IOException e) {
						logger.warn(e);
					}

					// Next, notify Airflow
					if (cookie == null) {
						logger.info("  ..fetch session cookie");
						cookie = fetchUrl(airflow_url+"/admin/", null);
						logger.info("  cookie is "+cookie);
					}
					fetchUrl(airflow_url+"/admin/airflow/delete?dag_id="+dag_name, cookie);
				}
			}
		}
	}
	private String fetchUrl(String url, String cookie) {
		logger.info("  ..curl "+url);
		WebTarget target = ClientBuilder.newClient().target(url);
		Builder b = target.request();
		if (cookie != null) {
			b = b.cookie(new Cookie("session", cookie));
		}
		Response r = b.buildGet().invoke();
		MultivaluedMap<java.lang.String,java.lang.Object> m = r.getMetadata();
		String setcookie = m.getFirst("Set-Cookie").toString();
		if (setcookie != null) {
			//  Set-Cookie => session=eyJjc3JmX3Rva2VuIjoiNTBhZDIwYzQ4ZTJiMTIwYmI4MzZjMGY5MDhlNWQ0NjdhOGQzYjcwNiJ9.XVMUWQ.eenCZYH1MPPzYOaSIIxsD8lfurs; HttpOnly; Path=/
			setcookie = setcookie
				.replaceAll("session=", "")
				.replaceAll(";.*$", "");
		}
		return setcookie;
	}

	public static void main(String[] args) throws InterruptedException {
		Properties pr = new Properties();
		pr.setProperty("workflow.airflow.prunetime", "10");
		pr.setProperty("workflow.airflow.root",      "/data" + Airflow.DEFAULT_ROOT);
		pr.setProperty("workflow.airflow.dags",      "/data" + Airflow.DEFAULT_DAGS);
		pr.setProperty("workflow.airflow.url",       "http://mtmac2.research.att.com:8080");
		new AirflowPruner(pr);
		Thread.sleep(5000L);
	}
}
