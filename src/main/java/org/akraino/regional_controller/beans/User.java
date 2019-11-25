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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

public class User extends BaseBean {
	public static final String CREDENTIALS_TAG = "password";
	public static final String ROLES_TAG    = "roles";

	// 22 alphanumeric chars = 128 bits of entropy
	// https://en.wikipedia.org/wiki/Password_strength
	public static final String HI_STRENGTH_RE = "[a-zA-Z0-9 -]{22,}";

	public static String createUser(JSONObject json, User creating_user) throws WebApplicationException {
		Logger logger = LogManager.getLogger();
		String n = json.optString(NAME_TAG);
		if (n == null || "".equals(n)) {
			logger.warn("Missing name");
			throw new BadRequestException("ARC-1013: Missing name");
		}
		String d = json.optString(DESCRIPTION_TAG);
		if (d == null) {
			d = "";
		}
		String pw = json.optString(CREDENTIALS_TAG);
		if (pw == null || "".equals(pw)) {
			logger.warn("Missing version");
			throw new BadRequestException("ARC-1014: Missing password");
		}
		if (! pw.matches(HI_STRENGTH_RE)) {
			String m = "Password is not strong enough; must match the regex: "+HI_STRENGTH_RE;
			logger.warn(m);
			throw new BadRequestException("ARC-1032: "+m);
		}
		JSONArray roles = json.optJSONArray(ROLES_TAG);
		Set<Role> newroles = null;
		if (roles == null) {
			// No roles specified, so use creating user's roles
			newroles = creating_user.getRoles();
		} else {
			// Verify passed in role list is a subset of the roles of the creating user
			newroles = new TreeSet<>();
			Set<Role> croles = creating_user.getRoles();
			for (int i = 0; i < roles.length(); i++) {
				Role r = roleInSet(roles.getString(i), croles);
				if (r != null) {
					newroles.add(r);
				} else {
					logger.warn("The requesting user does not possess the role: "+r);
					throw new BadRequestException("ARC-1023: The requesting user does not possess the role: "+r);
				}
			}
		}

		// Find a new, unused UUID
		UUID u;
		do {
			u = UUID.randomUUID();
		} while (getUserByUUID(u.toString()) != null);
		String uuid = u.toString();
		User user = new User(uuid, n, sha256Hex(pw), d);
		user.setRoles(newroles);

		try {
			DB db = DBFactory.getDB();
			db.createUser(user);
			return uuid;
		} catch (SQLException e1) {
			throw new InternalServerErrorException("ARC-4003: "+e1.getMessage());
		}
	}

	public static User getUser(String name, String password) {
		DB db = DBFactory.getDB();
		User u = db.getUser(name);
		Logger logger = LogManager.getLogger();
		if (u == null || password == null) {
			logger.info("No user with name: "+name);
			return null;
		}
		String hash2 = sha256Hex(password);
		if (!u.getPasswordHash().equalsIgnoreCase(hash2)) {
			logger.info("Password hash does not match: "+name);
			return null;
		}
		return u;
	}

	public static Collection<User> getUsers() {
		Map<String, User> map = pullFromDB();
		return map.values();
	}

	public static User getUserByUUID(final String uuid) {
		Map<String, User> map = pullFromDB();
		return map.get(uuid);
	}

	public void updateUser() throws WebApplicationException {
		try {
			DB db = DBFactory.getDB();
			db.updateUser(this);
		} catch (SQLException e1) {
			throw new InternalServerErrorException("ARC-4003: "+e1.getMessage());
		}
	}

	private static Map<String, User> pullFromDB() {
		Map<String, User> map = new HashMap<>();
		DB db = DBFactory.getDB();
		List<User> list = db.getUsers();
		for (User b : list) {
			map.put(b.getUuid(), b);
		}
		return map;
	}

	public static Role roleInSet(final String name, Set<Role> roles) {
		for (Role xr : roles) {
			if (xr.getName().equals(name))
				return xr;
			if (xr.getUuid().equals(name))
				return xr;
		}
		return null;
	}

	private static String sha256Hex(String arg) {
		StringBuffer hexString = new StringBuffer();
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] encodedhash = digest.digest(arg.getBytes(StandardCharsets.UTF_8));
			for (byte by : encodedhash) {
				String hex = Integer.toHexString(0xff & by);
				if (hex.length() == 1)
					hexString.append('0');
			    hexString.append(hex);
			}
		} catch (NoSuchAlgorithmException e) {
			Logger logger = LogManager.getLogger();
			logger.error(e);
		}
		return hexString.toString().toUpperCase();
	}

	private String pwhash;
	private Set<Role> roles;

	public User(String uuid, String name, String pwhash, String description) {
		super(uuid, name, description);
		this.pwhash = pwhash;
		this.roles = new TreeSet<>();
	}

	public String getPasswordHash() {
		return pwhash;
	}

	public void setPassword(String pw) {
		this.pwhash = sha256Hex(pw);
	}

	public Set<Role> getRoles() {
		return roles;
	}

	public void setRoles(Set<Role> r) {
		roles = r;
	}

	@Override
	public JSONObject toJSON() {
		JSONObject jo = super.toJSON();
		// don't display password
		if (roles != null) {
			JSONArray ja = new JSONArray();
			for (Role r : roles) {
				ja.put(r.toJSON());
			}
			jo.put("roles", ja);
		}
		return jo;
	}

	public static void main(String[] args) throws NoSuchAlgorithmException {
		// Utility function to compute SHA-256 hashes for passwords (to store in the DB)
		for (String a : args) {
			System.out.println(a + " ==> " + sha256Hex(a));
		}
	}
}
