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

package org.akraino.regional_controller.beans;

import java.sql.SQLException;
import java.sql.Timestamp;

import javax.ws.rs.WebApplicationException;

import org.akraino.regional_controller.db.DB;
import org.akraino.regional_controller.db.DBFactory;
import org.akraino.regional_controller.utils.JSONtoYAML;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

/*
 * The PodWorkflow object encapsulates all the information from the POD about a specific workflow instance on behalf
 * of that POD, e.g.
 * - POD UUID
 * - workflow name
 * - any YAML provided by the user in the POST/PUT
 * - start/end times of the workflow
 */
public class PODWorkflow extends BaseBean {
	public synchronized static PODWorkflow createPodWorkflow(POD p, String wfname, JSONObject json) throws WebApplicationException {
		Logger logger = LogManager.getLogger();
		try {
			// Find next index for this workflow
			DB db = DBFactory.getDB();
			int next_ix = 0;
			for (PODWorkflow pw : db.getPODWorkflows(p.getUuid())) {
				if (pw.getName().equals(wfname))
					next_ix++;
			}

			PODWorkflow pw = new PODWorkflow(p.getUuid(), wfname, next_ix, new JSONtoYAML(json).toString());
			db.createPodWorkflow(pw);
			return pw;
		} catch (SQLException e) {
			logger.warn(e);
			return null;
		}
	}

	private final int index;
	private final String yaml;			// JSON/YAML submitted by the user
	private final Timestamp starttime;
	private       Timestamp endtime;

	public PODWorkflow(String uuid, String name, int index, String yaml) {
		this(uuid, name, index, new Timestamp(System.currentTimeMillis()), yaml);
	}

	public PODWorkflow(String uuid, String name, int index, Timestamp stime, String yaml) {
		super(uuid, name, "");
		this.index = index;
		this.yaml = yaml;
		this.starttime = stime;
		this.endtime = null;
	}

	@Override
	public void setName(String name) {
		throw new IllegalArgumentException("ARC-4011: Cannot change the name of a PODWorkflow.");
	}

	@Override
	public void setDescription(String description) {
		throw new IllegalArgumentException("ARC-4002: Cannot change the description of a PODWorkflow.");
	}

	public int getIndex() {
		return index;
	}

	public String getYaml() {
		return yaml;
	}

	public Timestamp getStarttime() {
		return starttime;
	}

	public Timestamp getEndtime() {
		return endtime;
	}

	public void setEndtime(Timestamp endtime) {
		this.endtime = endtime;
	}

	public boolean isRunning() {
		return endtime == null;
	}

	public JSONObject toJSON() {
		JSONObject jo = super.toJSON();
		jo.remove(DESCRIPTION_TAG);
		jo.put("index", index);
		jo.put("yaml", yaml);
		jo.put("starttime", starttime.toString());
		if (endtime != null) {
			jo.put("endtime", endtime.toString());
		}
		return jo;
	}
}
