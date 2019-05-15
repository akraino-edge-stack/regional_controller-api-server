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

import java.util.Set;
import java.util.TreeSet;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * Role attributes are of the form {create,read,update,delete}-{object}
 *
 */
public class Role extends BaseBean implements Comparable<Role> {
	private final Set<String> attributes;

	public Role(String uuid, String name, String description, String[] attributes) {
		super(uuid, name, description);
		this.attributes = new TreeSet<String>();
		for (String s : attributes) {
			this.attributes.add(s);
		}
	}

	public Set<String> getAttributes() {
		return attributes;
	}

	@Override
	public JSONObject toJSON() {
		JSONObject jo = super.toJSON();
		JSONArray ja = new JSONArray();
		if (attributes != null) {
			for (String s : attributes) {
				ja.put(s);
			}
		}
		jo.put("attributes", ja);
		return jo;
	}

	@Override
	public int compareTo(Role r2) {
		return getUuid().compareTo(r2.getUuid());
	}
}
