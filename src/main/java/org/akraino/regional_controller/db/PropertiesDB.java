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

package org.akraino.regional_controller.db;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.akraino.regional_controller.beans.Blueprint;
import org.akraino.regional_controller.beans.Edgesite;
import org.akraino.regional_controller.beans.Hardware;
import org.akraino.regional_controller.beans.Node;
import org.akraino.regional_controller.beans.POD;
import org.akraino.regional_controller.beans.PODEvent;
import org.akraino.regional_controller.beans.PODWorkflow;
import org.akraino.regional_controller.beans.Region;
import org.akraino.regional_controller.beans.Role;
import org.akraino.regional_controller.beans.User;
import org.akraino.regional_controller.beans.UserSession;
import org.akraino.regional_controller.utils.JSONtoYAML;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The PropertiesDB uses a properties file to retrieve all DB rows.  It is primarily meant for testing.
 */
public class PropertiesDB implements DB {
	private static final String DEFAULT_PROPERTIES_FILE = "testing_db.properties";
	private static final Map<String, UserSession> sessions = new HashMap<>();
	private static final Logger logger = LogManager.getLogger();

	private final Properties props;

	public PropertiesDB(Properties api_props) {
		props = new Properties();
		InputStream is = null;
		try {
			String path = api_props.getProperty("db.path", DEFAULT_PROPERTIES_FILE);
			is = getClass().getClassLoader().getResourceAsStream(path);
			props.load(is);
		} catch (IOException e) {
			logger.warn(e);
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
	}

	// BLUEPRINTS ---------------------------------------------------------------------------------------------------------
	static final String BLUEPRINT_PROPERTY = "blueprint";

	@Override
	public void createBlueprint(final Blueprint b) throws SQLException {
		String key = nextKey(BLUEPRINT_PROPERTY);
		String value = b.getUuid() + "," + b.getName() + "," + b.getDescription() + "," + b.getVersion() + "," + b.getYaml();
		props.setProperty(key, value);
	}

	@Override
	public List<Blueprint> getBlueprints() {
		List<Blueprint> list = new ArrayList<>();
		for (String[] row : rowIterable(BLUEPRINT_PROPERTY)) {
			if (row.length > 5) {
				for (int i = 5; i < row.length; i++) {
					row[4] = row[4] + ", " + row[i];
				}
			}
			String yaml = new JSONtoYAML(row[4]).toString();
			Blueprint bp = new Blueprint(row[0], row[1], row[2], row[3], yaml);
			list.add(bp);
		}
		return list;
	}

	@Override
	public void updateBlueprint(Blueprint b) throws SQLException {
		deleteBlueprint(b);
		createBlueprint(b);
	}

	@Override
	public void deleteBlueprint(Blueprint b) {
		deleteProperty(BLUEPRINT_PROPERTY, b.getUuid());
	}

	// EDGESITES ---------------------------------------------------------------------------------------------------------
	@Override
	public void createEdgesite(final Edgesite e) {
		String key = nextKey("edgesite");
		String[] region1 = e.getRegions().toArray(new String[0]);
		String value = e.getUuid() + "," + e.getName() + "," + e.getDescription() + "," + region1[0];
		props.setProperty(key, value);
	}

	@Override
	public List<Edgesite> getEdgesites() {
		List<Edgesite> list = new ArrayList<>();
		for (String[] row : rowIterable("edgesite")) {
			Edgesite es = new Edgesite(row[0], row[1], row[2], Arrays.asList(row[3]));
			list.add(es);
		}
		return list;
	}

	@Override
	public void updateEdgesite(Edgesite e) throws SQLException {
		deleteEdgesite(e);
		createEdgesite(e);
	}

	@Override
	public void deleteEdgesite(Edgesite e) {
		deleteProperty("edgesite", e.getUuid());
	}

	// HARDWARE ---------------------------------------------------------------------------------------------------------
	@Override
	public void createHardware(final Hardware h) throws SQLException {
		String key = nextKey("hardware");
		String value = h.getUuid() + "," + h.getName() + "," + h.getDescription() + "," + h.getYaml();
		props.setProperty(key, value);
	}

	@Override
	public List<Hardware> getHardware() {
		List<Hardware> list = new ArrayList<>();
		for (String[] row : rowIterable("hardware")) {
			Hardware h = new Hardware(row[0], row[1], row[2], row[3]);
			list.add(h);
		}
		return list;
	}

	@Override
	public void updateHardware(Hardware h) throws SQLException {
		deleteHardware(h);
		createHardware(h);
	}

	@Override
	public void deleteHardware(Hardware h) {
		deleteProperty("hardware", h.getUuid());
	}

	// NODES ---------------------------------------------------------------------------------------------------------
	@Override
	public void createNode(final Node n) throws SQLException {
		String key = nextKey("node");
		String value = n.getUuid() + "," + n.getName() + "," + n.getDescription() + "," + n.getHardware() + "," + n.getYaml();
		props.setProperty(key, value);
	}

	@Override
	public List<Node> getNodes() {
		List<Node> list = new ArrayList<>();
		for (String[] row : rowIterable("node")) {
			Node n = new Node(row[0], row[1], row[2], row[3], row[4]);
			list.add(n);
		}
		return list;
	}

	@Override
	public void updateNode(Node n) throws SQLException {
		deleteNode(n);
		createNode(n);
	}

	@Override
	public void deleteNode(final Node n) {
		deleteProperty("node", n.getUuid());
	}

	// PODS ---------------------------------------------------------------------------------------------------------
	@Override
	public void createPod(final POD p) {
		String key = nextKey("pod");
		String value = p.getUuid() + "," + p.getName() + "," + p.getDescription() + "," + p.getBlueprint() + "," + p.getEdgesite() + "," + p.getYaml();
		props.setProperty(key, value);
	}

	@Override
	public List<POD> getPods() {
		List<POD> list = new ArrayList<>();
		for (String[] row : rowIterable("pod")) {
			POD p = new POD(row[0], row[1], row[2], row[3], row[4], row[5]);
			list.add(p);
		}
		return list;
	}

	@Override
	public void updatePod(final POD p) {
		createPod(p);
	}

	@Override
	public void deletePod(final POD p) {
		deleteProperty("pod", p.getUuid());
	}

	// PODS EVENTS ---------------------------------------------------------------------------------------------------------
	@Override
	public void createPodEvent(final PODEvent pe) {
		// do nothing
	}

	@Override
	public List<PODEvent> getPODEvents(String uuid) {
		return new ArrayList<>();
	}

	// PODS_WORKFLOWS ---------------------------------------------------------------------------------------------------------
	@Override
	public void createPodWorkflow(final PODWorkflow pw) throws SQLException {
		// do nothing - for now
	}

	@Override
	public List<PODWorkflow> getPODWorkflows(String uuid) {
		// do nothing - for now
		return new ArrayList<>();
	}

	@Override
	public void updatePodWorkflow(final PODWorkflow pw) throws SQLException {
		// do nothing - for now
	}

	// REGIONS ---------------------------------------------------------------------------------------------------------
	@Override
	public void createRegion(final Region r) {
		String key = nextKey("region");
		String value = r.getUuid() + "," + r.getName() + "," + r.getDescription();
		props.setProperty(key, value);
	}

	@Override
	public List<Region> getRegions() {
		List<Region> list = new ArrayList<>();
		for (String[] row : rowIterable("region")) {
			Region r = new Region(row[0], row[1], row[2]);
			list.add(r);
		}
		return list;
	}

	@Override
	public void updateRegion(Region r) throws SQLException {
		deleteRegion(r);
		createRegion(r);
	}

	@Override
	public void deleteRegion(Region r) {
		deleteProperty("region", r.getUuid());
	}

	// SESSIONS ---------------------------------------------------------------------------------------------------------
	@Override
	public void createSession(UserSession us) {
		if (us != null) {
			synchronized (sessions) {
				sessions.put(us.getToken(), us);
			}
		}
	}

	@Override
	public UserSession getSession(String token) {
		return sessions.get(token);
	}

	@Override
	public void invalidateSession(UserSession us) {
		if (us != null) {
			synchronized (sessions) {
				sessions.remove(us.getToken());
			}
		}
	}

	// USERS ---------------------------------------------------------------------------------------------------------
	@Override
	public User getUser(String name) {
		for (String[] row : rowIterable("user")) {
			User u = new User(row[0], row[1], row[2], row[3]);
			if (name.equals(u.getName())) {
				if (row.length > 4) {
					// Assign any roles - the PropertiesDB only supports one role per user
					Set<Role> roles = new TreeSet<>();
					roles.add(getRole(row[4]));
					u.setRoles(roles);
				}
				return u;
			}
		}
		return null;
	}

	private Role getRole(String name) {
		for (String[] row : rowIterable("role")) {
			String[] attributes = new String[0];
			if (row.length > 3) {
				attributes = new String[row.length-3];
				System.arraycopy(row, 3, attributes, 0, attributes.length);
			}
			Role r = new Role(row[0], row[1], row[2], attributes);
			if (name.equals(r.getName())) {
				return r;
			}
		}
		return null;
	}

	private Iterable<String[]> rowIterable(final String type) {
		return new Iterable<String[]>() {

			@Override
			public Iterator<String[]> iterator() {
				return new Iterator<String[]>() {
					private int n = 0;

					@Override
					public boolean hasNext() {
						String key = type + "." + (n+1);
						return props.get(key) != null;
					}

					@Override
					public String[] next() {
						String key = type + "." + ++n;
						String s = props.getProperty(key);
						if (s != null) {
							String[] rv = s.split(",");
							for (int i = 0; i < rv.length; i++) {
								rv[i] = rv[i].trim();
							}
							return rv;
						} else {
							throw new NoSuchElementException();
						}
					}
				};
			}

		};
	}
	private String nextKey(String base) {
		for (int n = 1; ; n++) {
			String key = base + "." + n;
			if (props.getProperty(key) == null) {
				return key;
			}
		}
	}
	private void deleteProperty(String base, String uuid) {
		for (String key : props.stringPropertyNames()) {
			if (key.startsWith(base)) {
				String val = props.getProperty(key);
				String[] rv = val.split(",");
				if (rv[0].equals(uuid)) {
					props.remove(key);
					return;
				}
			}
		}
	}
}
