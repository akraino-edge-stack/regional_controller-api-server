/*
 * Copyright (c) 2019, 2020 AT&T Intellectual Property. All rights reserved.
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

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.WebApplicationException;

import org.akraino.regional_controller.db.DB;
import org.akraino.regional_controller.db.DBFactory;
import org.akraino.regional_controller.utils.JSONtoYAML;
import org.akraino.regional_controller.utils.YAMLtoJSON;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.Validator;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class Blueprint extends BaseBean {
	// Some standard workflow names
	public static final String WF_CREATE = "create";
	public static final String WF_UPDATE = "update";
	public static final String WF_DELETE = "delete";
	public static final String WF_REGEX  = "^[a-zA-Z0-9_]{1,36}$";	// WF names must match this RE
	public static final String PARENT_TAG = "parent";
	public static final String HARDWARE_STANZA = "hardware_profile";

	private static final Map<String, String> schema_map = new HashMap<>();
	static {
		// Currently only handle version 1.0.0 of the Blueprint schema
		schema_map.put("1.0.0", "blueprint_schema-1.0.0.json");
	}

	public static String createBlueprint(JSONObject json) throws WebApplicationException {
		Logger logger = LogManager.getLogger();
		String schema = json.optString(SCHEMA_TAG);
		if (schema == null || "".equals(schema)) {
			logger.warn("Missing schema version");
			throw new BadRequestException("ARC-1016: Missing schema version");
		}
		if (!schema_map.keySet().contains(schema)) {
			logger.warn("The schema version "+schema+" is not recognized by this software.");
			throw new BadRequestException("ARC-1024: The schema version "+schema+" is not recognized by this software.");
		}
		String n = json.optString(NAME_TAG);
		if (n == null || "".equals(n)) {
			logger.warn("Missing name");
			throw new BadRequestException("ARC-1013: Missing name");
		}
		String d = json.optString(DESCRIPTION_TAG);
		if (d == null) {
			d = "";
		}
		String v = json.optString(VERSION_TAG);
		if (v == null || "".equals(v)) {
			logger.warn("Missing version");
			throw new BadRequestException("ARC-1017: Missing version");
		}
		JSONObject y = json.optJSONObject(YAML_TAG);
		if (y == null) {
			logger.warn("Missing YAML; are you sure about this Blueprint?");
			throw new BadRequestException("ARC-1008: Missing YAML; are you sure about this Blueprint?");
		}
		// The parent pointer will be stored in the YAML subtree
		String p = y.optString(PARENT_TAG);
		if (p != null && !p.equals("")) {
			if (getBlueprintByUUID(p) == null) {
				throw new BadRequestException("ARC-1033: No parent blueprint exists with UUID "+p);
			}
		}

		// Compare with appropriate JSON schema to validate the rest of the blueprint
		try {
			String schemafile = schema_map.get(schema);
			Object dummyobject = new Blueprint("", "", "", "", "");
			try (InputStream inputStream = dummyobject.getClass().getClassLoader().getResourceAsStream(schemafile)) {
				if (inputStream == null) {
					logger.warn("The schema version "+schema+" is not recognized by this software.");
					throw new BadRequestException("ARC-1024: The schema version "+schema+" is not recognized by this software.");
				}
				JSONObject rawSchema = new JSONObject(new JSONTokener(inputStream));
				Schema schema2 = SchemaLoader.load(rawSchema);
				// throws a ValidationException if this object is invalid
				Validator validator = Validator.builder().build();
				validator.performValidation(schema2, json);
			}
		} catch (IOException ex) {
			// ignore
		} catch (ValidationException ex) {
			logger.warn("Blueprint fails validation.");
			final StringBuilder sb = new StringBuilder();
			ex
				.getCausingExceptions()
				.stream()
				.map(ValidationException::getMessage)
				.forEach(new Consumer<String>() {
					@Override
					public void accept(String t) {
						sb.append("| ").append(t);
					}
				});
			throw new BadRequestException("ARC-1034: Blueprint fails validation."+sb);
		}

		String uuid = json.optString(UUID_TAG);
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
				throw new BadRequestException("ARC-1027: UUID "+uuid+" is already in use.");
			}
		}
		Blueprint b = new Blueprint(uuid, n, d, v, (new JSONtoYAML(y)).toString());

		// Make sure all the workflow names are valid
		for (String nm : b.getWorkFlowNames()) {
			if (!nm.matches(WF_REGEX)) {
				String m = "The workflow name '"+nm+"' is invalid.";
				logger.warn(m);
				throw new BadRequestException("ARC-1031: "+m);
			}
		}
		try {
			DB db = DBFactory.getDB();
			db.createBlueprint(b);
			return uuid;
		} catch (SQLException e1) {
			throw new InternalServerErrorException("ARC-4003: "+e1.getMessage());
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

	public void updateBlueprint() throws WebApplicationException {
		try {
			DB db = DBFactory.getDB();
			db.updateBlueprint(this);
		} catch (SQLException e1) {
			throw new InternalServerErrorException("ARC-4003: "+e1.getMessage());
		}
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

	/**
	 * Check if the Blueprint with the UUID child, as in fact a descendant of the Blueprint with the UUID parent.
	 * @param child the child UUID
	 * @param parent the parent UUID
	 * @return true if it is a descendant
	 */
	public static boolean isChildBlueprint(String child, String parent) {
		Logger logger = LogManager.getLogger();
		logger.info("child="+child + " parent="+parent);
		Blueprint b = getBlueprintByUUID(child);
		logger.info("getBlueprintByUUID returns "+b);
		if (b == null)
			return false;
		String parent_id = b.getParent();
		logger.info("parent_id is "+parent_id);
		if (parent_id == null)
			return false;
		if (parent_id.equalsIgnoreCase(parent))
			return true;
		return isChildBlueprint(parent_id, parent);
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

	public String getParent() {
		if (yaml != null) {
			YAMLtoJSON y = new YAMLtoJSON(yaml);
			JSONObject jo = y.toJSON();
			String parent = jo.optString(PARENT_TAG);
			if (parent != null && parent.length() > 0)
				return parent;
		}
		return null;
	}

	public String getYaml() {
		return yaml;
	}

	public void setYaml(String j) {
		this.yaml = j;
	}

	/**
	 * Return the list of PODs that are using this specific Blueprint.
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

	/**
	 * Return a set of all the workflow names that can be executed from this Blueprint, includes parent Blueprints.
	 * @return the set of workflow names
	 */
	public Set<String> getWorkFlowNames() {
		Set<String> set = new HashSet<>();
		String parent = getParent();
		if (parent != null) {
			Blueprint parentbp = getBlueprintByUUID(parent);
			set = parentbp.getWorkFlowNames();
		}
		if (yaml != null) {
			YAMLtoJSON y = new YAMLtoJSON(yaml);
			JSONObject jo = y.toJSON();
			jo = jo.optJSONObject("workflow");
			if (jo != null) {
				set.addAll(jo.keySet());
			}
		}
		return set;
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
		JSONObject jo = getObjectStanza(HARDWARE_STANZA);
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
										errors.add("Arrays of arrays not allowed for key: "+prefix+key);
										break;
									case "string":
									case "ipaddress":
									case "ipv4":
									case "ipv6":
									case "cidr":
									case "cidrv4":
									case "cidrv6":
										try {
											String v = a.getString(i);
											switch (type2) {
											case "ipaddress":
												if (!isValidIPAddress(v)) {
													errors.add("The value \""+v+"\" for key "+fullkey+" is not a valid IP address.");
												}
												break;
											case "ipv4":
												if (!isValidIPV4Address(v)) {
													errors.add("The value \""+v+"\" for key "+fullkey+" is not a valid IPV4 address.");
												}
												break;
											case "ipv6":
												if (!isValidIPV6Address(v)) {
													errors.add("The value \""+v+"\" for key "+fullkey+" is not a valid IPV6 address.");
												}
												break;
											case "cidr":
												if (!isValidCIDR(v)) {
													errors.add("The value \""+v+"\" for key "+fullkey+" is not a valid CIDR.");
												}
												break;
											case "cidrv4":
												if (!isValidV4CIDR(v)) {
													errors.add("The value \""+v+"\" for key "+fullkey+" is not a valid IPV4 CIDR.");
												}
												break;
											case "cidrv6":
												if (!isValidV6CIDR(v)) {
													errors.add("The value \""+v+"\" for key "+fullkey+" is not a valid IPV6 CIDR.");
												}
												break;
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
					validateString(input, prefix, key, errors);
					break;
				case "ipaddress":
					validateIPaddress(input, prefix, key, errors);
					break;
				case "ipv4":
					validateIPv4address(input, prefix, key, errors);
					break;
				case "ipv6":
					validateIPv6address(input, prefix, key, errors);
					break;
				case "cidr":
					validateCIDR(input, prefix, key, errors);
					break;
				case "cidrv4":
					validateV4CIDR(input, prefix, key, errors);
					break;
				case "cidrv6":
					validateV6CIDR(input, prefix, key, errors);
					break;
				case "integer":
					validateInteger(input, prefix, key, errors);
					break;
				default:
					errors.add("Unknown schema type : "+type);
					break;
				}
			}
		}
	}
	private void validateString(JSONObject input, String prefix, String key, List<String> errors) {
		try {
			input.getString(key);
		} catch (JSONException x) {
			errors.add("Missing required value of type string: "+prefix+key);
		}
	}
	private void validateIPaddress(JSONObject input, String prefix, String key, List<String> errors) {
		try {
			String v = input.getString(key);
			if (!isValidIPAddress(v)) {
				errors.add("The value \""+v+"\" for key "+prefix+key+" is not a valid IP address.");
			}
		} catch (JSONException x) {
			errors.add("Missing required value of type ipaddress: "+prefix+key);
		}
	}
	private void validateIPv4address(JSONObject input, String prefix, String key, List<String> errors) {
		try {
			String v = input.getString(key);
			if (!isValidIPV4Address(v)) {
				errors.add("The value \""+v+"\" for key "+prefix+key+" is not a valid IPV4 address.");
			}
		} catch (JSONException x) {
			errors.add("Missing required value of type ipv4: "+prefix+key);
		}
	}
	private void validateIPv6address(JSONObject input, String prefix, String key, List<String> errors) {
		try {
			String v = input.getString(key);
			if (!isValidIPV6Address(v)) {
				errors.add("The value \""+v+"\" for key "+prefix+key+" is not a valid IPV6 address.");
			}
		} catch (JSONException x) {
			errors.add("Missing required value of type ipv6: "+prefix+key);
		}
	}
	private void validateCIDR(JSONObject input, String prefix, String key, List<String> errors) {
		try {
			String v = input.getString(key);
			if (!isValidCIDR(v)) {
				errors.add("The value \""+v+"\" for key "+prefix+key+" is not a valid CIDR.");
			}
		} catch (JSONException x) {
			errors.add("Missing required value of type cidr: "+prefix+key);
		}
	}
	private void validateV4CIDR(JSONObject input, String prefix, String key, List<String> errors) {
		try {
			String v = input.getString(key);
			if (!isValidV4CIDR(v)) {
				errors.add("The value \""+v+"\" for key "+prefix+key+" is not a valid IPV4 CIDR.");
			}
		} catch (JSONException x) {
			errors.add("Missing required value of type cidrv4: "+prefix+key);
		}
	}
	private void validateV6CIDR(JSONObject input, String prefix, String key, List<String> errors) {
		try {
			String v = input.getString(key);
			if (!isValidV6CIDR(v)) {
				errors.add("The value \""+v+"\" for key "+prefix+key+" is not a valid IPV6 CIDR.");
			}
		} catch (JSONException x) {
			errors.add("Missing required value of type cidrv6: "+prefix+key);
		}
	}
	private void validateInteger(JSONObject input, String prefix, String key, List<String> errors) {
		try {
			input.getInt(key);
		} catch (JSONException x) {
			errors.add("Missing or invalid required value of type integer: "+prefix+key);
		}
	}
	private boolean isValidIPV4Address(final String ip) {
		String[] octets = ip.split("\\.");
		if (octets == null || octets.length != 4)
			return false;
		for (String octet : octets) {
			int n = Integer.parseInt(octet);
			if (n < 0 || n > 255)
				return false;
		}
		return true;
	}
	private boolean isValidIPV6Address(final String ip) {
		// Special check for loopback address
		if (! ip.equals("::1")) {
			String[] hextets = ip.split(":");
			if (hextets == null || hextets.length > 8)
				return false;
			boolean expect_empty = hextets.length != 8;
			for (String hextet : hextets) {
				if (hextet.length() == 0) {
					if (expect_empty) {
						expect_empty = false;
					} else {
						return false;
					}
				} else {
					if (!hextet.matches("[0-9a-fA-F]{1,4}")) {
						return false;
					}
				}
			}
		}
		return true;
	}
	private boolean isValidIPAddress(final String ip) {
		return isValidIPV4Address(ip) || isValidIPV6Address(ip);
	}
	private boolean isValidV4CIDR(final String cidr) {
		String[] parts = cidr.split("/");
		if (parts.length == 2) {
			try {
				int n = Integer.parseInt(parts[1]);
				if (n >= 0 && n <= 32) {
					return isValidIPV4Address(parts[0]);
				}
			} catch (NumberFormatException x) {
				// fall thru
			}
		}
		return false;
	}
	private boolean isValidV6CIDR(final String cidr) {
		String[] parts = cidr.split("/");
		if (parts.length == 2) {
			try {
				int n = Integer.parseInt(parts[1]);
				if (n >= 0 && n <= 128) {
					return isValidIPV6Address(parts[0]);
				}
			} catch (NumberFormatException x) {
				// fall thru
			}
		}
		return false;
	}
	private boolean isValidCIDR(final String cidr) {
		return isValidV4CIDR(cidr) || isValidV6CIDR(cidr);
	}

	public JSONObject getObjectStanza(final String path) {
		String[] pp = path.split("/");
		JSONObject jo = getStanzaCommon(pp);
		if (jo != null) {
			jo = jo.optJSONObject(pp[pp.length-1]);
			if (jo != null)
				return jo;
		}
		String parent = getParent();
		if (parent != null) {
			Blueprint parentbp = getBlueprintByUUID(parent);
			return parentbp.getObjectStanza(path);
		}
		return null;
	}

	public JSONArray getArrayStanza(final String path) {
		String[] pp = path.split("/");
		JSONObject jo = getStanzaCommon(pp);
		JSONArray ja = null;
		if (jo != null) {
			ja = jo.optJSONArray(pp[pp.length-1]);
			if (ja != null)
				return ja;
		}
		String parent = getParent();
		if (parent != null) {
			Blueprint parentbp = getBlueprintByUUID(parent);
			return parentbp.getArrayStanza(path);
		}
		return null;
	}

	private JSONObject getStanzaCommon(String[] pp) {
		if (yaml == null)
			return null;
		YAMLtoJSON y = new YAMLtoJSON(yaml);
		JSONObject jo = y.toJSON();
		for (int i = 0; i < pp.length-1; i++) {
			jo = jo.optJSONObject(pp[i]);
			if (jo == null)
				return null;
		}
		return jo;
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
