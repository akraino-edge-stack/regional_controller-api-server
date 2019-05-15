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

package org.akraino.regional_controller.utils;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;

public class YAMLtoJSON {
	private String yaml;

	public YAMLtoJSON(String yaml) {
		this.yaml = yaml;
	}

	public JSONObject toJSON() {
		Yaml yaml = new Yaml();
		@SuppressWarnings("unchecked")
		Map<String, Object> map = (Map<String, Object>) yaml.load(this.yaml);
		if (map == null) {
			Logger logger = LogManager.getLogger();
			logger.warn("Map is null for content "+yaml);
		}
		return convertMap(map);
	}

	private JSONObject convertMap(Map<String, Object> map) {
		JSONObject jo = new JSONObject();
		if (map != null) {
			for (String key : map.keySet()) {
				Object val = map.get(key);
				jo.put(key, convertVal(val));
			}
		}
		return jo;
	}

	private JSONArray convertList(List<Object> list) {
		JSONArray ja = new JSONArray();
		for (Object val : list) {
			ja.put(convertVal(val));
		}
		return ja;
	}

	@SuppressWarnings("unchecked")
	private Object convertVal(Object obj) {
		if (obj != null) {
			if (obj instanceof List) {
				return convertList( (List<Object>) obj);
			}
			if (obj instanceof Map) {
				return convertMap( (Map<String, Object>) obj);
			}
		}
		return obj;
	}
}
