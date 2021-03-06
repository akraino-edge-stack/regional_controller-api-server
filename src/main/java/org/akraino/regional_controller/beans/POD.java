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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.WebApplicationException;

import org.akraino.regional_controller.db.DB;
import org.akraino.regional_controller.db.DBFactory;
import org.akraino.regional_controller.utils.JSONtoYAML;
import org.akraino.regional_controller.utils.YAMLtoJSON;
import org.akraino.regional_controller.workflow.WorkFlow;
import org.akraino.regional_controller.workflow.WorkFlowFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * A POD is the deployment of a Blueprint on an EdgeSite.  See below for the states that a POD
 * may transition through.
 */
public class POD extends BaseBean {
	public static final String STATE_TAG     = "state";
	public static final String BLUEPRINT_TAG = "blueprint";
	public static final String EDGESITE_TAG  = "edgesite";

	public static Collection<POD> getPods() {
		Map<String, POD> map = pullFromDB();
		return map.values();
	}

	public static POD getPodByUUID(final String uuid) {
		Map<String, POD> map = pullFromDB();
		return map.get(uuid);
	}

	public static POD createPod(JSONObject json) throws WebApplicationException {
		String n = json.optString(NAME_TAG);
		if (n == null || "".equals(n))
			throw new BadRequestException("ARC-1013: Missing name");

		String blueprint = json.optString(BLUEPRINT_TAG);
		if (blueprint == null || "".equals(blueprint))
			throw new BadRequestException("ARC-1009: Missing blueprint UUID");
		if (Blueprint.getBlueprintByUUID(blueprint) == null)
			throw new BadRequestException("ARC-1025: There is no blueprint with UUID="+blueprint);

		String edgesite = json.optString(EDGESITE_TAG);
		if (edgesite == null || "".equals(edgesite))
			throw new BadRequestException("ARC-1010: Missing edgesite UUID");
		if (Edgesite.getEdgesiteByUUID(edgesite) == null)
			throw new BadRequestException("ARC-1026: There is no edgesite with UUID="+edgesite);

		String desc = json.optString(DESCRIPTION_TAG);
		desc = (desc == null) ? "" : desc;
		JSONObject y = json.optJSONObject(YAML_TAG);
		if (y == null) {
			y = new JSONObject();
		}
		UUID u;
		do {
			u = UUID.randomUUID();
		} while (getPodByUUID(u.toString()) != null);
		POD p = new POD(u.toString(), n, desc, blueprint, edgesite, (new JSONtoYAML(y)).toString());
		try {
			DB db = DBFactory.getDB();
			db.createPod(p);
			PODEvent pe = p.createPodEvent("INFO", "Pod created.");
			pe.writeEvent();
			return p;
		} catch (SQLException e1) {
			throw new InternalServerErrorException("ARC-4003: "+e1.getMessage());
		}
	}

	public void updatePod() throws WebApplicationException {
		try {
			DB db = DBFactory.getDB();
			db.updatePod(this);
		} catch (SQLException e1) {
			throw new InternalServerErrorException("ARC-4003: "+e1.getMessage());
		}
	}

	private static Map<String, POD> pullFromDB() {
		Map<String, POD> map = new HashMap<>();
		DB db = DBFactory.getDB();
		List<POD> list = db.getPods();
		for (POD p : list) {
			map.put(p.getUuid(), p);
		}
		return map;
	}

	/**
	 * This enum lists the various states that a POD may transition through from NEW to ZOMBIE.
	 *  State transitions:
	 *
	 *  NEW ----> WORKFLOW <---> ACTIVE
	 *   |          |  ^
	 *   |          |  |
	 *   |          |  +-------> FAILED
	 *   |          V
	 *   +------> DEAD --------> ZOMBIE
	 */
	public enum State {
		/** The POD is new and no workflow has yet been run on it */
		NEW,
		/** A workflow is currently running on the POD */
		WORKFLOW,
		/** No workflow is currently running on the POD; the POD is active */
		ACTIVE,
		/** No workflow is currently running on the POD; the last workflow failed */
		FAILED,
		/** The POD has been successfully DELETE-ed */
		DEAD,
		/** The POD is dead AND the Edgesite used by this POD has been repurposed */
		ZOMBIE
	}

	private String blueprint;			// UUID of the Blueprint
	private String edgesite;			// UUID of the EdgeSite
	private State state;				// the internal state of the POD
	private String yaml;				// extra JSON describing the POD

	public POD(String uuid, String name, String description, String bpid, String esid, String yaml) {
		this(uuid, name, description, State.NEW, bpid, esid, yaml);
	}

	public POD(String uuid, String name, String description, String state, String bpid, String esid, String yaml) {
		this(uuid, name, description, State.valueOf(state), bpid, esid, yaml);
	}

	public POD(String uuid, String name, String description, State state, String bpid, String esid, String yaml) {
		super(uuid, name, description);
		this.blueprint = bpid;
		this.edgesite  = esid;
		this.state = state;
		this.yaml = yaml;
	}

	public String getBlueprint() {
		return blueprint;
	}

	public void setBlueprint(String blueprint) {
		this.blueprint = blueprint;
	}

	public String getEdgesite() {
		return edgesite;
	}

	public State getState() {
		return state;
	}

	public void setState(State newstate) {
		if (this.state != newstate) {
			boolean ok = false;
			Logger logger = LogManager.getLogger();
			switch (this.state) {
			case NEW:
				ok = (newstate == State.WORKFLOW) || (newstate == State.DEAD);
				break;

			case WORKFLOW:
				ok = (newstate == State.ACTIVE) || (newstate == State.FAILED) || (newstate == State.DEAD);
				break;

			case ACTIVE:
			case FAILED:
				ok = (newstate == State.WORKFLOW) || (newstate == State.DEAD);
				break;

			case DEAD:
				ok = (newstate == State.ZOMBIE);
				break;

			case ZOMBIE:
				ok = false;
				break;

			default:
				logger.warn("Internal error, setState() with unknown state "+newstate);
				return;
			}
			if (ok) {
				if (this.state == State.WORKFLOW) {
					// Mark end time of pod_workflow - there should be only one that is running for this UUID
					DB db = DBFactory.getDB();
					Timestamp ts = new Timestamp(System.currentTimeMillis());
					for (PODWorkflow pw : db.getPODWorkflows(getUuid())) {
						if (pw.getEndtime() == null) {
							pw.setEndtime(ts);
							try {
								db.updatePodWorkflow(pw);
							} catch (SQLException e) {
								logger.warn("Internal error, while updating PODWorkflow: "+e);
							}
						}
					}
				}
				this.state = newstate;
				if (newstate == State.ZOMBIE) {
					// The Edgesite for this POD is being reused, so point to a non-existent ES
					this.edgesite = "00000000-0000-0000-0000-000000000000";	// TODO make sure this works
				}
				try {
					DB db = DBFactory.getDB();
					db.updatePod(this);
					logger.info("Set State of POD "+this.getUuid()+" to "+state.toString());
				} catch (SQLException e) {
					logger.warn("Internal error, while updating POD: "+e);
				}
			} else {
				logger.warn("Bad POD state transition requested: "+state+" -> "+newstate);
			}
		}
	}

	public String getYaml() {
		return yaml;
	}

	public void setYaml(String j) {
		this.yaml = j;
	}

	public Blueprint getBlueprintObject() {
		return Blueprint.getBlueprintByUUID(blueprint);
	}

	public Edgesite getEdgesiteObject() {
		return Edgesite.getEdgesiteByUUID(edgesite);
	}

	public boolean isWorkflowRunning() {
		return state == State.WORKFLOW;
	}

	public boolean isAlive() {
		return (state != State.DEAD) && (state != State.ZOMBIE);
	}

	public PODEvent createPodEvent(String level, String msg) {
		return new PODEvent(getUuid(), level, msg);
	}

	public List<PODEvent> getPodEvents() {
		DB db = DBFactory.getDB();
		return db.getPODEvents(this.getUuid());
	}

	public synchronized boolean startWorkFlow(PODWorkflow pwf) {
		if (isAlive() && ! isWorkflowRunning()) {
			WorkFlow wf = WorkFlowFactory.getWorkFlow();
			if (wf.initialize(this, pwf)) {
				wf.start();
				setState(State.WORKFLOW);
				return true;
			}
		}
		return false;
	}

	@Override
	public JSONObject toJSON() {
		JSONObject jo = super.toJSON();
		jo.put(BLUEPRINT_TAG, blueprint);
		jo.put(EDGESITE_TAG, edgesite);
		jo.put(STATE_TAG, state.toString());
		if (yaml != null && !"".equals(yaml)) {
			jo.put(YAML_TAG, new YAMLtoJSON(yaml).toJSON());
		}
		Blueprint bp = getBlueprintObject();
		jo.put("workflows", new JSONArray(bp.getWorkFlowNames()));
		return jo;
	}
}
