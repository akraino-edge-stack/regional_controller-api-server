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
import java.util.Set;
import java.util.TreeSet;

import org.akraino.regional_controller.db.DB;
import org.akraino.regional_controller.db.DBFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

public class User extends BaseBean {
	public static User getUser(String name, String password) {
		DB db = DBFactory.getDB();
		User u = db.getUser(name);
		Logger logger = LogManager.getLogger();
		if (u == null || password == null) {
			logger.info("No user with name: "+name);
			return null;
		}
		String hash2 = sha256Hex(password);
		if (!u.getPasswordHash().equals(hash2)) {
			logger.info("Password hash does not match: "+name);
			return null;
		}
		return u;
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

	private final String pwhash;
	private       Set<Role> roles;

	public User(String uuid, String name, String pwhash, String description) {
		super(uuid, name, description);
		this.pwhash = pwhash;
		this.roles = new TreeSet<>();
	}

	public String getPasswordHash() {
		return pwhash;
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
