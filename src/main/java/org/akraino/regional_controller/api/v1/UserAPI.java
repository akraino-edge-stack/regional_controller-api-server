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

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.Set;
import java.util.TreeSet;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.akraino.regional_controller.beans.Role;
import org.akraino.regional_controller.beans.User;
import org.akraino.regional_controller.db.DB;
import org.akraino.regional_controller.db.DBFactory;
import org.akraino.regional_controller.utils.JSONtoYAML;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@Path(UserAPI.USER_PATH)
public class UserAPI extends APIBase {
	public static final String USER_PATH = "user";

	protected static final String[] USER_CREATE_RBAC = { "create-*", "create-user" };
	protected static final String[] USER_READ_RBAC   = { "read-*",   "read-user" };
	protected static final String[] USER_UPDATE_RBAC = { "update-*", "update-user" };
	protected static final String[] USER_DELETE_RBAC = { "delete-*", "delete-user" };

	@POST
	@Consumes({APPLICATION_YAML, MediaType.APPLICATION_JSON})
	public Response createUser(
		@HeaderParam(SESSION_TOKEN_HDR) final String token,
		@HeaderParam(REAL_IP_HDR)       final String realIp,
		@HeaderParam(CONTENT_TYPE_HDR)  final String ctype,
		String content)
	{
		String method = "POST /api/v1/user";
		User u = checkToken(token, method, realIp);
		checkRBAC(u, USER_CREATE_RBAC, method, realIp);

		try {
			JSONObject jo = getContent(ctype, content);
			String uuid = User.createUser(jo, u);
			String url = buildUrl(uuid);
			return Response.created(new URI(url)).build();
		} catch (URISyntaxException e) {
			logger.warn(e.toString());
			throw new BadRequestException("ARC-1030: "+e.toString());
		}
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public String getUsersJSON(
		@HeaderParam(SESSION_TOKEN_HDR) String token,
		@HeaderParam(REAL_IP_HDR) String realIp
	) {
		JSONObject jo = getUsersCommon(token, realIp);
		return jo.toString();
	}

	@GET
	@Produces(APPLICATION_YAML)
	public String getUsersYAML(
		@HeaderParam(SESSION_TOKEN_HDR) String token,
		@HeaderParam(REAL_IP_HDR) String realIp
	) {
		JSONObject jo = getUsersCommon(token, realIp);
		return new JSONtoYAML(jo).toString();
	}

	private JSONObject getUsersCommon(String token, String realIp) {
		String method = "GET /api/v1/user";
		User u = checkToken(token, method, realIp);
		checkRBAC(u, USER_READ_RBAC, method, realIp);

		JSONArray ja = new JSONArray();
		for (User bp : User.getUsers()) {
			JSONObject jo = bp.toJSON();
			jo.put("url", buildUrl(jo.getString("uuid")));
			ja.put(jo);
		}
		api_logger.info("{} user {}, realip {} => 200", method, u.getName(), realIp);
		JSONObject jo = new JSONObject();
		jo.put("users", ja);
		return jo;
	}

	@GET
	@Path("/{uuid}")
	@Produces(MediaType.APPLICATION_JSON)
	public String getUsersDetailsJSON(
		@HeaderParam(SESSION_TOKEN_HDR) String token,
		@HeaderParam(REAL_IP_HDR) String realIp,
		@PathParam("uuid") String uuid
	) {
		JSONObject jo = getUsersDetailsCommon(token, realIp, uuid);
		return jo.toString();
	}

	@GET
	@Path("/{uuid}")
	@Produces(APPLICATION_YAML)
	public String getUsersDetailsYAML(
		@HeaderParam(SESSION_TOKEN_HDR) String token,
		@HeaderParam(REAL_IP_HDR) String realIp,
		@PathParam("uuid") String uuid
	) {
		JSONObject jo = getUsersDetailsCommon(token, realIp, uuid);
		return new JSONtoYAML(jo).toString();
	}

	private JSONObject getUsersDetailsCommon(String token, String realIp, String uuid) {
		String method = "GET /api/v1/user/"+uuid;
		User u = checkToken(token, method, realIp);
		checkRBAC(u, USER_READ_RBAC, method, realIp);

		User bp = User.getUserByUUID(uuid);
		if (bp == null) {
			api_logger.info("{} user {}, realip {} => 404", method, u.getName(), realIp);
			throw new NotFoundException("ARC-4001: object not found");
		}
		api_logger.info("{} user {}, realip {} => 200", method, u.getName(), realIp);
		return bp.toJSON();
	}

	@PUT
	@Path("/{uuid}")
	@Consumes({APPLICATION_YAML, MediaType.APPLICATION_JSON})
	public Response putUsers(
		@HeaderParam(SESSION_TOKEN_HDR) String token,
		@HeaderParam(REAL_IP_HDR) String realIp,
		@HeaderParam(CONTENT_TYPE_HDR)  final String ctype,
		@PathParam("uuid") String uuid,
		String content
	) {
		String method = "PUT /api/v1/user/"+uuid;
		User u = checkToken(token, method, realIp);
		checkRBAC(u, USER_UPDATE_RBAC, method, realIp);

		User user = User.getUserByUUID(uuid);
		if (user == null) {
			api_logger.info("{} user {}, realip {} => 404", method, u.getName(), realIp);
			throw new NotFoundException("ARC-4001: object not found");
		}

		try {
			// Can only change the description and roles of the User
			JSONObject jo = getContent(ctype, content);
			Set<String> keys = jo.keySet();
			if (keys.contains(User.UUID_TAG)) {
				throw new ForbiddenException("ARC-3017: Not allowed to modify the User's UUID.");
			}
			if (keys.contains(User.NAME_TAG)) {
				throw new ForbiddenException("ARC-3018: Not allowed to modify the User's name.");
			}
			boolean doupdate = false;
			if (keys.contains(User.PASSWORD_TAG)) {
				String pw = jo.getString(User.PASSWORD_TAG);
				if (! pw.matches(User.HI_STRENGTH_RE)) {
					String m = "Password is not strong enough; must match the regex: "+User.HI_STRENGTH_RE;
					logger.warn(m);
					throw new BadRequestException("ARC-1030: "+m);
				}
				user.setPassword(pw);
				doupdate = true;
			}
			if (keys.contains(User.DESCRIPTION_TAG)) {
				String description = jo.getString(User.DESCRIPTION_TAG);
				if (!description.equals(user.getDescription())) {
					user.setDescription(description);
					doupdate = true;
				}
			}
			if (keys.contains(User.ROLES_TAG)) {
				JSONArray roles = jo.optJSONArray(User.ROLES_TAG);
				// Verify passed in role list is a subset of the roles of the creating user
				Set<Role> newroles = new TreeSet<>();
				Set<Role> croles = u.getRoles();
				for (int i = 0; i < roles.length(); i++) {
					Role r = User.roleInSet(roles.getString(i), croles);
					if (r != null) {
						newroles.add(r);
					} else {
						logger.warn("The requesting user does not possess the role: "+r);
						throw new BadRequestException("ARC-1023: The requesting user does not possess the role: "+r);
					}
				}
				user.setRoles(newroles);
				doupdate = true;
			}
			if (doupdate) {
				user.updateUser();
			}
			return Response.ok().build();
		} catch (JSONException e) {
			logger.warn(e.toString());
			throw new BadRequestException("ARC-1030: "+e.toString());
		}
	}

	@DELETE
	@Path("/{uuid}")
	public Response deleteUserByUUID(
		@HeaderParam(SESSION_TOKEN_HDR) String token,
		@HeaderParam(REAL_IP_HDR)       String realIp,
		@PathParam("uuid") String uuid)
	{
		String method = "DELETE /api/v1/user/"+uuid;
		User u = checkToken(token, method, realIp);
		checkRBAC(u, USER_DELETE_RBAC, method, realIp);

		if (uuid == null || "".equals(uuid)) {
			api_logger.info("{} user {}, realip {} => 400", method, u.getName(), realIp);
			throw new BadRequestException("ARC-1028: bad UUID");
		}
		User b = User.getUserByUUID(uuid);
		if (b == null) {
			api_logger.info("{} user {}, realip {} => 404", method, u.getName(), realIp);
			throw new NotFoundException("ARC-4001: object not found");
		}
		try {
			DB db = DBFactory.getDB();
			db.deleteUser(b);
			api_logger.info("{} user {}, realip {} => 204", method, u.getName(), realIp);
			return Response.noContent().build();
		} catch (SQLException e) {
			api_logger.info("{} user {}, realip {} => 500", method, u.getName(), realIp);
			return Response.serverError().build();
		}
	}

	private String buildUrl(String uuid) {
		return String.format("/api/v1/%s/%s", USER_PATH, uuid);
	}
}
