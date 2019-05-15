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
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.akraino.regional_controller.beans.Edgesite;
import org.akraino.regional_controller.beans.POD;
import org.akraino.regional_controller.beans.User;
import org.akraino.regional_controller.db.DB;
import org.akraino.regional_controller.db.DBFactory;
import org.akraino.regional_controller.utils.JSONtoYAML;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * The Edgesite API supports
 */
@Path(EdgesiteAPI.EDGESITE_PATH)
public class EdgesiteAPI extends APIBase {
	public static final String EDGESITE_PATH = "edgesite";

	protected static final String[] EDGESITE_CREATE_RBAC = { "create-*", "create-edgesite" };
	protected static final String[] EDGESITE_READ_RBAC   = { "read-*",   "read-edgesite" };
	protected static final String[] EDGESITE_UPDATE_RBAC = { "update-*", "update-edgesite" };
	protected static final String[] EDGESITE_DELETE_RBAC = { "delete-*", "delete-edgesite" };

	@POST
	@Consumes({APPLICATION_YAML, MediaType.APPLICATION_JSON})
	public Response createEdgesite(
		@HeaderParam(SESSION_TOKEN_HDR) final String token,
		@HeaderParam(REAL_IP_HDR)       final String realIp,
		@HeaderParam(CONTENT_TYPE_HDR)  final String ctype,
		String content)
	{
		String method = "POST /api/v1/edgesite";
		User u = checkToken(token, method, realIp);
		checkRBAC(u, EDGESITE_CREATE_RBAC, method, realIp);

		try {
			JSONObject jo = getContent(ctype, content);
			String uuid = Edgesite.createEdgesite(jo);
			try {
				return Response.created(new URI("/api/v1/edgesite/"+uuid))
					.build();
			} catch (URISyntaxException e) {
				logger.warn(e.toString());
				throw new BadRequestException(e.toString());
			}
		} catch (JSONException e) {
			throw new BadRequestException("Invalid JSON object");
		}
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public String getEdgesitesJSON(
		@HeaderParam(SESSION_TOKEN_HDR) String token,
		@HeaderParam(REAL_IP_HDR) String realIp,
		@QueryParam("region") String region
	) {
		JSONObject jo = getEdgesitesCommon(token, realIp, region);
		return jo.toString();
	}

	@GET
	@Produces(APPLICATION_YAML)
	public String getEdgesitesYAML(
		@HeaderParam(SESSION_TOKEN_HDR) String token,
		@HeaderParam(REAL_IP_HDR) String realIp,
		@QueryParam("region") String region
	) {
		JSONObject jo = getEdgesitesCommon(token, realIp, region);
		return new JSONtoYAML(jo).toString();
	}

	private JSONObject getEdgesitesCommon(String token, String realIp, String region) {
		String method = "GET /api/v1/edgesite";
		User u = checkToken(token, method, realIp);
		checkRBAC(u, EDGESITE_READ_RBAC, method, realIp);

		JSONArray ja = new JSONArray();
		for (Edgesite e : Edgesite.getEdgesitesByRegion(region)) {
			JSONObject jo = e.toJSON();
			jo.put("url", "/api/v1/edgesite/" + jo.get("uuid"));
			ja.put(jo);
		}
		JSONObject jo = new JSONObject();
		jo.put("edgesites", ja);
		return jo;
	}

	@GET
	@Path("/{uuid}")
	@Produces(MediaType.APPLICATION_JSON)
	public String getEdgesiteDetailsJSON(
		@HeaderParam(SESSION_TOKEN_HDR) String token,
		@HeaderParam(REAL_IP_HDR) String realIp,
		@PathParam("uuid") String uuid)
	{
		JSONObject jo = getEdgesiteDetailsCommon(token, realIp, uuid);
		return jo.toString();
	}

	@GET
	@Path("/{uuid}")
	@Produces(APPLICATION_YAML)
	public String getEdgesiteDetailsYAML(
		@HeaderParam(SESSION_TOKEN_HDR) String token,
		@HeaderParam(REAL_IP_HDR) String realIp,
		@PathParam("uuid") String uuid)
	{
		JSONObject jo = getEdgesiteDetailsCommon(token, realIp, uuid);
		return new JSONtoYAML(jo).toString();
	}

	private JSONObject getEdgesiteDetailsCommon(String token, String realIp, String uuid) {
		String method = "GET /api/v1/edgesite";
		User u = checkToken(token, method, realIp);
		checkRBAC(u, EDGESITE_READ_RBAC, method, realIp);

		if (uuid == null || "".equals(uuid)) {
			throw new BadRequestException("bad uuid");
		}
		Edgesite e = Edgesite.getEdgesiteByUUID(uuid);
		if (e == null) {
			throw new NotFoundException();
		}
		return e.toJSON();
	}

	@PUT
	@Path("/{uuid}")
	@Produces(MediaType.APPLICATION_JSON)
	public String putEdgesiteDetailsJSON(
		@HeaderParam(SESSION_TOKEN_HDR) final String token,
		@HeaderParam(REAL_IP_HDR)       final String realIp,
		@HeaderParam(CONTENT_TYPE_HDR)  final String ctype,
		@PathParam("uuid") String uuid,
		String content)
	{
		JSONObject jo = putEdgesiteCommon(token, realIp, uuid);
		return jo.toString();
	}

	@PUT
	@Path("/{uuid}")
	@Produces(APPLICATION_YAML)
	public String putEdgesiteDetailsYAML(
		@HeaderParam(SESSION_TOKEN_HDR) final String token,
		@HeaderParam(REAL_IP_HDR)       final String realIp,
		@HeaderParam(CONTENT_TYPE_HDR)  final String ctype,
		@PathParam("uuid") String uuid,
		String content)
	{
		JSONObject jo = putEdgesiteCommon(token, realIp, uuid);
		return jo.toString();
	}
	private JSONObject putEdgesiteCommon(String token, String realIp, String uuid) {
		String method = "PUT /api/v1/edgesite/"+uuid;
		User u = checkToken(token, method, realIp);
		checkRBAC(u, EDGESITE_UPDATE_RBAC, method, realIp);

		Edgesite es = Edgesite.getEdgesiteByUUID(uuid);
		if (es == null) {
			api_logger.info("{} user {}, realip {} => 404", method, u.getName(), realIp);
			throw new NotFoundException();
		}
		// For now, we ALWAYS disallow this operation
		api_logger.info("{} user {}, realip {} => 403", method, u.getName(), realIp);
		throw new ForbiddenException("RBAC does not allow");
	}

	@DELETE
	@Path("/{uuid}")
	public Response deleteEdgesiteByUUID(
		@HeaderParam(SESSION_TOKEN_HDR) String token,
		@HeaderParam(REAL_IP_HDR)       String realIp,
		@PathParam("uuid") String uuid)
	{
		String method = "DELETE /api/v1/edgesite/"+uuid;
		User u = checkToken(token, method, realIp);
		checkRBAC(u, EDGESITE_DELETE_RBAC, method, realIp);

		if (uuid == null || "".equals(uuid)) {
			api_logger.info("{} user {}, realip {} => 400", method, u.getName(), realIp);
			throw new BadRequestException("bad uuid");
		}
		Edgesite e = Edgesite.getEdgesiteByUUID(uuid);
		if (e == null) {
			api_logger.info("{} user {}, realip {} => 404", method, u.getName(), realIp);
			throw new NotFoundException();
		}
		// Check if Edgesite is in use (any PODs have edgesite == uuid), if so send a 409
		POD p = e.getPOD();
		if (p != null) {
			api_logger.info("{} user {}, realip {} => 409", method, u.getName(), realIp);
			throw new ClientErrorException("This Edgesite is still in use by POD "+p.getUuid(), HttpServletResponse.SC_CONFLICT);
		}
		try {
			DB db = DBFactory.getDB();
			db.deleteEdgesite(e);
			api_logger.info("{} user {}, realip {} => 204", method, u.getName(), realIp);
			return Response.noContent().build();
		} catch (SQLException ex) {
			api_logger.info("{} user {}, realip {} => 500", method, u.getName(), realIp);
			return Response.serverError().build();
		}
	}
}
