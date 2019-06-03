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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;

import org.akraino.regional_controller.beans.Blueprint;
import org.akraino.regional_controller.beans.Edgesite;
import org.akraino.regional_controller.beans.Hardware;
import org.akraino.regional_controller.beans.Node;
import org.akraino.regional_controller.beans.POD;
import org.akraino.regional_controller.beans.PODEvent;
import org.akraino.regional_controller.beans.Region;
import org.akraino.regional_controller.beans.Role;
import org.akraino.regional_controller.beans.User;
import org.akraino.regional_controller.beans.UserSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The StandardDB uses an SQL database (normally MariaDB) for all items required.
 */
public class StandardDB implements DB {
	protected static final Logger logger = LogManager.getLogger();

	public final String db_url;
	public final String db_login;
	public final String db_password;
	public final Queue<Connection> connqueue;

	public StandardDB(Properties api_props) throws ClassNotFoundException {
		this.db_url      = api_props.getProperty("db.jdbc.url");
		this.db_login    = api_props.getProperty("db.jdbc.user");
		this.db_password = api_props.getProperty("db.jdbc.password");
		Class.forName(     api_props.getProperty("db.driver"));
		this.connqueue   = new LinkedList<>();
	}

	private Connection getConnection() throws SQLException {
		// getConnection() and releaseConnection() implement a very simple Connection pool.
		// This should probably be upgraded later.
		synchronized (connqueue) {
			while (connqueue.size() > 0) {
				Connection c = connqueue.remove();
				if (!c.isClosed()) {
					if (c.isValid(1)) {
						return c;
					}
					try {
						c.close();
					} catch (Exception e) {
						// ignore this
					}
				}
			}
		}
		return DriverManager.getConnection(db_url, db_login, db_password);
	}

	private void releaseConnection(Connection conn) {
		if (conn != null) {
			synchronized (connqueue) {
				connqueue.add(conn);
			}
		}
	}

	// BLUEPRINTS ---------------------------------------------------------------------------------------------------------
	@Override
	public void createBlueprint(final Blueprint b) throws SQLException {
		Connection conn = null;
		try {
			conn = getConnection();
			String sql = "INSERT INTO AKRAINO.BLUEPRINT (uuid, name, description, version, yaml) VALUES(?, ?, ?, ?, ?)";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setString(1, b.getUuid());
				stmt.setString(2, b.getName());
				stmt.setString(3, b.getDescription());
				stmt.setString(4, b.getVersion());
				stmt.setString(5, b.getYaml());
				stmt.execute();
			}
		} catch (SQLException ex) {
			logger.error(ex);
			throw ex;
		} finally {
			releaseConnection(conn);
		}
	}

	@Override
	public List<Blueprint> getBlueprints() {
		List<Blueprint> list = new ArrayList<>();
		Connection conn = null;
		try {
			conn = getConnection();
			try (Statement stmt = conn.createStatement()) {
				String sql = "SELECT * FROM AKRAINO.BLUEPRINT";
				do {
					try (ResultSet rs = stmt.executeQuery(sql)) {
						while (rs.next() ) {
							String version     = rs.getString("version");
							String description = rs.getString("description");
							String yaml        = rs.getString("yaml");
							Blueprint bp = new Blueprint(
								rs.getString("uuid"),
								rs.getString("name"),
								(description == null) ? "" : description,
								(version == null) ? "" : version,
								(yaml == null) ? "" : yaml
							);
							list.add(bp);
						}
					}
				} while (stmt.getMoreResults() || stmt.getUpdateCount() != -1);
			}
		} catch (SQLException e) {
			logger.error(e);
		} finally {
			releaseConnection(conn);
		}
		return list;
	}

	@Override
	public void deleteBlueprint(Blueprint b) throws SQLException {
		Connection conn = null;
		try {
			conn = getConnection();
			String sql = "DELETE FROM AKRAINO.BLUEPRINT WHERE uuid = ?";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setString(1, b.getUuid());
				stmt.execute();
			}
		} catch (SQLException ex) {
			logger.error(ex);
			throw ex;
		} finally {
			releaseConnection(conn);
		}
	}

	// EDGESITES ---------------------------------------------------------------------------------------------------------
	@Override
	public void createEdgesite(final Edgesite e) throws SQLException {
		Connection conn = null;
		try {
			conn = getConnection();
			String sql = "INSERT INTO AKRAINO.EDGESITE (uuid, name, description) VALUES(?, ?, ?)";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setString(1, e.getUuid());
				stmt.setString(2, e.getName());
				stmt.setString(3, e.getDescription());
				stmt.execute();
			}

			sql = "INSERT INTO AKRAINO.EDGESITE_ARRAYS (uuid, fkey, type) VALUES(?, ?, ?)";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				for (String s : e.getNodes()) {
					stmt.setString(1, e.getUuid());
					stmt.setString(2, s);
					stmt.setString(3, "N");
					stmt.execute();
				}
				for (String s : e.getRegions()) {
					stmt.setString(1, e.getUuid());
					stmt.setString(2, s);
					stmt.setString(3, "R");
					stmt.execute();
				}
			}
		} catch (SQLException ex) {
			logger.error(ex);
			throw ex;
		} finally {
			releaseConnection(conn);
		}
	}

	@Override
	public List<Edgesite> getEdgesites() {
		Map<String, Edgesite> map = new HashMap<>();
		Connection conn = null;
		try {
			conn = getConnection();
			try (Statement  stmt = conn.createStatement()) {
				String sql = "SELECT * FROM AKRAINO.EDGESITE";
				do {
					try (ResultSet rs = stmt.executeQuery(sql)) {
						while (rs.next() ) {
							String description = rs.getString("description");
							Edgesite es = new Edgesite(
								rs.getString("uuid"),
								rs.getString("name"),
								(description == null) ? "" : description
							);
							map.put(es.getUuid(), es);
						}
					}
				} while (stmt.getMoreResults() || stmt.getUpdateCount() != -1);

				sql = "SELECT * FROM AKRAINO.EDGESITE_ARRAYS";
				do {
					try (ResultSet rs = stmt.executeQuery(sql)) {
						while (rs.next() ) {
							String uuid = rs.getString("uuid");
							Edgesite es = map.get(uuid);
							if (uuid != null && es != null) {
								String fkey = rs.getString("fkey");
								String type = rs.getString("type");
								if (type.equals("N")) {
									es.getNodes().add(fkey);
								}
								if (type.equals("R")) {
									es.getRegions().add(fkey);
								}
							}
						}
					}
				} while (stmt.getMoreResults() || stmt.getUpdateCount() != -1);
			}
		} catch (SQLException e) {
			logger.error(e);
		} finally {
			releaseConnection(conn);
		}
		List<Edgesite> list = new ArrayList<Edgesite>();
		list.addAll(map.values());
		return list;
	}

	@Override
	public void deleteEdgesite(Edgesite e) throws SQLException {
		Connection conn = null;
		try {
			conn = getConnection();
			String sql = "DELETE FROM AKRAINO.EDGESITE WHERE uuid = ?";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setString(1, e.getUuid());
				stmt.execute();
			}

			sql = "DELETE FROM AKRAINO.EDGESITE_ARRAYS WHERE uuid = ?";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setString(1, e.getUuid());
				stmt.execute();
			}
		} catch (SQLException ex) {
			logger.error(ex);
			throw ex;
		} finally {
			releaseConnection(conn);
		}
	}

	// HARDWARE ---------------------------------------------------------------------------------------------------------
	@Override
	public void createHardware(final Hardware h) throws SQLException {
		Connection conn = null;
		try {
			conn = getConnection();
			String sql = "INSERT INTO AKRAINO.HARDWARE (uuid, name, description, yaml) VALUES(?, ?, ?, ?)";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setString(1, h.getUuid());
				stmt.setString(2, h.getName());
				stmt.setString(3, h.getDescription());
				stmt.setString(4, h.getYaml());
				stmt.execute();
			}
		} catch (SQLException e) {
			logger.error(e);
			throw e;
		} finally {
			releaseConnection(conn);
		}
	}

	@Override
	public List<Hardware> getHardware() {
		List<Hardware> list = new ArrayList<>();
		Connection conn = null;
		try {
			conn = getConnection();
			try (Statement  stmt = conn.createStatement()) {
				String sql = "SELECT * FROM AKRAINO.HARDWARE";
				do {
					try (ResultSet rs = stmt.executeQuery(sql)) {
						while (rs.next() ) {
							String uuid = rs.getString("uuid");
							String name = rs.getString("name");
							String description = rs.getString("description");
							String yaml = rs.getString("yaml");
							Hardware h = new Hardware(uuid, name, description, yaml);
							list.add(h);
						}
					}
				} while (stmt.getMoreResults() || stmt.getUpdateCount() != -1);
			}
		} catch (SQLException e) {
			logger.error(e);
		} finally {
			releaseConnection(conn);
		}
		return list;
	}

	@Override
	public void deleteHardware(Hardware h) throws SQLException {
		Connection conn = null;
		try {
			conn = getConnection();
			String sql = "DELETE FROM AKRAINO.HARDWARE WHERE uuid = ?";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setString(1, h.getUuid());
				stmt.execute();
			}
		} catch (SQLException ex) {
			logger.error(ex);
			throw ex;
		} finally {
			releaseConnection(conn);
		}
	}

	// NODES ---------------------------------------------------------------------------------------------------------
	@Override
	public void createNode(final Node n) throws SQLException {
		Connection conn = null;
		try {
			conn = getConnection();
			String sql = "INSERT INTO AKRAINO.NODE (uuid, name, description, hardware, yaml) VALUES(?, ?, ?, ?, ?)";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setString(1, n.getUuid());
				stmt.setString(2, n.getName());
				stmt.setString(3, n.getDescription());
				stmt.setString(4, n.getHardware());
				stmt.setString(5, n.getYaml());
				stmt.execute();
			}
		} catch (SQLException e) {
			logger.error(e);
			throw e;
		} finally {
			releaseConnection(conn);
		}
	}

	@Override
	public List<Node> getNodes() {
		List<Node> list = new ArrayList<>();
		Connection conn = null;
		try {
			conn = getConnection();
			try (Statement  stmt = conn.createStatement()) {
				String sql = "SELECT * FROM AKRAINO.NODE";
				do {
					try (ResultSet rs = stmt.executeQuery(sql)) {
						while (rs.next() ) {
							String uuid = rs.getString("uuid");
							String name = rs.getString("name");
							String description = rs.getString("description");
							String hw   = rs.getString("hardware");
							String yaml = rs.getString("yaml");
							Node n = new Node(uuid, name, description, hw, yaml);
							list.add(n);
						}
					}
				} while (stmt.getMoreResults() || stmt.getUpdateCount() != -1);
			}
		} catch (SQLException e) {
			logger.error(e);
		} finally {
			releaseConnection(conn);
		}
		return list;
	}

	@Override
	public void deleteNode(final Node n) throws SQLException {
		Connection conn = null;
		try {
			conn = getConnection();
			String sql = "DELETE FROM AKRAINO.NODE WHERE uuid = ?";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setString(1, n.getUuid());
				stmt.execute();
			}
		} catch (SQLException ex) {
			logger.error(ex);
			throw ex;
		} finally {
			releaseConnection(conn);
	}
	}

	// PODS ---------------------------------------------------------------------------------------------------------
	@Override
	public void createPod(final POD p) throws SQLException {
		Connection conn = null;
		try {
			conn = getConnection();
			String sql = "INSERT INTO AKRAINO.POD (uuid, name, description, state, bp_uuid, es_uuid, yaml) VALUES(?, ?, ?, ?, ?, ?, ?)";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setString(1, p.getUuid());
				stmt.setString(2, p.getName());
				stmt.setString(3, p.getDescription());
				stmt.setString(4, p.getState().toString());
				stmt.setString(5, p.getBlueprint());
				stmt.setString(6, p.getEdgesite());
				stmt.setString(7, p.getYaml());
				stmt.execute();
			}
		} catch (SQLException e) {
			logger.error(e);
			throw e;
		} finally {
			releaseConnection(conn);
		}
	}

	@Override
	public List<POD> getPods() {
		List<POD> list = new ArrayList<>();
		Connection conn = null;
		try {
			conn = getConnection();
			try (Statement  stmt = conn.createStatement()) {
				String sql = "SELECT * FROM AKRAINO.POD";
				do {
					try (ResultSet rs = stmt.executeQuery(sql)) {
						while (rs.next() ) {
							String uuid        = rs.getString("uuid");
							String name        = rs.getString("name");
							String description = rs.getString("description");
							String state       = rs.getString("state");
							String blueprint   = rs.getString("bp_uuid");
							String edgesite    = rs.getString("es_uuid");
							String yaml        = rs.getString("yaml");
							POD p = new POD(uuid, name, description, state, blueprint, edgesite, yaml);
							list.add(p);
						}
					}
				} while (stmt.getMoreResults() || stmt.getUpdateCount() != -1);
			}
		} catch (SQLException e) {
			logger.error(e);
		} finally {
			releaseConnection(conn);
		}
		return list;
	}

	@Override
	public void updatePod(final POD p) throws SQLException {
		Connection conn = null;
		try {
			conn = getConnection();
			String sql = "UPDATE AKRAINO.POD SET state = ?, yaml = ?, es_uuid = ? WHERE uuid = ?";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setString(1, p.getState().toString());
				stmt.setString(2, p.getYaml());
				stmt.setString(3, p.getEdgesite());
				stmt.setString(4, p.getUuid());
				stmt.execute();
			}
		} catch (SQLException e) {
			logger.error(e);
			throw e;
		} finally {
			releaseConnection(conn);
		}
	}

	@Override
	public void deletePod(final POD p) throws SQLException {
		Connection conn = null;
		try {
			conn = getConnection();
			String sql = "DELETE FROM AKRAINO.POD WHERE uuid = ?";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setString(1, p.getUuid());
				stmt.execute();
			}
			sql = "DELETE FROM AKRAINO.POD_EVENTS WHERE uuid = ?";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setString(1, p.getUuid());
				stmt.execute();
			}
		} catch (SQLException ex) {
			logger.error(ex);
			throw ex;
		} finally {
			releaseConnection(conn);
		}
	}

	// PODS EVENTS ---------------------------------------------------------------------------------------------------------
	public void createPodEvent(final PODEvent pe) throws SQLException {
		Connection conn = null;
		try {
			conn = getConnection();
			String sql = "INSERT INTO AKRAINO.POD_EVENTS (uuid, eventtime, level, eventmsg) VALUES (?, ?, ?, ?)";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setString(1, pe.getUuid());
				stmt.setTimestamp(2, pe.getEventtime());
				stmt.setString(3, pe.getLevel());
				stmt.setString(4, pe.getMessage());
				stmt.execute();
			}
		} catch (SQLException e) {
			logger.error(e);
			throw e;
		} finally {
			releaseConnection(conn);
		}
	}

	public List<PODEvent> getPODEvents(final String uuid) {
		List<PODEvent> list = new ArrayList<>();
		Connection conn = null;
		try {
			conn = getConnection();
			String sql = "SELECT * FROM AKRAINO.POD_EVENTS WHERE UUID = ?";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setString(1, uuid);
				do {
					try (ResultSet rs = stmt.executeQuery()) {
						while (rs.next() ) {
							Timestamp time = rs.getTimestamp("eventtime");
							String level   = rs.getString("level");
							String msg     = rs.getString("eventmsg");
							PODEvent pe = new PODEvent(uuid, time, level, msg);
							list.add(pe);
						}
					}
				} while (stmt.getMoreResults() || stmt.getUpdateCount() != -1);
			}
		} catch (SQLException e) {
			logger.error(e);
		} finally {
			releaseConnection(conn);
		}
		return list;
	}

	// REGIONS ---------------------------------------------------------------------------------------------------------
	@Override
	public void createRegion(final Region r) throws SQLException {
		Connection conn = null;
		try {
			conn = getConnection();
			String sql = "INSERT INTO AKRAINO.REGION (uuid, name, description, parent) VALUES(?, ?, ?, ?)";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setString(1, r.getUuid());
				stmt.setString(2, r.getName());
				stmt.setString(3, r.getDescription());
				stmt.setString(4, r.getParent());
				stmt.execute();
			}
		} catch (SQLException e) {
			logger.error(e);
			throw e;
		} finally {
			releaseConnection(conn);
		}
	}

	@Override
	public List<Region> getRegions() {
		List<Region> list = new ArrayList<>();
		Connection conn = null;
		try {
			conn = getConnection();
			try (Statement  stmt = conn.createStatement()) {
				String sql = "SELECT * FROM AKRAINO.REGION";
				do {
					try (ResultSet rs = stmt.executeQuery(sql)) {
						while (rs.next() ) {
							String description = rs.getString("description");
							String parent      = rs.getString("parent");
							Region r = new Region(
								rs.getString("uuid"),
								rs.getString("name"),
								(description == null) ? "" : description,
								(parent == null) ? Region.UNIVERSAL_REGION : parent
							);
							list.add(r);
						}
					}
				} while (stmt.getMoreResults() || stmt.getUpdateCount() != -1);
			}
		} catch (SQLException e) {
			logger.error(e);
		} finally {
			releaseConnection(conn);
		}
		return list;
	}

	@Override
	public void deleteRegion(Region r) throws SQLException {
		Connection conn = null;
		try {
			conn = getConnection();
			String sql = "DELETE FROM AKRAINO.REGION WHERE uuid = ?";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setString(1, r.getUuid());
				stmt.execute();
			}
		} catch (SQLException ex) {
			logger.error(ex);
			throw ex;
		} finally {
			releaseConnection(conn);
		}
	}

	// SESSIONS ---------------------------------------------------------------------------------------------------------
	@Override
	public void createSession(UserSession us) {
		Connection conn = null;
		try {
			conn = getConnection();
			String sql = "INSERT INTO AKRAINO.SESSIONS (userid, cookie, expires) VALUES (?, ?, ?)";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setString(1, us.getUser().getUuid());
				stmt.setString(2, us.getToken());
				stmt.setLong(3, us.getExpires());
				stmt.execute();
			}
			// Remove old expired sessions from the table
			sql = "DELETE FROM AKRAINO.SESSIONS WHERE expires < ?";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setLong(1, System.currentTimeMillis());
				stmt.execute();
			}
		} catch (SQLException e) {
			logger.error(e);
		} finally {
			releaseConnection(conn);
		}
	}

	@Override
	public UserSession getSession(String cookie) {
		Connection conn = null;
		try {
			conn = getConnection();
			String sql = "SELECT * FROM AKRAINO.SESSIONS WHERE COOKIE = ?";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setString(1, cookie);
				try (ResultSet rs = stmt.executeQuery()) {
					if (rs.next() ) {
						long expires = rs.getLong("expires");
						User u = getUserByUuid(rs.getString("userid"));
						UserSession us = new UserSession(u, cookie, expires);
						return us;
					}
				}
				stmt.close();
			}
		} catch (SQLException e) {
			logger.error(e);
		} finally {
			releaseConnection(conn);
		}
		return null;
	}

	@Override
	public void invalidateSession(UserSession us) {
		Connection conn = null;
		try {
			conn = getConnection();
			String sql = "DELETE FROM AKRAINO.SESSIONS WHERE cookie = ?";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setString(1, us.getToken());
				stmt.execute();
			}
		} catch (SQLException e) {
			logger.error(e);
		} finally {
			releaseConnection(conn);
		}
	}

	// USERS ---------------------------------------------------------------------------------------------------------
	@Override
	public User getUser(String name) {
		return getUserCommon("SELECT * FROM AKRAINO.USERS WHERE name = ?", name);
	}

	protected User getUserByUuid(String uuid) {
		return getUserCommon("SELECT * FROM AKRAINO.USERS WHERE uuid = ?", uuid);
	}

	private User getUserCommon(String sql, String value) {
		Connection conn = null;
		try {
			conn = getConnection();
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setString(1, value);
				try (ResultSet rs = stmt.executeQuery()) {
					if (rs.next() ) {
						String description = rs.getString("description");
						String uuid = rs.getString("uuid");
						String name = rs.getString("name");
						String password = rs.getString("pwhash");
						User u = new User(uuid, name, password, (description == null) ? "" : description);
						u.setRoles(getRolesForUser(uuid));
						return u;
					}
				}
			}
		} catch (SQLException e) {
			logger.error(e);
		} finally {
			releaseConnection(conn);
		}
		return null;
	}

	private Set<Role> getRolesForUser(final String uuid) {
		Set<Role> set = new TreeSet<>();
		Connection conn = null;
		try {
			conn = getConnection();
			String sql = "SELECT * FROM AKRAINO.USER_ROLES WHERE USER_UUID = ?";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setString(1, uuid);
				do {
					try (ResultSet rs = stmt.executeQuery()) {
						while (rs.next() ) {
							String role_uuid = rs.getString("role_uuid");
							Role r = getRoleByUuid(role_uuid);
							if (r != null) {
								set.add(r);
							} else {
								logger.warn("Invalid role uuid = "+role_uuid);
							}
						}
					}
				} while (stmt.getMoreResults() || stmt.getUpdateCount() != -1);
			}
		} catch (SQLException e) {
			logger.error(e);
		} finally {
			releaseConnection(conn);
		}
		return set;
	}
	private Role getRoleByUuid(String role_uuid) {
		Connection conn = null;
		try {
			conn = getConnection();
			String sql = "SELECT * FROM AKRAINO.ROLES WHERE UUID = ?";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setString(1, role_uuid);
				do {
					try (ResultSet rs = stmt.executeQuery()) {
						if (rs.next() ) {
							String description = rs.getString("description");
							description = (description == null) ? "" : description;
							String uuid = rs.getString("uuid");
							String name = rs.getString("name");
							Set<String> attr = getRoleAttributes(uuid);
							Role r = new Role(uuid, name, description, attr.toArray(new String[0]));
							return r;
						}
					}
				} while (stmt.getMoreResults() || stmt.getUpdateCount() != -1);
			}
		} catch (SQLException e) {
			logger.error(e);
		} finally {
			releaseConnection(conn);
		}
		return null;
	}
	private Set<String> getRoleAttributes(String uuid) {
		Set<String> set = new TreeSet<>();
		Connection conn = null;
		try {
			conn = getConnection();
			String sql = "SELECT * FROM AKRAINO.ROLE_ATTRIBUTES WHERE ROLE_UUID = ?";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setString(1, uuid);
				do {
					try (ResultSet rs = stmt.executeQuery()) {
						while (rs.next() ) {
							String attr = rs.getString("attribute");
							set.add(attr);
						}
					}
				} while (stmt.getMoreResults() || stmt.getUpdateCount() != -1);
			}
		} catch (SQLException e) {
			logger.error(e);
		} finally {
			releaseConnection(conn);
		}
		return set;
	}
}
