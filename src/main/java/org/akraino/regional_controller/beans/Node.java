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
import org.akraino.regional_controller.utils.JSONtoYAML;
import org.akraino.regional_controller.utils.YAMLtoJSON;
import org.json.JSONObject;

public class Node extends BaseBean {
	public static Collection<Node> getNodes() {
		Map<String, Node> map = pullFromDB();
		return map.values();
	}

	public static Node getNodeByUUID(final String uuid) {
		Map<String, Node> map = pullFromDB();
		return map.get(uuid);
	}

	public static String createNode(JSONObject json) throws WebApplicationException {
		String n = json.getString(NAME_TAG);
		if (n == null || "".equals(n))
			throw new BadRequestException("Missing name");
		String d = json.optString("description");
		String h = json.optString("hardware");
		if (Hardware.getHardwareByUUID(h) == null) {
			throw new BadRequestException("Invalid Hardware uuid "+h);
		}
		JSONObject y = json.optJSONObject(YAML_TAG);
		if (y == null) {
			y = new JSONObject();
		}

		String uuid = json.optString("uuid");
		if (uuid == null || "".equals(uuid)) {
			// Find a new, unused UUID
			UUID u;
			do {
				u = UUID.randomUUID();
			} while (getNodeByUUID(u.toString()) != null);
			uuid = u.toString();
		} else {
			// Use the UUID provided
			if (getNodeByUUID(uuid) != null) {
				throw new BadRequestException("UUID "+uuid+" is already in use.");
			}
		}
		Node n2 = new Node(uuid, n, d, h, (new JSONtoYAML(y)).toString());
		try {
			DB db = DBFactory.getDB();
			db.createNode(n2);
			return uuid;
		} catch (SQLException e1) {
			throw new InternalServerErrorException(e1.getMessage());
		}
	}

	private static Map<String, Node> pullFromDB() {
		Map<String, Node> map = new HashMap<>();
		DB db = DBFactory.getDB();
		List<Node> list = db.getNodes();
		for (Node n : list) {
			map.put(n.getUuid(), n);
		}
		return map;
	}

	private String hardware;
	private String yaml;			// extra JSON describing the Node

	public Node(String uuid, String name, String description, String hardware, String yaml) {
		super(uuid, name, description);
		this.hardware = hardware;
		this.yaml = yaml;
	}

	public String getHardware() {
		return hardware;
	}

	public void setHardware(String hardware) {
		this.hardware = hardware;
	}

	public String getYaml() {
		return yaml;
	}

	public void setYaml(String j) {
		this.yaml = j;
	}

	public Edgesite getEdgesite() {
		String myuuid = getUuid();
		for (Edgesite es : Edgesite.getEdgesites()) {
			for (String uuid : es.getNodes()) {
				if (uuid.equalsIgnoreCase(myuuid))
					return es;
			}
		}
		return null;
	}

	@Override
	public JSONObject toJSON() {
		JSONObject jo = super.toJSON();
		jo.put("hardware", hardware);
		if (yaml != null && !"".equals(yaml)) {
			jo.put("yaml", new YAMLtoJSON(yaml).toJSON());
		}
		return jo;
	}
}
