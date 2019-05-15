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

package org.akraino.regional_controller.api.v1;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.core.MediaType;

import org.akraino.regional_controller.beans.Role;
import org.akraino.regional_controller.beans.User;
import org.akraino.regional_controller.beans.UserSession;
import org.akraino.regional_controller.db.DB;
import org.akraino.regional_controller.db.DBFactory;
import org.akraino.regional_controller.utils.YAMLtoJSON;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

public class APIBase {
	/** There is no official MIME type for YAML, so this is what we will use */
	public static final String APPLICATION_YAML  = "application/yaml";

	/** The name of the HTTP header where the Session token is located */
	public static final String SESSION_TOKEN_HDR = "X-ARC-Token";

	/** The name of the HTTP header (passed from NGiNX) where the real IP address of the client is located */
	public static final String REAL_IP_HDR       = "X-Real-IP";

	/** The normal Content-Type header */
	public static final String CONTENT_TYPE_HDR  = "Content-type";

	protected static final Logger logger     = LogManager.getLogger();
	protected static final Logger api_logger = LogManager.getLogger("ApiLogger");

	protected APIBase() {
		// nothing
	}

	protected User checkToken(String token, String method, String realIp) {
		if (token == null || "".equals(token)) {
			api_logger.info("{} user {}, realip {} => 401", method, null, realIp);
			throw new NotAuthorizedException("not authorized");
		}
		DB db = DBFactory.getDB();
		UserSession us = db.getSession(token);
		if (us == null) {
			api_logger.info("{} user {}, realip {} => 401", method, null, realIp);
			throw new NotAuthorizedException("not authorized");
		}
		if (!us.isValid()) {
			us.invalidate();
			api_logger.info("{} user {}, realip {} => 401", method, null, realIp);
			throw new NotAuthorizedException("session expired");
		}
		return us.getUser();
	}

	/**
	 * Check if a user is allowed access to one of a number of role attributes
	 * @param u the User
	 * @param requiredRoles the list of role attributes to check against
	 * @throws ForbiddenException if the user is not allowed the role
	 */
	protected void checkRBAC(User u, String[] requiredRoles, String method, String realIp) {
		for (Role ur : u.getRoles()) {
			for (String rr : requiredRoles) {
				if (ur.getAttributes().contains(rr)) {
					return;
				}
			}
		}
		api_logger.info("{} user {}, realip {} => 401", method, null, realIp);
		throw new ForbiddenException("RBAC does not allow");
	}

	/**
	 * Get content (in either JSON or YAML form), and convert to a JSONObject.
	 * @param ctype the Content-Type header
	 * @param content the content itself
	 * @return a JSONObject of the content
	 * @throws ClientErrorException a 415 response if the content-type header is not acceptable
	 */
	protected JSONObject getContent(String ctype, String content) {
		if (ctype.equalsIgnoreCase(APPLICATION_YAML)) {
			logger.info("get YAML; content is {}", content);
			YAMLtoJSON y = new YAMLtoJSON(content);
			JSONObject rv = y.toJSON();
			logger.info("get YAML; converted to {}", rv.toString());
			return rv;
		}
		if (ctype.equalsIgnoreCase(MediaType.APPLICATION_JSON)) {
			return new JSONObject(content);
		}
		throw new ClientErrorException(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
	}
}
