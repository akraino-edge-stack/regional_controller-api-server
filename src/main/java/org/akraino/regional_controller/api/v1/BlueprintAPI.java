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
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
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

import org.akraino.regional_controller.beans.Blueprint;
import org.akraino.regional_controller.beans.POD;
import org.akraino.regional_controller.beans.User;
import org.akraino.regional_controller.db.DB;
import org.akraino.regional_controller.db.DBFactory;
import org.akraino.regional_controller.utils.JSONtoYAML;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * The Blueprint API supports
 * - GET: to retrieve a list of Blueprints, or information about one Blueprint.
 */
@Path(BlueprintAPI.BLUEPRINT_PATH)
public class BlueprintAPI extends APIBase {
	public static final String BLUEPRINT_PATH = "blueprint";

	protected static final String[] BLUEPRINT_CREATE_RBAC = { "create-*", "create-blueprint" };
	protected static final String[] BLUEPRINT_READ_RBAC   = { "read-*",   "read-blueprint" };
	protected static final String[] BLUEPRINT_UPDATE_RBAC = { "update-*", "update-blueprint" };
	protected static final String[] BLUEPRINT_DELETE_RBAC = { "delete-*", "delete-blueprint" };

	@POST
	@Consumes({APPLICATION_YAML, MediaType.APPLICATION_JSON})
	public Response createBlueprint(
		@HeaderParam(SESSION_TOKEN_HDR) final String token,
		@HeaderParam(REAL_IP_HDR)       final String realIp,
		@HeaderParam(CONTENT_TYPE_HDR)  final String ctype,
		String content)
	{
		String method = "POST /api/v1/blueprint";
		User u = checkToken(token, method, realIp);
		checkRBAC(u, BLUEPRINT_CREATE_RBAC, method, realIp);

		try {
			JSONObject jo = getContent(ctype, content);
			String uuid = Blueprint.createBlueprint(jo);
			String url = buildUrl(uuid);
			return Response.created(new URI(url)).build();
		} catch (URISyntaxException e) {
			logger.warn(e.toString());
			throw new BadRequestException("ARC-1030: "+e.toString());
		}
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public String getBlueprintsJSON(
		@HeaderParam(SESSION_TOKEN_HDR) String token,
		@HeaderParam(REAL_IP_HDR) String realIp
	) {
		JSONObject jo = getBlueprintsCommon(token, realIp);
		return jo.toString();
	}

	@GET
	@Produces(APPLICATION_YAML)
	public String getBlueprintsYAML(
		@HeaderParam(SESSION_TOKEN_HDR) String token,
		@HeaderParam(REAL_IP_HDR) String realIp
	) {
		JSONObject jo = getBlueprintsCommon(token, realIp);
		return new JSONtoYAML(jo).toString();
	}

	private JSONObject getBlueprintsCommon(String token, String realIp) {
		String method = "GET /api/v1/blueprint";
		User u = checkToken(token, method, realIp);
		checkRBAC(u, BLUEPRINT_READ_RBAC, method, realIp);

		JSONArray ja = new JSONArray();
		for (Blueprint bp : Blueprint.getBlueprints()) {
			JSONObject jo = bp.toJSON();
			jo.put("url", buildUrl(jo.getString("uuid")));
			ja.put(jo);
		}
		api_logger.info("{} user {}, realip {} => 200", method, u.getName(), realIp);
		JSONObject jo = new JSONObject();
		jo.put("blueprints", ja);
		return jo;
	}

	@GET
	@Path("/{uuid}")
	@Produces(MediaType.APPLICATION_JSON)
	public String getBlueprintsDetailsJSON(
		@HeaderParam(SESSION_TOKEN_HDR) String token,
		@HeaderParam(REAL_IP_HDR) String realIp,
		@PathParam("uuid") String uuid
	) {
		JSONObject jo = getBlueprintsDetailsCommon(token, realIp, uuid);
		return jo.toString();
	}

	@GET
	@Path("/{uuid}")
	@Produces(APPLICATION_YAML)
	public String getBlueprintsDetailsYAML(
		@HeaderParam(SESSION_TOKEN_HDR) String token,
		@HeaderParam(REAL_IP_HDR) String realIp,
		@PathParam("uuid") String uuid
	) {
		JSONObject jo = getBlueprintsDetailsCommon(token, realIp, uuid);
		return new JSONtoYAML(jo).toString();
	}

	private JSONObject getBlueprintsDetailsCommon(String token, String realIp, String uuid) {
		String method = "GET /api/v1/blueprint/"+uuid;
		User u = checkToken(token, method, realIp);
		checkRBAC(u, BLUEPRINT_READ_RBAC, method, realIp);

		Blueprint bp = Blueprint.getBlueprintByUUID(uuid);
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
	public Response putBlueprints(
		@HeaderParam(SESSION_TOKEN_HDR) String token,
		@HeaderParam(REAL_IP_HDR) String realIp,
		@HeaderParam(CONTENT_TYPE_HDR)  final String ctype,
		@PathParam("uuid") String uuid,
		String content
	) {
		String method = "PUT /api/v1/blueprint/"+uuid;
		User u = checkToken(token, method, realIp);
		checkRBAC(u, BLUEPRINT_UPDATE_RBAC, method, realIp);

		Blueprint bp = Blueprint.getBlueprintByUUID(uuid);
		if (bp == null) {
			api_logger.info("{} user {}, realip {} => 404", method, u.getName(), realIp);
			throw new NotFoundException("ARC-4001: object not found");
		}

		try {
			// Can only change the description of the Blueprint
			JSONObject jo = getContent(ctype, content);
			Set<String> keys = jo.keySet();
			if (keys.contains(Blueprint.UUID_TAG)) {
				throw new ForbiddenException("ARC-3002: Not allowed to modify the Blueprint's UUID.");
			}
			if (keys.contains(Blueprint.NAME_TAG)) {
				throw new ForbiddenException("ARC-3004: Not allowed to modify the Blueprint's name.");
			}
			if (keys.contains(Blueprint.VERSION_TAG)) {
				throw new ForbiddenException("ARC-3005: Not allowed to modify the Blueprint's version.");
			}
			if (keys.contains(Blueprint.YAML_TAG)) {
				throw new ForbiddenException("ARC-3003: Not allowed to modify the Blueprint's YAML.");
			}
			if (keys.contains(Blueprint.DESCRIPTION_TAG)) {
				String description = jo.getString(Blueprint.DESCRIPTION_TAG);
				if (!description.equals(bp.getDescription())) {
					bp.setDescription(description);
					bp.updateBlueprint();
				}
			}
			return Response.ok().build();
		} catch (JSONException e) {
			logger.warn(e.toString());
			throw new BadRequestException("ARC-1030: "+e.toString());
		}
	}

	@DELETE
	@Path("/{uuid}")
	public Response deleteBlueprintByUUID(
		@HeaderParam(SESSION_TOKEN_HDR) String token,
		@HeaderParam(REAL_IP_HDR)       String realIp,
		@PathParam("uuid") String uuid)
	{
		String method = "DELETE /api/v1/blueprint/"+uuid;
		User u = checkToken(token, method, realIp);
		checkRBAC(u, BLUEPRINT_DELETE_RBAC, method, realIp);

		if (uuid == null || "".equals(uuid)) {
			api_logger.info("{} user {}, realip {} => 400", method, u.getName(), realIp);
			throw new BadRequestException("ARC-1028: bad UUID");
		}
		Blueprint b = Blueprint.getBlueprintByUUID(uuid);
		if (b == null) {
			api_logger.info("{} user {}, realip {} => 404", method, u.getName(), realIp);
			throw new NotFoundException("ARC-4001: object not found");
		}
		// Check if blueprint is in use (any PODs have blueprint == uuid), if so send a 409
		List<POD> list = b.getPODs();
		if (list != null && !list.isEmpty()) {
			POD p1 = list.get(0);
			api_logger.info("{} user {}, realip {} => 409", method, u.getName(), realIp);
			throw new ClientErrorException("ARC-2001: This Blueprint is still in use by POD "+p1.getUuid(), HttpServletResponse.SC_CONFLICT);
		}
		try {
			DB db = DBFactory.getDB();
			db.deleteBlueprint(b);
			api_logger.info("{} user {}, realip {} => 204", method, u.getName(), realIp);
			return Response.noContent().build();
		} catch (SQLException e) {
			api_logger.info("{} user {}, realip {} => 500", method, u.getName(), realIp);
			return Response.serverError().build();
		}
	}

	private String buildUrl(String uuid) {
		return String.format("/api/v1/%s/%s", BLUEPRINT_PATH, uuid);
	}
}
