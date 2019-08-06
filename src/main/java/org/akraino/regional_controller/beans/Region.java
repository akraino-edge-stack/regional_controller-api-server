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
import java.util.UUID;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.WebApplicationException;

import org.akraino.regional_controller.db.DB;
import org.akraino.regional_controller.db.DBFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Region extends BaseBean {
	public static final String UNIVERSAL_REGION = "00000000-0000-0000-0000-000000000000";
	public static final String PARENT_TAG = "parent";

	public static String createRegion(JSONObject json) {
		try {
			String n = json.getString(NAME_TAG);
			if (n == null || "".equals(n))
				return null;
			String d = json.optString(DESCRIPTION_TAG);
			if (d == null)
				d = "";
			String p = json.optString(PARENT_TAG);
			if (p == null || "".equals(p))
				p = UNIVERSAL_REGION;
			String uuid = json.optString(UUID_TAG);
			if (uuid == null || "".equals(uuid)) {
				// Find a new, unused UUID
				UUID u;
				do {
					u = UUID.randomUUID();
				} while (getRegionByUUID(u.toString()) != null);
				uuid = u.toString();
			} else {
				// Use the UUID provided
				if (getRegionByUUID(uuid) != null) {
					throw new BadRequestException("ARC-1027: UUID "+uuid+" is already in use.");
				}
			}
			Region r = new Region(uuid, n, d, p);
			DB db = DBFactory.getDB();
			db.createRegion(r);
			return uuid;
		} catch (JSONException e) {
			return null;
		} catch (SQLException e1) {
			return null;
		}
	}

	public static Collection<Region> getRegions() {
		Map<String, Region> map = pullFromDB();
		return map.values();
	}

	public static Region getRegionByUUID(final String uuid) {
		Map<String, Region> map = pullFromDB();
		return map.get(uuid);
	}

	public void updateRegion() throws WebApplicationException {
		try {
			DB db = DBFactory.getDB();
			db.updateRegion(this);
		} catch (SQLException e1) {
			throw new InternalServerErrorException("ARC-4003: "+e1.getMessage());
		}
	}

	private static Map<String, Region> pullFromDB() {
		Map<String, Region> map = new HashMap<>();
		DB db = DBFactory.getDB();
		List<Region> list = db.getRegions();
		for (Region r : list) {
			map.put(r.getUuid(), r);
		}
		return map;
	}

	private String parent;

	public Region(String uuid, String name, String description) {
		super(uuid, name, description);
		this.parent = UNIVERSAL_REGION;
	}

	public Region(String uuid, String name, String description, String parent) {
		super(uuid, name, description);
		this.parent = (parent == null) ? UNIVERSAL_REGION : parent;
	}

	public String getParent() {
		return parent;
	}

	public void setParent(String parent) {
		this.parent = parent;
	}

	/**
	 * Return the list of Regions that are using this Region as their parent.
	 * @return the list (which may be empty).
	 */
	public List<Region> getChildRegions() {
		List<Region> list = new ArrayList<>();
		String uuid = getUuid();
		for (Region r : Region.getRegions()) {
			if (r.getParent().equals(uuid)) {
				list.add(r);
			}
		}
		return list;
	}

	/**
	 * Return the list of Edgesites that are using this Region as one of their member Regions.
	 * @return the list (which may be empty).
	 */
	public List<Edgesite> getEdgesites() {
		List<Edgesite> list = new ArrayList<>();
		String uuid = getUuid();
		for (Edgesite e : Edgesite.getEdgesites()) {
			for (String r : e.getRegions()) {
				if (r.equals(uuid)) {
					list.add(e);
				}
			}
		}
		return list;
	}

	@Override
	public JSONObject toJSON() {
		JSONObject jo = super.toJSON();
		if (parent != null) {
			jo.put("parent",  parent);
		}
		List<Region> children = getChildRegions();
		if (children.size() > 0) {
			JSONArray ja = new JSONArray();
			for (Region ch : children) {
				ja.put(ch.getUuid());
			}
			jo.put("children",  ja);
		}
		return jo;
	}
}
