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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.WebApplicationException;

import org.akraino.regional_controller.db.DB;
import org.akraino.regional_controller.db.DBFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

public class Edgesite extends BaseBean {
	private static final Logger logger = LogManager.getLogger();
	public static final String NODES_TAG   = "nodes";
	public static final String REGIONS_TAG = "regions";

	public static String createEdgesite(JSONObject json) throws WebApplicationException {
		String n = json.optString(NAME_TAG);
		if (n == null || "".equals(n)) {
			logger.warn("Missing name");
			throw new BadRequestException("Missing name");
		}
		String d = json.optString(DESCRIPTION_TAG);
		if (d == null) {
			d = "";
		}

		// Make sure all of the Nodes are valid and not in use
		JSONArray nodes = json.optJSONArray(NODES_TAG);
		if (nodes == null || nodes.length() == 0) {
			logger.warn("No nodes listed in JSON");
			throw new BadRequestException("No nodes listed in JSON");
		}
		Set<String> nset = new TreeSet<>();
		for (int i = 0; i < nodes.length(); i++) {
			String nodeid = nodes.getString(i);
			Node node = Node.getNodeByUUID(nodeid);
			if (node == null) {
				logger.warn("Invalid Node UUID="+nodeid);
				throw new BadRequestException("Invalid Node uuid "+nodeid);
			}
			Edgesite es = node.getEdgesite();
			if (es != null) {
				logger.warn("Node is already a member of EdgeSite "+es.getUuid());
				throw new BadRequestException("Node is already a member of EdgeSite "+es.getUuid());
			}
			nset.add(nodeid);
		}

		// Make sure all regions are valid
		JSONArray regions = json.optJSONArray(REGIONS_TAG);
		if (regions == null || regions.length() == 0) {
			logger.warn("Missing regions");
			throw new BadRequestException("Missing regions");
		}
		Set<String> rset = new TreeSet<>();
		for (int i = 0; i < regions.length(); i++) {
			String regionid = regions.getString(i);
			Region reg = Region.getRegionByUUID(regionid);
			if (reg == null) {
				logger.warn("Invalid Region uuid "+regionid);
				throw new BadRequestException("Invalid Region uuid "+regionid);
			}
			rset.add(regionid);
		}

		String uuid = json.optString(UUID_TAG);
		if (uuid == null || "".equals(uuid)) {
			UUID u;
			do {
				u = UUID.randomUUID();
			} while (getEdgesiteByUUID(u.toString()) != null);
			uuid = u.toString();
		} else {
			// Use the UUID provided
			if (getEdgesiteByUUID(uuid) != null) {
				throw new BadRequestException("UUID "+uuid+" is already in use.");
			}
		}
		Edgesite e = new Edgesite(uuid, n, d);
		e.setNodes(nset);
		e.setRegions(rset);
		try {
			DB db = DBFactory.getDB();
			db.createEdgesite(e);
			return uuid;
		} catch (SQLException e1) {
			throw new InternalServerErrorException(e1.getMessage());
		}
	}

	public static Collection<Edgesite> getEdgesites() {
		Map<String, Edgesite> map = pullFromDB();
		return map.values();
	}

	public static Collection<Edgesite> getEdgesitesByRegion(String region_uuid) {
		if (region_uuid == null || "".equals(region_uuid)) {
			// Return all regions
			return getEdgesites();
		} else {
			Collection<Edgesite> c = new ArrayList<>();
			for (Edgesite e : getEdgesites()) {
				if (e.getRegions().contains(region_uuid)) {
					c.add(e);
				}
			}
			return c;
		}
	}

	public static Edgesite getEdgesiteByUUID(final String uuid) {
		Map<String, Edgesite> map = pullFromDB();
		return map.get(uuid);
	}

	public void updateEdgesite() throws WebApplicationException {
		try {
			DB db = DBFactory.getDB();
			db.updateEdgesite(this);
		} catch (SQLException e1) {
			throw new InternalServerErrorException(e1.getMessage());
		}
	}

	private static Map<String, Edgesite> pullFromDB() {
		Map<String, Edgesite> map = new HashMap<>();
		DB db = DBFactory.getDB();
		List<Edgesite> list = db.getEdgesites();
		for (Edgesite e : list) {
			map.put(e.getUuid(), e);
		}
		return map;
	}

	private Set<String> regions;
	private Set<String> nodes;

	public Edgesite(String uuid, String name, String description) {
		super(uuid, name, description);
		this.regions = new TreeSet<>();
		this.nodes   = new TreeSet<>();
	}

	public Edgesite(String uuid, String name, String description, Collection<String> regions) {
		this(uuid, name, description);
		this.regions.addAll(regions);
	}

	public Edgesite(String uuid, String name, String description, Collection<String> regions, Collection<String> nodes) {
		this(uuid, name, description, regions);
		this.nodes.addAll(nodes);
	}

	public Set<String> getRegions() {
		return regions;
	}

	public void setRegions(Set<String> regions) {
		this.regions = regions;
	}

	public Set<String> getNodes() {
		return nodes;
	}

	public void setNodes(Set<String> nodes) {
		this.nodes = nodes;
	}

	/**
	 * Return the POD (if any) that is using this Edgesite.
	 * @return the POD, or null
	 */
	public POD getPOD() {
		String uuid = getUuid();
		for (POD p : POD.getPods()) {
			if (p.getEdgesite().equals(uuid)) {
				return p;
			}
		}
		return null;
	}

	@Override
	public JSONObject toJSON() {
		JSONObject jo = super.toJSON();
		JSONArray rarray = new JSONArray();
		for (String s : regions) {
			rarray.put(s);
		}
		jo.put(REGIONS_TAG,  rarray);
		JSONArray narray = new JSONArray();
		for (String s : nodes) {
			narray.put(s);
		}
		jo.put(NODES_TAG, narray);
		return jo;
	}
}
