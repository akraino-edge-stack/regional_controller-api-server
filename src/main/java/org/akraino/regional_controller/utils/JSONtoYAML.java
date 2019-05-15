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

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;

public class JSONtoYAML {
	private final JSONObject jo;

	public JSONtoYAML(String s) {
		this.jo = new JSONObject(s);
	}

	public JSONtoYAML(JSONObject jo) {
		this.jo = jo;
	}

	@Override
	public String toString() {
		Map<String, Object> map = convertObject(jo);
		StringWriter writer = new StringWriter();
		Yaml yaml = new Yaml();
		yaml.dump(map, writer);
		return writer.toString();
	}

	private Map<String, Object> convertObject(JSONObject jo) {
		Map<String, Object> map = new HashMap<>();
		for (String key : jo.keySet()) {
			Object val = jo.get(key);
			map.put(key, convert(val));
		}
		return map;
	}

	private List<Object> convertArray(JSONArray ja) {
		List<Object> list = new ArrayList<>();
		for (int i = 0; i < ja.length(); i++) {
			Object val = ja.get(i);
			list.add(convert(val));
		}
		return list;
	}

	private Object convert(Object val) {
		if (val instanceof JSONArray) {
			return convertArray((JSONArray) val);
		}
		if (val instanceof JSONObject) {
			return convertObject((JSONObject) val);
		}
		return val;
	}

	public static void main(String[] a) {
		String[] tests = {
			"{\"blueprints\":[{\"foo\":\"bar\",\"name\":\"Unicycle\",\"description\":\"Multi node cluster using Airship for deployment\",\"uuid\":\"82f490de-2e28-11e9-a0e0-0017f20dbff8\",\"version\":\"0.0.2-SNAPSHOT\",\"url\":\"/api/v1/blueprint/82f490de-2e28-11e9-a0e0-0017f20dbff8\"},{\"name\":\"Rover\",\"description\":\"Single node cluster using Airship-in-a-bottle for deployment\",\"uuid\":\"827cfe84-2e28-11e9-bb34-0017f20dbff8\",\"version\":\"0.0.2-SNAPSHOT\",\"url\":\"/api/v1/blueprint/827cfe84-2e28-11e9-bb34-0017f20dbff8\"},{\"name\":\"REC\",\"description\":\"Radio Edge Cloud (NCIR)\",\"uuid\":\"e17d25f6-3dcf-11e9-ad7a-770ce7e08f5e\",\"version\":\"1.0\",\"url\":\"/api/v1/blueprint/e17d25f6-3dcf-11e9-ad7a-770ce7e08f5e\"}]}\n",
			"{\"edgesites\":[{\"nodes\":[],\"name\":\"Atlanta-2\",\"description\":\"Atlanta #2\",\"region\":\"680ce7ca-2e33-11e9-972f-0017f20dbff8\",\"uuid\":\"2d353844-3dcb-11e9-953b-8b0554de4c14\",\"url\":\"/api/v1/edgesite/2d353844-3dcb-11e9-953b-8b0554de4c14\"},{\"nodes\":[],\"name\":\"Atlanta-4\",\"description\":\"Atlanta #4\",\"region\":\"680ce7ca-2e33-11e9-972f-0017f20dbff8\",\"uuid\":\"2d353952-3dcb-11e9-953d-73acafa46a9f\",\"url\":\"/api/v1/edgesite/2d353952-3dcb-11e9-953d-73acafa46a9f\"},{\"nodes\":[],\"name\":\"Atlanta-3\",\"description\":\"Atlanta #3\",\"region\":\"680ce7ca-2e33-11e9-972f-0017f20dbff8\",\"uuid\":\"2d3538c6-3dcb-11e9-953c-cf5ab5df3fc8\",\"url\":\"/api/v1/edgesite/2d3538c6-3dcb-11e9-953c-cf5ab5df3fc8\"},{\"nodes\":[],\"name\":\"PA-Cluster-1\",\"description\":\"Palo Alto #1\",\"region\":\"015589b6-2fd1-11e9-8de4-07a20e8ebae1\",\"uuid\":\"2d35348e-3dcb-11e9-9534-2365184b56d9\",\"url\":\"/api/v1/edgesite/2d35348e-3dcb-11e9-9534-2365184b56d9\"},{\"nodes\":[],\"name\":\"PA-Cluster-2\",\"description\":\"Palo Alto #2\",\"region\":\"015589b6-2fd1-11e9-8de4-07a20e8ebae1\",\"uuid\":\"2d35351a-3dcb-11e9-9535-e36fdca4d937\",\"url\":\"/api/v1/edgesite/2d35351a-3dcb-11e9-9535-e36fdca4d937\"},{\"nodes\":[],\"name\":\"Atlanta-1\",\"description\":\"Atlanta #1\",\"region\":\"680ce7ca-2e33-11e9-972f-0017f20dbff8\",\"uuid\":\"2d3537c2-3dcb-11e9-953a-9bd0c95c8e50\",\"url\":\"/api/v1/edgesite/2d3537c2-3dcb-11e9-953a-9bd0c95c8e50\"},{\"nodes\":[],\"name\":\"MT-Cluster-1\",\"description\":\"Middletown #1\",\"region\":\"59b9daca-2e33-11e9-8b69-0017f20dbff8\",\"uuid\":\"2d35307e-3dcb-11e9-9532-27ce192bb5c9\",\"url\":\"/api/v1/edgesite/2d35307e-3dcb-11e9-9532-27ce192bb5c9\"},{\"nodes\":[],\"name\":\"Chicago-2\",\"description\":\"Chicago #2\",\"region\":\"59b9daca-2e33-11e9-8b69-0017f20dbff8\",\"uuid\":\"2d353628-3dcb-11e9-9537-0b640b1bffec\",\"url\":\"/api/v1/edgesite/2d353628-3dcb-11e9-9537-0b640b1bffec\"},{\"nodes\":[],\"name\":\"Chicago-1\",\"description\":\"Chicago #1\",\"region\":\"59b9daca-2e33-11e9-8b69-0017f20dbff8\",\"uuid\":\"2d35359c-3dcb-11e9-9536-dfcac5928c4d\",\"url\":\"/api/v1/edgesite/2d35359c-3dcb-11e9-9536-dfcac5928c4d\"},{\"nodes\":[],\"name\":\"Chicago-3\",\"description\":\"Chicago #3\",\"region\":\"59b9daca-2e33-11e9-8b69-0017f20dbff8\",\"uuid\":\"2d3536aa-3dcb-11e9-9538-9f041af1eeb8\",\"url\":\"/api/v1/edgesite/2d3536aa-3dcb-11e9-9538-9f041af1eeb8\"},{\"nodes\":[],\"name\":\"MT-Cluster-2\",\"description\":\"Middletown #2\",\"region\":\"59b9daca-2e33-11e9-8b69-0017f20dbff8\",\"uuid\":\"2d3533e4-3dcb-11e9-9533-87ac04f6a7e6\",\"url\":\"/api/v1/edgesite/2d3533e4-3dcb-11e9-9533-87ac04f6a7e6\"},{\"nodes\":[],\"name\":\"Chicago-4\",\"description\":\"Chicago #4\",\"region\":\"59b9daca-2e33-11e9-8b69-0017f20dbff8\",\"uuid\":\"2d353736-3dcb-11e9-9539-43216df93629\",\"url\":\"/api/v1/edgesite/2d353736-3dcb-11e9-9539-43216df93629\"}]}",
			"{\"nodes\":[{\"name\":\"node13\",\"description\":\"Node 13\",\"uuid\":\"0a106d4c-47fe-11e9-95ee-b783665f2118\",\"url\":\"/api/v1/node/0a106d4c-47fe-11e9-95ee-b783665f2118\"},{\"name\":\"node17\",\"description\":\"Node 17\",\"uuid\":\"15ff23dc-47fe-11e9-a7a8-4398edc4664f\",\"url\":\"/api/v1/node/15ff23dc-47fe-11e9-a7a8-4398edc4664f\"},{\"name\":\"node20\",\"description\":\"Node 20\",\"uuid\":\"1ef2a4a0-47fe-11e9-ac4c-1b881663eaff\",\"url\":\"/api/v1/node/1ef2a4a0-47fe-11e9-ac4c-1b881663eaff\"},{\"name\":\"node01\",\"description\":\"Node 1\",\"uuid\":\"e63f27e6-47fd-11e9-b4b5-2bcc0d21031b\",\"url\":\"/api/v1/node/e63f27e6-47fd-11e9-b4b5-2bcc0d21031b\"},{\"name\":\"node12\",\"description\":\"Node 12\",\"uuid\":\"0714f40a-47fe-11e9-b1a7-cf7d5cdb612e\",\"url\":\"/api/v1/node/0714f40a-47fe-11e9-b1a7-cf7d5cdb612e\"},{\"name\":\"node05\",\"description\":\"Node 5\",\"uuid\":\"f23095e4-47fd-11e9-96ac-3fd64fcc2792\",\"url\":\"/api/v1/node/f23095e4-47fd-11e9-96ac-3fd64fcc2792\"},{\"name\":\"node16\",\"description\":\"Node 16\",\"uuid\":\"1303aca2-47fe-11e9-b680-eb477aa82825\",\"url\":\"/api/v1/node/1303aca2-47fe-11e9-b680-eb477aa82825\"},{\"name\":\"node14\",\"description\":\"Node 14\",\"uuid\":\"0d0c4f48-47fe-11e9-9204-d7daec38a2f8\",\"url\":\"/api/v1/node/0d0c4f48-47fe-11e9-9204-d7daec38a2f8\"},{\"name\":\"node15\",\"description\":\"Node 15\",\"uuid\":\"10081fd8-47fe-11e9-820d-372ca4a11e2a\",\"url\":\"/api/v1/node/10081fd8-47fe-11e9-820d-372ca4a11e2a\"},{\"name\":\"node19\",\"description\":\"Node 19\",\"uuid\":\"1bf6c29a-47fe-11e9-a236-1f1ebfcdb341\",\"url\":\"/api/v1/node/1bf6c29a-47fe-11e9-a236-1f1ebfcdb341\"},{\"name\":\"node07\",\"description\":\"Node 7\",\"uuid\":\"f827d12e-47fd-11e9-ba83-1f2645e15e53\",\"url\":\"/api/v1/node/f827d12e-47fd-11e9-ba83-1f2645e15e53\"},{\"name\":\"node18\",\"description\":\"Node 18\",\"uuid\":\"18fb1bb8-47fe-11e9-b0a6-4710b55fad5c\",\"url\":\"/api/v1/node/18fb1bb8-47fe-11e9-b0a6-4710b55fad5c\"},{\"name\":\"node09\",\"description\":\"Node 9\",\"uuid\":\"fe200ccc-47fd-11e9-9efb-9bfd41625790\",\"url\":\"/api/v1/node/fe200ccc-47fd-11e9-9efb-9bfd41625790\"},{\"name\":\"node11\",\"description\":\"Node 11\",\"uuid\":\"0417fc5c-47fe-11e9-a33d-937b5486310c\",\"url\":\"/api/v1/node/0417fc5c-47fe-11e9-a33d-937b5486310c\"},{\"name\":\"node10\",\"description\":\"Node 10\",\"uuid\":\"011bc90c-47fe-11e9-a04c-4f6070745df6\",\"url\":\"/api/v1/node/011bc90c-47fe-11e9-a04c-4f6070745df6\"},{\"name\":\"node08\",\"description\":\"Node 8\",\"uuid\":\"fb24902e-47fd-11e9-a6e6-dbb9904f6eea\",\"url\":\"/api/v1/node/fb24902e-47fd-11e9-a6e6-dbb9904f6eea\"},{\"name\":\"node03\",\"description\":\"Node 3\",\"uuid\":\"ec399b9a-47fd-11e9-9f20-af67efa1a3dd\",\"url\":\"/api/v1/node/ec399b9a-47fd-11e9-9f20-af67efa1a3dd\"},{\"name\":\"node06\",\"description\":\"Node 6\",\"uuid\":\"f52c21b4-47fd-11e9-aaee-0b3beccb8442\",\"url\":\"/api/v1/node/f52c21b4-47fd-11e9-aaee-0b3beccb8442\"},{\"name\":\"node02\",\"description\":\"Node 2\",\"uuid\":\"e93df346-47fd-11e9-8eef-4f92eac06ca2\",\"url\":\"/api/v1/node/e93df346-47fd-11e9-8eef-4f92eac06ca2\"},{\"name\":\"node04\",\"description\":\"Node 4\",\"uuid\":\"ef35175c-47fd-11e9-b69c-1f9861d8a906\",\"url\":\"/api/v1/node/ef35175c-47fd-11e9-b69c-1f9861d8a906\"}]}\n",
			"{\"blueprints\":{\"Rover\":\"0.0.2-SNAPSHOT\",\"REC\":\"1.0\",\"Unicycle\":\"0.0.2-SNAPSHOT\"},\"containers\":{\"akraino-regional_controller\":\"0.0.1\",\"osixia/openldap\":\"1.2.3\",\"postgres\":\"9.6.9\"}}\n",
			"{\"regions\":[{\"name\":\"US Northeast\",\"description\":\"NYNEX/SNET Area\",\"uuid\":\"59b9daca-2e33-11e9-8b69-0017f20dbff8\",\"url\":\"/api/v1/region/59b9daca-2e33-11e9-8b69-0017f20dbff8\"},{\"name\":\"US South\",\"description\":\"BellSouth Area\",\"uuid\":\"680ce7ca-2e33-11e9-972f-0017f20dbff8\",\"url\":\"/api/v1/region/680ce7ca-2e33-11e9-972f-0017f20dbff8\"},{\"name\":\"US Central\",\"description\":\"Ameritech Area\",\"uuid\":\"a591446c-313d-11e9-b4cb-0017f20dbff8\",\"url\":\"/api/v1/region/a591446c-313d-11e9-b4cb-0017f20dbff8\"},{\"name\":\"US Northwest\",\"description\":\"USWest Area\",\"uuid\":\"5c1e6560-2e33-11e9-821c-0017f20dbff8\",\"url\":\"/api/v1/region/5c1e6560-2e33-11e9-821c-0017f20dbff8\"},{\"name\":\"US West\",\"description\":\"Pacific Telesis Area\",\"uuid\":\"015589b6-2fd1-11e9-8de4-07a20e8ebae1\",\"url\":\"/api/v1/region/015589b6-2fd1-11e9-8de4-07a20e8ebae1\"},{\"name\":\"new_region\",\"description\":\"testing only\",\"uuid\":\"f7902439-eaed-4532-87f4-58a10549ebe5\",\"url\":\"/api/v1/region/f7902439-eaed-4532-87f4-58a10549ebe5\"}]}\n"
		};
		for (String t : tests) {
			JSONObject jo = new JSONObject(t);
			JSONtoYAML jy = new JSONtoYAML(jo);
			System.out.println(jy);
			System.out.println();
		}
	}
}
