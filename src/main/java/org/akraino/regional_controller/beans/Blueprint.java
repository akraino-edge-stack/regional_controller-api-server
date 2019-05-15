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

import java.net.InetAddress;
import java.net.UnknownHostException;
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
import org.akraino.regional_controller.utils.JSONtoYAML;
import org.akraino.regional_controller.utils.YAMLtoJSON;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Blueprint extends BaseBean {
	// Some standard workflow names
	public static final String WF_CREATE = "create";
	public static final String WF_UPDATE = "update";
	public static final String WF_DELETE = "delete";

	private static final Set<String> schema_set = new TreeSet<>();
	static {
		schema_set.add("1.0.0");	// currently only handle version 1.0.0 of the blueprint schema
	}

	public static String createBlueprint(JSONObject json) throws WebApplicationException {
		Logger logger = LogManager.getLogger();
		String schema = json.optString(SCHEMA_TAG);
		if (schema == null || "".equals(schema)) {
			logger.warn("Missing schema version");
			throw new BadRequestException("Missing schema version");
		}
		if (!schema_set.contains(schema)) {
			logger.warn("The schema version "+schema+" is not recognized by this software.");
			throw new BadRequestException("The schema version "+schema+" is not recognized by this software.");
		}
		String n = json.optString(NAME_TAG);
		if (n == null || "".equals(n)) {
			logger.warn("Missing name");
			throw new BadRequestException("Missing name");
		}
		String d = json.optString(DESCRIPTION_TAG);
		if (d == null) {
			d = "";
		}
		String v = json.optString(VERSION_TAG);
		if (v == null || "".equals(v)) {
			logger.warn("Missing version");
			throw new BadRequestException("Missing version");
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
			} while (getBlueprintByUUID(u.toString()) != null);
			uuid = u.toString();
		} else {
			// Use the UUID provided
			if (getBlueprintByUUID(uuid) != null) {
				throw new BadRequestException("UUID "+uuid+" is already in use.");
			}
		}
		Blueprint b = new Blueprint(uuid, n, d, v, (new JSONtoYAML(y)).toString());
		try {
			DB db = DBFactory.getDB();
			db.createBlueprint(b);
			return uuid;
		} catch (SQLException e1) {
			throw new InternalServerErrorException(e1.getMessage());
		}
	}

	public static Collection<Blueprint> getBlueprints() {
		Map<String, Blueprint> map = pullFromDB();
		return map.values();
	}

	public static Blueprint getBlueprintByUUID(final String uuid) {
		Map<String, Blueprint> map = pullFromDB();
		return map.get(uuid);
	}

	private static Map<String, Blueprint> pullFromDB() {
		Map<String, Blueprint> map = new HashMap<>();
		DB db = DBFactory.getDB();
		List<Blueprint> list = db.getBlueprints();
		for (Blueprint b : list) {
			map.put(b.getUuid(), b);
		}
		return map;
	}

	private final String version;
	private String yaml;			// extra JSON describing the Blueprint

	public Blueprint(String uuid, String name, String description, String version, String yaml) {
		super(uuid, name, description);
		this.version = version;
		this.yaml = yaml;
	}

	public String getVersion() {
		return version;
	}

	public String getYaml() {
		return yaml;
	}

	public void setYaml(String j) {
		this.yaml = j;
	}

	/**
	 * Return the list of PODs that are using this Blueprint.
	 * @return the collection (which may be empty).
	 */
	public List<POD> getPODs() {
		List<POD> list = new ArrayList<>();
		String uuid = getUuid();
		for (POD p : POD.getPods()) {
			if (p.getBlueprint().equals(uuid)) {
				list.add(p);
			}
		}
		return list;
	}

	public List<String> getWorkFlowNames() {
		List<String> list = new ArrayList<>();
		if (yaml != null) {
			YAMLtoJSON y = new YAMLtoJSON(yaml);
			JSONObject jo = y.toJSON();
			jo = jo.optJSONObject("workflow");
			if (jo != null) {
				list.addAll(jo.keySet());
			}
		}
		return list;
	}

	/**
	 * Check if this Blueprint is compatible with the list of Nodes in the Edgesite e. It does this by verifying the
	 * nodes in the Edgesite against the hardware_profile stanza in the Blueprint.  The hardware_profile stanza consists
	 * of a rule (that may contain other subsidiary rules) defining what hardware is required for the Blueprint.
	 *
	 * A Rule is:
	 * { and: [ list of rules ] }		# all subsidiary rules must match
	 * { or: [ list of rules ] }		# any one subsidiary rule must match
	 * { uuid: <UUID> [ , min: <N> ] [,  max: <N> ] }	# match the UUID of a HW profile
	 * { name: <RE> [ , min: <N> ] [,  max: <N> ] }		# match the name of a HW profile with a regex
	 *
	 * @param e the Edgesite to test against.
	 * @return true if compatible, false otherwise
	 */
	public List<String> isCompatibleHardware(Edgesite e) {
		JSONObject jo = getObjectStanza("hardware_profile");
		if (jo == null)
			return new ArrayList<>();
		return matchRules(jo, e);
	}
	private List<String> matchRules(JSONObject rule, Edgesite es) {
		JSONArray ja = rule.optJSONArray("and");
		if (ja != null)
			return matchAndRule(ja, es);
		ja = rule.optJSONArray("or");
		if (ja != null)
			return matchOrRule(ja, es);

		List<String> errors = new ArrayList<>();
		String uuid = rule.optString("uuid");
		String name = rule.optString("name");
		int    min  = rule.optInt("min");
		int    max  = rule.optInt("max");
		if (min == 0)
			min = 1;
		if (max == 0)
			max = 1000000;
		if (!"".equals(uuid) && !"".equals(name)) {
			errors.add("Bad rule: uuid and name cannot be used together");
		} else if (!"".equals(uuid)) {
			String s = matchUUIDRule(es, uuid, min, max);
			if (s != null)
				errors.add(s);
		} else if (!"".equals(name)) {
			String s = matchNameRule(es, name, min, max);
			if (s != null)
				errors.add(s);
		} else {
			errors.add("Bad rule: no and, or, uuid, or name");
		}
		return errors;
	}
	private String matchUUIDRule(Edgesite es, String uuid, int min, int max) {
		int matches = 0;
		for (String nuuid : es.getNodes()) {
			Node n = Node.getNodeByUUID(nuuid);
			Hardware h = Hardware.getHardwareByUUID(n.getHardware());
			if (h.getUuid().equalsIgnoreCase(uuid)) {
				matches++;
			}
		}
		if (matches < min)
			return "The number of nodes matching the hardware UUID of "+uuid+" is less than the minimum of "+min;
		if (matches > max)
			return "The number of nodes matching the hardware UUID of "+uuid+" is greater than the maximum of "+max;
		return null;
	}
	private String matchNameRule(Edgesite es, String name, int min, int max) {
		int matches = 0;
		for (String nuuid : es.getNodes()) {
			Node n = Node.getNodeByUUID(nuuid);
			Hardware h = Hardware.getHardwareByUUID(n.getHardware());
			String hname = h.getName();
			if (hname.matches(name)) {
				matches++;
			}
		}
		if (matches < min)
			return "The number of nodes matching the hardware name RE of "+name+" is less than the minimum of "+min;
		if (matches > max)
			return "The number of nodes matching the hardware name RE of "+name+" is greater than the maximum of "+max;
		return null;
	}
	private List<String> matchAndRule(JSONArray ja, Edgesite e) {
		List<String> errors = new ArrayList<>();
		for (int i = 0; i < ja.length(); i++) {
			JSONObject jo = ja.getJSONObject(i);
			if (jo != null) {
				errors.addAll(matchRules(jo, e));
			}
		}
		return errors;
	}
	private List<String> matchOrRule(JSONArray ja, Edgesite e) {
		List<String> errors = new ArrayList<>();
		for (int i = 0; i < ja.length(); i++) {
			JSONObject jo = ja.getJSONObject(i);
			if (jo != null) {
				List<String> z = matchRules(jo, e);
				if (z.isEmpty())
					return z;	// One rule succeeded, so the "or" succeeds
				errors.addAll(z);
			}
		}
		return errors;
	}

	/**
	 * Verify if the YAML provided by the user (from a POST) is compatible with the the input_schema in the workflow
	 * section of the blueprint specified by <i>wf</i>.
	 * @param wf the workflow in the blueprint to check
	 * @param jo the input YAML, converted to JSON
	 * @return
	 */
	public List<String> isCompatibleYAML(String wf, JSONObject input) {
		List<String> errors = new ArrayList<>();
		JSONObject schema = getObjectStanza("workflow/"+wf+"/input_schema");
		recursiveCompare("", schema, input, errors);
		return errors;
	}
	private void recursiveCompare(String prefix, JSONObject schema, JSONObject input, List<String> errors) {
		if (schema == null)
			schema = new JSONObject();
		if (input == null)
			input = new JSONObject();
		for (String key : schema.keySet()) {
			JSONObject j2 = schema.getJSONObject(key);
			String type = j2.optString("type");
			if (type == null || "".equals(type)) {
				errors.add("Bad input_schema; missing type for key: "+prefix+key);
			} else {
				switch (type) {
				case "object":
					try {
						JSONObject props = j2.getJSONObject("properties");
						try {
							JSONObject v = input.getJSONObject(key);
							recursiveCompare(prefix+key+".", props, v, errors);
						} catch (JSONException x) {
							errors.add("Missing required object: "+prefix+key);
						}
					} catch (JSONException x) {
						errors.add("Bad input_schema; missing properties for key: "+prefix+key);
					}
					break;
				case "array":
					try {
						JSONObject items = j2.getJSONObject("items");
						String type2 = items.optString("type");
						if (type == null || "".equals(type)) {
							errors.add("Bad input_schema; missing type for key: "+prefix+key+".items");
						} else {
							try {
								JSONArray a = input.getJSONArray(key);
								for (int i = 0; i < a.length(); i++) {
									String fullkey = String.format("%s%s[%d]", prefix, key, i);
									switch (type2) {
									case "object":
										try {
											JSONObject props = items.getJSONObject("properties");
											try {
												JSONObject v = a.getJSONObject(i);
												recursiveCompare(fullkey+".", props, v, errors);
											} catch (JSONException x) {
												errors.add("Missing required object: "+fullkey);
											}
										} catch (JSONException x) {
											errors.add("Bad input_schema; missing properties for key: "+prefix+key);
										}
										break;
									case "array":
										errors.add("Arrays of arrays not allowed!");
										break;
									case "string":
									case "ipaddress":
									case "cidr":
										try {
											String v = a.getString(i);
											if (type2.contentEquals("ipaddress")) {
												if (!isValidIPAddress(v)) {
													errors.add("The value \""+v+"\" for key "+fullkey+" is not a valid IP address.");
												}
											}
											if (type2.contentEquals("cidr")) {
												if (!isValidCIDR(v)) {
													errors.add("The value \""+v+"\" for key "+fullkey+" is not a valid CIDR.");
												}
											}
										} catch (JSONException x) {
											errors.add("Missing required value of type "+type2+": "+fullkey);
										}
										break;
									case "integer":
										try {
											a.getInt(i);
										} catch (JSONException x) {
											errors.add("Missing or invalid required value of type integer: "+fullkey);
										}
										break;
									}
								}
							} catch (JSONException x) {
								errors.add("Missing required list: "+prefix+key);
							}
						}
					} catch (JSONException x) {
						errors.add("Bad input_schema; missing items for key: "+prefix+key);
					}
					break;
				case "string":
				case "ipaddress":
				case "cidr":
					try {
						String v = input.getString(key);
						if (type.contentEquals("ipaddress")) {
							if (!isValidIPAddress(v)) {
								errors.add("The value \""+v+"\" for key "+prefix+key+" is not a valid IP address.");
							}
						}
						if (type.contentEquals("cidr")) {
							if (!isValidCIDR(v)) {
								errors.add("The value \""+v+"\" for key "+prefix+key+" is not a valid CIDR.");
							}
						}
					} catch (JSONException x) {
						errors.add("Missing required value of type "+type+": "+prefix+key);
					}
					break;
				case "integer":
					try {
						input.getInt(key);
					} catch (JSONException x) {
						errors.add("Missing or invalid required value of type integer: "+prefix+key);
					}
					break;
				default:
					errors.add("Unknown schema type : "+type);
					break;
				}
			}
		}
	}
	private boolean isValidIPAddress(String ip) {
		try {
			InetAddress.getByName(ip);
			return true;
		} catch (UnknownHostException x) {
			return false;
		}
	}
	private boolean isValidCIDR(String cidr) {
		String[] parts = cidr.split("/");
		if (parts.length == 2) {
			try {
				int n = Integer.parseInt(parts[1]);
				if (n >= 0 && n <= 32) {
					// should adjust later for IPv6
					return isValidIPAddress(parts[0]);
				}
			} catch (NumberFormatException x) {
				// fall thru
			}
		}
		return false;
	}

	public JSONObject getObjectStanza(String path) {
		if (yaml == null)
			return null;
		String[] pp = path.split("/");
		YAMLtoJSON y = new YAMLtoJSON(yaml);
		JSONObject jo = y.toJSON();
		for (int i = 0; i < pp.length; i++) {
			jo = jo.optJSONObject(pp[i]);
			if (jo == null)
				return null;
		}
		return jo;
	}

	public JSONArray getArrayStanza(String path) {
		if (yaml == null)
			return null;
		String[] pp = path.split("/");
		YAMLtoJSON y = new YAMLtoJSON(yaml);
		JSONObject jo = y.toJSON();
		for (int i = 0; i < pp.length-1; i++) {
			jo = jo.optJSONObject(pp[i]);
			if (jo == null)
				return null;
		}
		return jo.optJSONArray(pp[pp.length-1]);
	}

	@Override
	public JSONObject toJSON() {
		JSONObject jo = super.toJSON();
		jo.put("version",  version);
		if (yaml != null && !"".equals(yaml)) {
			jo.put("yaml", new YAMLtoJSON(yaml).toJSON());
		}
		return jo;
	}
}
