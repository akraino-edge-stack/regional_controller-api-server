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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.akraino.regional_controller.beans.Blueprint;
import org.akraino.regional_controller.beans.Edgesite;
import org.akraino.regional_controller.beans.POD;
import org.akraino.regional_controller.utils.JSONtoYAML;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

@SuppressWarnings("unused")
public class Airflow implements WorkFlow {
	public static final String TEMPLATE_PYTHON = "template_python.py";
	public static final String TEMPLATE_SHELL  = "template_shell.py";
	public static final String DEFAULT_URL     = "http://arc-airflow-webserver:8080";
	public static final String DEFAULT_ROOT    = "/workflow";
	public static final String DEFAULT_DAGS    = "/dags";

	private final Properties props;
	private final Logger     logger;
	private final String     airflow_url;
	private final String     workspace_directory;
	private final String     dags_directory;
	private String           dag_name;
	private boolean          isrunning;

	public Airflow(Properties api_props) {
		this.props  = api_props;
		this.logger = LogManager.getLogger();
		this.airflow_url         = props.getProperty("workflow.airflow.url",  DEFAULT_URL);
		this.workspace_directory = props.getProperty("workflow.airflow.root", DEFAULT_ROOT);
		this.dags_directory      = props.getProperty("workflow.airflow.dags", DEFAULT_DAGS);
		this.dag_name = null;
		this.isrunning = false;
	}

	@Override
	public boolean initialize(POD pod, String phase) {
		// 1. Start the Workflow in process
		final String uuid = pod.getUuid();
		final Blueprint bp = pod.getBlueprintObject();
		JSONObject wf_json = bp.getObjectStanza("workflow/"+phase);
		pod.createPodEvent("INFO", "Starting workflow: "+phase).writeEvent();

		// 2. Build the workspace for the workflow
		final String wfname = String.format("%s-%s", phase, uuid);
		final String wfdir  = String.format("%s/%s", workspace_directory, wfname);
		if (!new File(wfdir).mkdirs()) {
			logger.warn("Could not create the workflow directory $DROOT"+wfdir);
			return false;
		}
		logger.info("Workflow workspace is "+wfdir);
		pod.createPodEvent("INFO", "Workflow directory created: $DROOT"+wfdir).writeEvent();

		// 3. Grab workflow from the URL
		String url = wf_json.optString("url");
		if (url == null || "".contentEquals(url)) {
			logger.warn("No "+phase+" workflow URL in Blueprint "+bp.getUuid());
			pod.createPodEvent("WARN", "No "+phase+" workflow URL in Blueprint "+bp.getUuid()).writeEvent();
			return false;
		}
		if (!(url.endsWith(".py") || url.endsWith(".sh"))) {
			logger.warn("The "+phase+" workflow URL in Blueprint "+bp.getUuid()+ " must end in .py or .sh");
			pod.createPodEvent("WARN", "The "+phase+" workflow URL in Blueprint "+bp.getUuid()+ " must end in .py or .sh").writeEvent();
			return false;
		}
		if (!this.fetchFile(url, wfdir)) {
			logger.warn("Could not fetch the workflow file "+url);
			pod.createPodEvent("WARN", "Could not fetch the workflow file "+url).writeEvent();
			return false;
		}
		pod.createPodEvent("INFO", "Workflow fetched: "+url).writeEvent();

		// 4. Gather components specified in the Blueprint
		JSONArray ja = wf_json.optJSONArray("components");
		if (ja != null) {
			boolean success = true;
			for (int i = 0; i < ja.length(); i++) {
				String asset = ja.optString(i);
				if (asset == null || !this.fetchFile(asset, wfdir)) {
					logger.warn("Could not fetch the component file "+asset);
					pod.createPodEvent("WARN", "Workflow component could NOT be fetched: "+asset).writeEvent();
					success = false;
				}
				pod.createPodEvent("INFO", "Workflow component fetched: "+asset).writeEvent();
			}
			if (!success) {
				return false;
			}
		}

		// 5. Create top-level workflow files
		if (!this.createWorkFlow(wfdir, wfname, pod, bp, pod.getEdgesiteObject(), phase, url)) {
			logger.warn("Could not create the workflow template file.");
			return false;
		}
		pod.createPodEvent("INFO", "Workflow template created.").writeEvent();

		return true;
	}

	@Override
	public synchronized void start() {
		if (dag_name != null) {
			// Unpause the DAG
			runAirFlowCommand("unpause");

			// Trigger the DAG
			runAirFlowCommand("trigger_dag");

			isrunning = true;
		}
	}

	@Override
	public void cancel() {
		if (isrunning) {
			// TODO - provide workflow URL to use that may be PUT/DELETE-ed
		}
	}

	@Override
	public boolean isFinished() {
		return false;	//TODO - determine if WF is running
	}

	private boolean fetchFile(String url, String dir) {
		Logger logger = LogManager.getLogger();
		try {
			int ix = url.lastIndexOf('/');
			String fname = url.substring(ix+1);
			Path path = Paths.get(dir, fname);
			URL u = new URL(url);
			try (InputStream in = u.openStream()) {
				Files.copy(in, path);
				return true;
			} catch (IOException e) {
				logger.warn("IOException: "+e);
			}
		} catch (MalformedURLException e) {
			logger.warn("MalformedURLException: "+e);
		}
		return false;
	}

	private boolean createWorkFlow(String wfdir, String wfname, POD pod, Blueprint bp, Edgesite es, String phase, String url) {
		int ix = url.lastIndexOf('/');
		String wfscript = url.substring(ix+1);
		boolean ispython = url.endsWith(".py");
		if (ispython) {
			wfscript = wfscript.substring(0, wfscript.length()-3);
		}

		// Create the POD.py file
		StringBuilder sb = new StringBuilder();
		sb.append("POD = '").append(pod.getUuid()).append("'\n\n");
		sb.append("BLUEPRINT = \"\"\"\n").append(new JSONtoYAML(bp.toJSON()).toString()).append("\"\"\"\n\n");
		sb.append("EDGESITE = \"\"\"\n") .append(new JSONtoYAML(es.toJSON()).toString()).append("\"\"\"\n\n");
		if (!writeToFile(sb.toString(), Paths.get(wfdir, "POD.py")))
			return false;

		// Create the POD.sh file
		sb.setLength(0);
		sb.append("export POD='").append(pod.getUuid()).append("'\n\n");
		sb.append("export BLUEPRINT='\n").append(new JSONtoYAML(bp.toJSON()).toString()).append("'\n\n");
		sb.append("export EDGESITE='\n") .append(new JSONtoYAML(es.toJSON()).toString()).append("'\n\n");
		if (!writeToFile(sb.toString(), Paths.get(wfdir, "POD.sh")))
			return false;

		// Create the top level workflow script (Python)
		String t = getTemplate(ispython);
		t = t.replace("##UUID##", pod.getUuid());
		t = t.replace("##PHASE##", phase);
		t = t.replace("##WFNAME##", wfscript);
		Path wfpath = Paths.get(dags_directory, wfname + ".py");
		if (!writeToFile(t, wfpath))
			return false;

		dag_name = wfname;
		return true;
	}

	private boolean writeToFile(String t, Path path) {
		try {
			Files.copy(new ByteArrayInputStream(t.getBytes()), path);
			return true;
		} catch (IOException e) {
			logger.warn("IOException: writing to "+path);
			logger.warn(e);
			return false;
		}
	}

	private String getTemplate(boolean ispython) {
		String template = ispython ? TEMPLATE_PYTHON : TEMPLATE_SHELL;
		try (InputStream in = getClass().getClassLoader().getResourceAsStream(template)) {
			try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
				byte[] buf = new byte[1024];
		        int len;
		        while ((len = in.read(buf)) > 0){
		            out.write(buf, 0, len);
		        }
				return out.toString();
			}
		} catch (IOException e) {
			return null;
		}
	}

	private void runAirFlowCommand(String command) {
		switch (command) {
		case "trigger_dag":
//			{
//				String uri = String.format("%s/api/experimental/dags/%s/dag_runs", airflow_url, dag_name);
//				WebTarget target = ClientBuilder.newClient().target(uri);
//				Entity<?> entity = Entity.entity("{\"conf\": {\"key\": \"value\"}}", MediaType.APPLICATION_JSON);
//				Builder b = target
//					.request()
//					.header("Cache-Control", "no-cache");
//				Response r = b.buildPost(entity).invoke();
//				if (r.getStatus() != 200) {
//					logger.warn("runAirFlowCommand(trigger_dag) on DAG "+dag_name+" returned "+r.getStatus());
//				}
//			}
			break;

		case "unpause":
//			{
//				String uri = String.format("%s/api/experimental/dags/%s/paused/false", airflow_url, dag_name);
//				WebTarget target = ClientBuilder.newClient().target(uri);
//				Builder b = target.request();
//				Response r = b.buildGet().invoke();
//				if (r.getStatus() != 200) {
//					logger.warn("runAirFlowCommand(unpause) on DAG "+dag_name+" returned "+r.getStatus());
//				}
//			}
			break;

		default:
			logger.warn("Internal error, unknown runAirFlowCommand command "+command);
		}
	}
}
