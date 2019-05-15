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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.akraino.regional_controller.beans.User;
import org.akraino.regional_controller.beans.UserSession;
import org.akraino.regional_controller.db.DBFactory;
import org.akraino.regional_controller.utils.JSONtoYAML;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * The Login API supports
 * - POST: to create a new login
 * - GET: to retrieve login information (such as a list of roles for RBAC)
 * - DELETE: to logout the user
 */
@Path(LoginAPI.LOGIN_PATH)
public class LoginAPI extends APIBase {
	public  static final String LOGIN_PATH   = "login";

	private final int max_age;

	public LoginAPI() {
		// Initialize some things from properties
		Properties props = new Properties();
		InputStream is = null;
		try {
			is = getClass().getClassLoader().getResourceAsStream(DBFactory.PROPERTIES_FILE);
			props.load(is);
		} catch (IOException e) {
			logger.error("Cannot read api.properties!");
		}
		this.max_age = Integer.valueOf(props.getProperty("api.maxage", "3600"));	// default = 1 hour
		logger.info("LoginAPI: max_age = {}", this.max_age);
	}

	/**
	 * Login a user.
	 * @param json the JSON content provided by the API client
	 */
	@POST
	@Consumes({APPLICATION_YAML, MediaType.APPLICATION_JSON})
	public Response loginUser(
		@HeaderParam(REAL_IP_HDR)      final String realIp,
		@HeaderParam(CONTENT_TYPE_HDR) final String ctype,
		String content
	) {
		String method = "POST /api/v1/login";
		try {
			JSONObject jo = getContent(ctype, content);
			String username = jo.getString("name");
			String password = jo.getString("password");
			// look user up in LDAP
			// verify
			User u = User.getUser(username, password);
			if (u == null) {
				api_logger.info("{} user {}, realip {} => 404", method, "-", realIp);
				throw new NotFoundException("not found.");
			}
			UserSession us = new UserSession(u, max_age, realIp);	// creates the token
			api_logger.info("{} user {}, realip {} => 201", method, u.getName(), realIp);
			return Response
				.created(new URI("/api/v1/login/"))
				.header(SESSION_TOKEN_HDR, us.getToken())
				.build();
		} catch (JSONException | URISyntaxException e) {
			logger.warn(e.toString());
			api_logger.info("{} user {}, realip {} => 400", method, "-", realIp);
			throw new BadRequestException(e.toString());
		}
	}

	/**
	 * Get the User Session information for a user, in JSON form.
	 * @param token the users login token
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public String getLoginRolesJSON(
		@HeaderParam(SESSION_TOKEN_HDR) String token,
		@HeaderParam(REAL_IP_HDR) String realIp
	) {
		UserSession us = getLoginRolesCommon(token, realIp);
		return us.toJSON().toString();
	}

	/**
	 * Get the User Session information for a user, in YAML form.
	 * @param token the users login token
	 */
	@GET
	@Produces(APPLICATION_YAML)
	public String getLoginRolesYAML(
		@HeaderParam(SESSION_TOKEN_HDR) String token,
		@HeaderParam(REAL_IP_HDR) String realIp
	) {
		UserSession us = getLoginRolesCommon(token, realIp);
		return new JSONtoYAML(us.toJSON()).toString();
	}

	private UserSession getLoginRolesCommon(String token, String realIp) {
		logger.info("GET /login token is {}", token);
		UserSession us = UserSession.getSession(token);
		if (us == null) {
			String m = "No login for token: "+token;
			logger.warn(m);
			api_logger.info("GET /api/v1/login user {}, realip {} => 401", "-", realIp);
			throw new NotAuthorizedException(m);
		}
		api_logger.info("GET /api/v1/login user {}, realip {} => 200", us.getUser().getName(), realIp);
		return us;
	}

	/**
	 * Logout a user.  Invalidates the UserSession indicated by the token
	 * @param token the users login token
	 * @param realIp the users IP address (passed form NGiNX)
	 * @return an HTTP response to use
	 */
	@DELETE
	public Response logoutUser(
		@HeaderParam(SESSION_TOKEN_HDR) String token,
		@HeaderParam(REAL_IP_HDR) String realIp
	) {
		logger.info("Delete session token {}", token);
		UserSession us = UserSession.getSession(token);
		if (us == null) {
			String m = "No login for token: "+token;
			logger.warn(m);
			api_logger.info("DELETE /api/v1/login user {}, realip {} => 401", "-", realIp);
			throw new NotAuthorizedException(m);
		}
		us.invalidate();
		api_logger.info("DELETE /api/v1/login user {}, realip {} => 200", us.getUser().getName(), realIp);
		return Response.ok().build();
	}
}
