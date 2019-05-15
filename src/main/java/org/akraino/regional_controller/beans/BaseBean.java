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

import org.akraino.regional_controller.utils.JSONtoYAML;
import org.json.JSONObject;

public class BaseBean {
	public static final String SCHEMA_TAG      = "blueprint";
	public static final String UUID_TAG        = "uuid";
	public static final String NAME_TAG        = "name";
	public static final String DESCRIPTION_TAG = "description";
	public static final String VERSION_TAG     = "version";
	public static final String YAML_TAG        = "yaml";

	private final String uuid;
	private final String name;
	private final String description;

	protected BaseBean(String uuid, String name, String description) {
		this.uuid = uuid;
		this.name = name;
		this.description = description;
	}

	public String getUuid() {
		return uuid;
	}

	public String getId() {
		// Get the shortened version of the UUID
		return getUuid().substring(0, 8);
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public JSONObject toJSON() {
		JSONObject jo = new JSONObject();
		jo.put(UUID_TAG,  uuid);
		jo.put(NAME_TAG,  name);
		if (description != null && !"".contentEquals(description)) {
			jo.put(DESCRIPTION_TAG,  description);
		}
		return jo;
	}

	public String toYAML() {
		JSONtoYAML j = new JSONtoYAML(toJSON());
		return j.toString();
	}

	@Override
	public String toString() {
		return toJSON().toString();
	}
}
