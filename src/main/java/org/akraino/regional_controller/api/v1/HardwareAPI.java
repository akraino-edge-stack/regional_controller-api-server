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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.akraino.regional_controller.beans.Hardware;
import org.akraino.regional_controller.beans.Node;
import org.akraino.regional_controller.beans.User;
import org.akraino.regional_controller.db.DB;
import org.akraino.regional_controller.db.DBFactory;
import org.akraino.regional_controller.utils.JSONtoYAML;
import org.json.JSONArray;
import org.json.JSONObject;

@Path(HardwareAPI.HARDWARE_PATH)
public class HardwareAPI extends APIBase {
	public static final String HARDWARE_PATH = "hardware";

	protected static final String[] HARDWARE_CREATE_RBAC = { "create-*", "create-hardware" };
	protected static final String[] HARDWARE_READ_RBAC   = { "read-*",   "read-hardware" };
	protected static final String[] HARDWARE_UPDATE_RBAC = { "update-*", "update-hardware" };
	protected static final String[] HARDWARE_DELETE_RBAC = { "delete-*", "delete-hardware" };

	@POST
	@Consumes({APPLICATION_YAML, MediaType.APPLICATION_JSON})
	public Response createHardware(
		@HeaderParam(SESSION_TOKEN_HDR) final String token,
		@HeaderParam(REAL_IP_HDR)       final String realIp,
		@HeaderParam(CONTENT_TYPE_HDR)  final String ctype,
		String content)
	{
		String method = "POST /api/v1/hardware";
		User u = checkToken(token, method, realIp);
		checkRBAC(u, HARDWARE_CREATE_RBAC, method, realIp);

		try {
			JSONObject jo = getContent(ctype, content);
			String uuid = Hardware.createHardware(jo);
			String url = buildUrl(uuid);
			api_logger.info("{} user {}, realip {} => 201", method, u.getName(), realIp);
			return Response.created(new URI(url)).build();
		} catch (URISyntaxException e) {
			logger.warn(e.toString());
			api_logger.info("{} user {}, realip {} => 400", method, u.getName(), realIp);
			throw new BadRequestException(e.toString());
		}
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public String getHardwareJSON(
		@HeaderParam(SESSION_TOKEN_HDR) String token,
		@HeaderParam(REAL_IP_HDR) String realIp
	) {
		JSONObject jo = getHardwareCommon(token, realIp);
		return jo.toString();
	}

	@GET
	@Produces(APPLICATION_YAML)
	public String getHardwareYAML(
		@HeaderParam(SESSION_TOKEN_HDR) String token,
		@HeaderParam(REAL_IP_HDR) String realIp
	) {
		JSONObject jo = getHardwareCommon(token, realIp);
		return new JSONtoYAML(jo).toString();
	}

	private JSONObject getHardwareCommon(String token, String realIp) {
		String method = "GET /api/v1/hardware";
		User u = checkToken(token, method, realIp);
		checkRBAC(u, HARDWARE_READ_RBAC, method, realIp);

		JSONArray ja = new JSONArray();
		for (Hardware h : Hardware.getHardware()) {
			JSONObject jo = h.toJSON();
			jo.put("url", buildUrl(jo.getString("uuid")));
			ja.put(jo);
		}
		JSONObject jo = new JSONObject();
		jo.put("hardware", ja);
		return jo;
	}

	@GET
	@Path("/{uuid}")
	@Produces(MediaType.APPLICATION_JSON)
	public String getHardwareDetailsJSON(
		@HeaderParam(SESSION_TOKEN_HDR) String token,
		@HeaderParam(REAL_IP_HDR)       String realIp,
		@PathParam("uuid") String uuid
	) {
		JSONObject jo = getHardwareDetailsCommon(token, realIp, uuid);
		return jo.toString();
	}

	@GET
	@Path("/{uuid}")
	@Produces(APPLICATION_YAML)
	public String getHardwareDetailsYAML(
		@HeaderParam(SESSION_TOKEN_HDR) String token,
		@HeaderParam(REAL_IP_HDR)       String realIp,
		@PathParam("uuid") String uuid
	) {
		JSONObject jo = getHardwareDetailsCommon(token, realIp, uuid);
		return new JSONtoYAML(jo).toString();
	}

	private JSONObject getHardwareDetailsCommon(String token, String realIp, String uuid) {
		String method = "GET /api/v1/hardware/"+uuid;
		User u = checkToken(token, method, realIp);
		checkRBAC(u, HARDWARE_READ_RBAC, method, realIp);

		if (uuid == null || "".equals(uuid)) {
			throw new BadRequestException("bad uuid");
		}
		Hardware h = Hardware.getHardwareByUUID(uuid);
		if (h == null) {
			throw new NotFoundException();
		}
		return h.toJSON();
	}

	@PUT
	@Path("/{uuid}")
	@Consumes({APPLICATION_YAML, MediaType.APPLICATION_JSON})
	@Produces(MediaType.APPLICATION_JSON)
	public String putHardwaresJSON(
		@HeaderParam(SESSION_TOKEN_HDR) final String token,
		@HeaderParam(REAL_IP_HDR)       final String realIp,
		@HeaderParam(CONTENT_TYPE_HDR)  final String ctype,
		@PathParam("uuid") String uuid,
		String content
	) {
		JSONObject jo = putHardwaresCommon(token, realIp, uuid);
		return jo.toString();
	}

	@PUT
	@Path("/{uuid}")
	@Consumes({APPLICATION_YAML, MediaType.APPLICATION_JSON})
	@Produces(APPLICATION_YAML)
	public String putHardwaresYAML(
		@HeaderParam(SESSION_TOKEN_HDR) final String token,
		@HeaderParam(REAL_IP_HDR)       final String realIp,
		@HeaderParam(CONTENT_TYPE_HDR)  final String ctype,
		@PathParam("uuid") String uuid,
		String content
	) {
		JSONObject jo = putHardwaresCommon(token, realIp, uuid);
		return new JSONtoYAML(jo).toString();
	}

	private JSONObject putHardwaresCommon(String token, String realIp, String uuid) {
		String method = "PUT /api/v1/hardware/"+uuid;
		User u = checkToken(token, method, realIp);
		checkRBAC(u, HARDWARE_UPDATE_RBAC, method, realIp);

		Hardware bp = Hardware.getHardwareByUUID(uuid);
		if (bp == null) {
			api_logger.info("{} user {}, realip {} => 404", "PUT /api/v1/hardware/"+uuid, u.getName(), realIp);
			throw new NotFoundException();
		}
		// For now, we ALWAYS disallow this operation
		api_logger.info("{} user {}, realip {} => 403", "PUT /api/v1/hardware/"+uuid, u.getName(), realIp);
		throw new ForbiddenException("RBAC does not allow");
	}

	@DELETE
	@Path("/{uuid}")
	public Response deleteHardwareByUUID(
		@HeaderParam(SESSION_TOKEN_HDR) final String token,
		@HeaderParam(REAL_IP_HDR)       final String realIp,
		@PathParam("uuid") String uuid)
	{
		String method = "DELETE /api/v1/hardware/"+uuid;
		User u = checkToken(token, method, realIp);
		checkRBAC(u, HARDWARE_DELETE_RBAC, method, realIp);

		if (uuid == null || "".equals(uuid)) {
			throw new BadRequestException("bad uuid");
		}
		Hardware h = Hardware.getHardwareByUUID(uuid);
		if (h == null) {
			throw new NotFoundException();
		}
		// Check if hardware is in use (any nodes have hardware == uuid), if so send a 409
		for (Node n : Node.getNodes()) {
			if (n.getHardware().equals(uuid)) {
				throw new ClientErrorException("This hardware profile is still in use by Node "+n.getUuid(), HttpServletResponse.SC_CONFLICT);
			}
		}
		try {
			DB db = DBFactory.getDB();
			db.deleteHardware(h);
			return Response.noContent().build();
		} catch (SQLException e) {
			return Response.serverError().build();
		}
	}

	private String buildUrl(String uuid) {
		return String.format("/api/v1/%s/%s", HARDWARE_PATH, uuid);
	}
}
