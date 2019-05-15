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

import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;

import org.akraino.regional_controller.db.DB;
import org.akraino.regional_controller.db.DBFactory;
import org.json.JSONObject;

public class UserSession {
	private final User user;
	private final String token;
	private       long expires;

	public static UserSession getSession(String token) {
		DB db = DBFactory.getDB();
		UserSession us = db.getSession(token);
		return (us != null && us.isValid()) ? us : null;
	}

	/**
	 * This form of the constructor is used to build UserSession beans from the database.
	 * @param user the User this session refers to
	 * @param token the session token
	 * @param expires when the session expires
	 */
	public UserSession(User user, String token, long expires) {
		this.user    = user;
		this.token   = token;
		this.expires = expires;
	}

	/**
	 * This form of the constructor will create a NEW UserSession in the database.
	 * @param user the User requesting a new UserSession
	 * @param age the number of seconds until this session expires
	 * @param realIp the IP address of the requester
	 */
	public UserSession(User user, long age, String realIp) {
		if (realIp == null)
			realIp = "0.0.0.0";
		this.user    = user;
		this.token   = generateToken(user.getName(), realIp);
		this.expires = System.currentTimeMillis() + (age * 1000L);

		DB db = DBFactory.getDB();
		db.createSession(this);
	}

	// <12 bytes user name><14 bytes session time><8 bytes IP addr>
	private String generateToken(String name, String ip) {
		String[] ipp = ip.split("\\.");
		String t = String.format("%-12.12s%14d%02x%02x%02x%02x",
			name, System.currentTimeMillis(),
			Integer.parseInt(ipp[0]),
			Integer.parseInt(ipp[1]),
			Integer.parseInt(ipp[2]),
			Integer.parseInt(ipp[3])
		);
		byte[] x = Base64.getEncoder().encode(t.getBytes());
		return new String(x);
	}

	public User getUser() {
		return user;
	}

	public String getToken() {
		return token;
	}

	public long getExpires() {
		return expires;
	}

	public boolean isValid() {
		return System.currentTimeMillis() < expires;
	}

	public void invalidate() {
		DB db = DBFactory.getDB();
		db.invalidateSession(this);
		expires = 0;
	}

	public JSONObject toJSON() {
		SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String fmttime = fmt.format(new Date(expires));
		JSONObject jo = new JSONObject();
		jo.put("user", user.toJSON());
		jo.put("token", token);
		jo.put("expires", expires);
		jo.put("expiration_time", fmttime);
		return jo;
	}
}
