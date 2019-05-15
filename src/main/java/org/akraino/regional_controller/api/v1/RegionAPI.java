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
import org.akraino.regional_controller.beans.Region;
import org.akraino.regional_controller.beans.User;
import org.akraino.regional_controller.db.DB;
import org.akraino.regional_controller.db.DBFactory;
import org.akraino.regional_controller.utils.JSONtoYAML;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * The Region API supports
 * - POST: to create a new region
 * - GET: to retrieve a list of regions, or information about one region
 */
@Path(RegionAPI.REGION_PATH)
public class RegionAPI extends APIBase {
	public static final String REGION_PATH = "region";

	protected static final String[] REGION_CREATE_RBAC = { "create-*", "create-region" };
	protected static final String[] REGION_READ_RBAC   = { "read-*",   "read-region" };
	protected static final String[] REGION_UPDATE_RBAC = { "update-*", "update-region" };
	protected static final String[] REGION_DELETE_RBAC = { "delete-*", "delete-region" };

	@POST
	@Consumes({APPLICATION_YAML, MediaType.APPLICATION_JSON})
	public Response createRegion(
		@HeaderParam(SESSION_TOKEN_HDR) final String token,
		@HeaderParam(REAL_IP_HDR)       final String realIp,
		@HeaderParam(CONTENT_TYPE_HDR)  final String ctype,
		String content
	) {
		String method = "POST /api/v1/region";
		User u = checkToken(token, method, realIp);
		checkRBAC(u, REGION_CREATE_RBAC, method, realIp);

		try {
			JSONObject jo = getContent(ctype, content);
			String uuid = Region.createRegion(jo);
			if (uuid == null) {
				// Region NOT created
				api_logger.info("{} user {}, realip {} => 400", method, u.getName(), realIp);
				throw new BadRequestException("Region not created.");
			}
			api_logger.info("{} user {}, realip {} => 201", method, u.getName(), realIp);
			return Response.created(new URI("/api/v1/region/"+uuid))
				.build();
		} catch (URISyntaxException e) {
			logger.warn(e.toString());
			throw new BadRequestException(e.toString());
		}
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public String getRegionsJSON(
		@HeaderParam(SESSION_TOKEN_HDR) String token,
		@HeaderParam(REAL_IP_HDR) String realIp,
		@QueryParam("region") String region
	) {
		JSONObject jo = getRegionsCommon(token, realIp, region);
		return jo.toString();
	}

	@GET
	@Produces(APPLICATION_YAML)
	public String getRegionsYAML(
		@HeaderParam(SESSION_TOKEN_HDR) String token,
		@HeaderParam(REAL_IP_HDR) String realIp,
		@QueryParam("region") String region
	) {
		JSONObject jo = getRegionsCommon(token, realIp, region);
		return new JSONtoYAML(jo).toString();
	}

	private JSONObject getRegionsCommon(String token, String realIp, String region) {
		String method = "GET /api/v1/region";
		User u = checkToken(token, method, realIp);
		checkRBAC(u, REGION_READ_RBAC, method, realIp);

		JSONArray ja = new JSONArray();
		for (Region r : Region.getRegions()) {
			JSONObject jo = r.toJSON();
			jo.put("url", "/api/v1/region/" + jo.get("uuid"));
			ja.put(jo);
		}
		JSONObject jo = new JSONObject();
		jo.put("regions", ja);
		return jo;
	}

	@GET
	@Path("/{uuid}")
	@Produces(MediaType.APPLICATION_JSON)
	public String getRegionDetailsJSON(
		@HeaderParam(SESSION_TOKEN_HDR) String token,
		@HeaderParam(REAL_IP_HDR) String realIp,
		@PathParam("uuid") String uuid
	) {
		JSONObject jo = getRegionDetailsCommon(token, realIp, uuid);
		return jo.toString();
	}

	@GET
	@Path("/{uuid}")
	@Produces(APPLICATION_YAML)
	public String getRegionDetailsYAML(
		@HeaderParam(SESSION_TOKEN_HDR) String token,
		@HeaderParam(REAL_IP_HDR) String realIp,
		@PathParam("uuid") String uuid
	) {
		JSONObject jo = getRegionDetailsCommon(token, realIp, uuid);
		return new JSONtoYAML(jo).toString();
	}

	private JSONObject getRegionDetailsCommon(String token, String realIp, String uuid) {
		String method = "GET /api/v1/region/"+uuid;
		User u = checkToken(token, method, realIp);
		checkRBAC(u, REGION_READ_RBAC, method, realIp);

		if (uuid == null || "".equals(uuid)) {
			throw new BadRequestException("bad uuid");
		}
		Region r = Region.getRegionByUUID(uuid);
		if (r == null) {
			throw new NotFoundException();
		}
		// Get a list of Edgesites in the region
		JSONObject jo = r.toJSON();
		JSONArray ja = new JSONArray();
		for (Edgesite e : Edgesite.getEdgesitesByRegion(uuid)) {
			ja.put(e.getUuid());
		}
		jo.put("edgesites", ja);
		return jo;
	}

	@PUT
	@Path("/{uuid}")
	@Consumes({APPLICATION_YAML, MediaType.APPLICATION_JSON})
	@Produces(MediaType.APPLICATION_JSON)
	public String putRegionJSON(
		@HeaderParam(SESSION_TOKEN_HDR) final String token,
		@HeaderParam(REAL_IP_HDR)       final String realIp,
		@HeaderParam(CONTENT_TYPE_HDR)  final String ctype,
		@PathParam("uuid") String uuid,
		String content
	) {
		JSONObject jo = putRegionCommon(token, realIp, uuid);
		return jo.toString();
	}

	@PUT
	@Path("/{uuid}")
	@Consumes({APPLICATION_YAML, MediaType.APPLICATION_JSON})
	@Produces(APPLICATION_YAML)
	public String putRegionYAML(
		@HeaderParam(SESSION_TOKEN_HDR) final String token,
		@HeaderParam(REAL_IP_HDR)       final String realIp,
		@HeaderParam(CONTENT_TYPE_HDR)  final String ctype,
	  	@PathParam("uuid") String uuid,
	  	String content
	) {
		JSONObject jo = putRegionCommon(token, realIp, uuid);
		return new JSONtoYAML(jo).toString();
	}

	private JSONObject putRegionCommon(String token, String realIp, String uuid) {
		String method = "PUT /api/v1/region/"+uuid;
		User u = checkToken(token, method, realIp);
		checkRBAC(u, REGION_UPDATE_RBAC, method, realIp);

		Region r = Region.getRegionByUUID(uuid);
		if (r == null) {
			api_logger.info("{} user {}, realip {} => 404", method, u.getName(), realIp);
			throw new NotFoundException();
		}
		// For now, we ALWAYS disallow this operation
		api_logger.info("{} user {}, realip {} => 403", method, u.getName(), realIp);
		throw new ForbiddenException("RBAC does not allow");
	}

	@DELETE
	@Path("/{uuid}")
	public Response deleteRegionByUUID(
		@HeaderParam(SESSION_TOKEN_HDR) String token,
		@HeaderParam(REAL_IP_HDR)       String realIp,
		@PathParam("uuid") String uuid)
	{
		String method = "DELETE /api/v1/region/"+uuid;
		User u = checkToken(token, method, realIp);
		checkRBAC(u, REGION_DELETE_RBAC, method, realIp);

		if (uuid == null || "".equals(uuid)) {
			api_logger.info("{} user {}, realip {} => 400", method, u.getName(), realIp);
			throw new BadRequestException("bad uuid");
		}
		Region r = Region.getRegionByUUID(uuid);
		if (r == null) {
			api_logger.info("{} user {}, realip {} => 404", method, u.getName(), realIp);
			throw new NotFoundException();
		}
		// Check if Region is in use by any other Regions or Edgesite
		List<Region> list = r.getChildRegions();
		if (list != null && !list.isEmpty()) {
			Region r1 = list.get(0);
			api_logger.info("{} user {}, realip {} => 409", method, u.getName(), realIp);
			throw new ClientErrorException("This Region is still in use by another Region "+r1.getUuid(), HttpServletResponse.SC_CONFLICT);
		}
		List<Edgesite> list2 = r.getEdgesites();
		if (list2 != null && list2.size() > 0) {
			Edgesite e1 = list2.get(0);
			api_logger.info("{} user {}, realip {} => 409", method, u.getName(), realIp);
			throw new ClientErrorException("This Region is still in use by Edgesite "+e1.getUuid(), HttpServletResponse.SC_CONFLICT);
		}
		try {
			DB db = DBFactory.getDB();
			db.deleteRegion(r);
			api_logger.info("{} user {}, realip {} => 204", method, u.getName(), realIp);
			return Response.noContent().build();
		} catch (SQLException e) {
			api_logger.info("{} user {}, realip {} => 500", method, u.getName(), realIp);
			return Response.serverError().build();
		}
	}
}
