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
import org.akraino.regional_controller.utils.YAMLtoJSON;
import org.json.JSONObject;

public class Hardware extends BaseBean {
	public static Collection<Hardware> getHardware() {
		Map<String, Hardware> map = pullFromDB();
		return map.values();
	}

	public static Hardware getHardwareByUUID(final String uuid) {
		Map<String, Hardware> map = pullFromDB();
		return map.get(uuid);
	}

	public static String createHardware(JSONObject json) throws WebApplicationException {
		String n = json.optString(NAME_TAG);
		if (n == null || "".equals(n))
			throw new BadRequestException("Missing name");
		String d = json.optString("description");
		String uuid = json.optString("uuid");
		if (uuid == null || "".equals(uuid)) {
			// Find a new, unused UUID
			UUID u;
			do {
				u = UUID.randomUUID();
			} while (getHardwareByUUID(u.toString()) != null);
			uuid = u.toString();
		} else {
			// Use the UUID provided
			if (getHardwareByUUID(uuid) != null) {
				throw new BadRequestException("UUID "+uuid+" is already in use.");
			}
		}
		Hardware h2 = new Hardware(uuid, n, d, json.toString());
		try {
			DB db = DBFactory.getDB();
			db.createHardware(h2);
			return uuid;
		} catch (SQLException e1) {
			throw new InternalServerErrorException(e1.getMessage());
		}
	}

	private static Map<String, Hardware> pullFromDB() {
		Map<String, Hardware> map = new HashMap<>();
		DB db = DBFactory.getDB();
		List<Hardware> list = db.getHardware();
		for (Hardware h : list) {
			map.put(h.getUuid(), h);
		}
		return map;
	}

	private String yaml;			// extra JSON describing the Node

	public Hardware(String uuid, String name, String description, String yaml) {
		super(uuid, name, description);
		this.yaml = yaml;
	}

	public String getYaml() {
		return yaml;
	}

	public void setYaml(String j) {
		this.yaml = j;
	}

	@Override
	public JSONObject toJSON() {
		JSONObject jo = super.toJSON();
		if (yaml != null && !"".equals(yaml)) {
			jo.put("yaml", new YAMLtoJSON(yaml).toJSON());
		}
		return jo;
	}
}
