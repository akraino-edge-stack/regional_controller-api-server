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

import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.WebApplicationException;

import org.akraino.regional_controller.db.DB;
import org.akraino.regional_controller.db.DBFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

public class PODEvent {
	public static PODEvent createPodEvent(JSONObject json) throws WebApplicationException {
		String u = json.optString("uuid");
		if (u == null || "".equals(u))
			throw new BadRequestException("Missing UUID");
		String t = json.optString("time");
		String l = json.optString("level");
		if (l == null || "".equals(l))
			throw new BadRequestException("Missing level");
		String m = json.optString("message");
		if (m == null || "".equals(m))
			throw new BadRequestException("Missing message");
		POD pod = POD.getPodByUUID(u);
		if (pod == null)
			throw new BadRequestException("No POD with UUID="+u);

		try {
			Timestamp ts;
			if (t == null || "".equals(t)) {
				// No time provided - use the current time
				ts = new Timestamp(new Date().getTime());
			} else {
				SimpleDateFormat fmt1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				Date date1 = fmt1.parse(t);
				ts = new Timestamp(date1.getTime());
			}
			PODEvent pe = new PODEvent(u, ts, l, m);
			pe.writeEvent();
			return pe;
		} catch (ParseException e) {
			throw new BadRequestException("Invalid timestamp: "+e);
		}
	}

	private final Timestamp eventtime;
	private final String uuid;
	private final String level;
	private final String message;

	public PODEvent(String uuid, String level, String msg) {
		this(uuid, new Timestamp(System.currentTimeMillis()), level, msg);
	}

	public PODEvent(String uuid, Timestamp time, String level, String msg) {
		this.uuid = uuid;
		this.eventtime = time;
		this.level = level;
		this.message = msg;
	}

	public Timestamp getEventtime() {
		return eventtime;
	}

	public String getUuid() {
		return uuid;
	}

	public String getLevel() {
		return level;
	}

	public String getMessage() {
		return message;
	}

	public void writeEvent() {
		Logger logger = LogManager.getLogger();
		try {
			DB db = DBFactory.getDB();
			db.createPodEvent(this);

			// If the PodEvent has a level of STATUS, this will change state of the POD
			if (level.equals("STATUS")) {
				String s = message;
				int ix = s.indexOf(':');
				if (ix >= 0) {
					s = s.substring(ix+1);
				}
				POD pod = POD.getPodByUUID(uuid);
				if (pod == null) {
					logger.warn("Internal error: cannot find POD "+uuid);
				} else {
					s = s.trim();
					try {
						POD.State newstate = POD.State.valueOf(s);
						pod.setState(newstate);	// This will update the POD in the DB
					} catch (IllegalArgumentException e) {
						logger.warn("Workflow error: invalid state "+s);
					}
				}
			}
		} catch (SQLException e) {
			logger.warn(e);
		}
	}

	public JSONObject toJSON() {
		JSONObject jo = new JSONObject();
		jo.put("time", eventtime.toString());
		jo.put("level", level);
		jo.put("message", message);
		return jo;
	}
}
