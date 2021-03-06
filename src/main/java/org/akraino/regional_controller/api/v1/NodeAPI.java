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

import org.akraino.regional_controller.beans.BaseBean;
import org.akraino.regional_controller.beans.Edgesite;
import org.akraino.regional_controller.beans.Node;
import org.akraino.regional_controller.beans.User;
import org.akraino.regional_controller.db.DB;
import org.akraino.regional_controller.db.DBFactory;
import org.akraino.regional_controller.utils.JSONtoYAML;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@Path(NodeAPI.NODE_PATH)
public class NodeAPI extends APIBase {
	public static final String NODE_PATH = "node";

	protected static final String[] NODE_CREATE_RBAC = { "create-*", "create-node" };
	protected static final String[] NODE_READ_RBAC   = { "read-*",   "read-node" };
	protected static final String[] NODE_UPDATE_RBAC = { "update-*", "update-node" };
	protected static final String[] NODE_DELETE_RBAC = { "delete-*", "delete-node" };

	@POST
	@Consumes({APPLICATION_YAML, MediaType.APPLICATION_JSON})
	public Response createNode(
		@HeaderParam(SESSION_TOKEN_HDR) final String token,
		@HeaderParam(REAL_IP_HDR)       final String realIp,
		@HeaderParam(CONTENT_TYPE_HDR)  final String ctype,
		String content)
	{
		String method = "POST /api/v1/node";
		User u = checkToken(token, method, realIp);
		checkRBAC(u, NODE_CREATE_RBAC, method, realIp);

		try {
			JSONObject jo = getContent(ctype, content);
			String uuid = Node.createNode(jo);
			String url = buildUrl(uuid);
			return Response.created(new URI(url)).build();
		} catch (URISyntaxException e) {
			logger.warn(e.toString());
			throw new BadRequestException("ARC-1030: "+e.toString());
		}
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public String getNodesJSON(
		@HeaderParam(SESSION_TOKEN_HDR) String token,
		@HeaderParam(REAL_IP_HDR) String realIp,
		@QueryParam("region") String region)
	{
		JSONObject jo = getNodesCommon(token, realIp);
		return jo.toString();
	}

	@GET
	@Produces(APPLICATION_YAML)
	public String getNodesYAML(
		@HeaderParam(SESSION_TOKEN_HDR) String token,
		@HeaderParam(REAL_IP_HDR) String realIp,
		@QueryParam("region") String region)
	{
		JSONObject jo = getNodesCommon(token, realIp);
		return new JSONtoYAML(jo).toString();
	}

	private JSONObject getNodesCommon(String token, String realIp) {
		String method = "GET /api/v1/node";
		User u = checkToken(token, method, realIp);
		checkRBAC(u, NODE_READ_RBAC, method, realIp);

		JSONArray ja = new JSONArray();
		for (Node n : Node.getNodes()) {
			JSONObject jo = n.toJSON();
			jo.put("url", buildUrl(jo.getString("uuid")));
			ja.put(jo);
		}
		JSONObject jo = new JSONObject();
		jo.put("nodes", ja);
		return jo;
	}

	@GET
	@Path("/{uuid}")
	@Produces(MediaType.APPLICATION_JSON)
	public String getNodeDetailsJSON(
		@HeaderParam(SESSION_TOKEN_HDR) String token,
		@HeaderParam(REAL_IP_HDR) String realIp,
		@PathParam("uuid") String uuid)
	{
		JSONObject jo = getNodeDetailsCommon(token, realIp, uuid);
		return jo.toString();
	}

	@GET
	@Path("/{uuid}")
	@Produces(APPLICATION_YAML)
	public String getNodeDetailsYAML(
		@HeaderParam(SESSION_TOKEN_HDR) String token,
		@HeaderParam(REAL_IP_HDR) String realIp,
		@PathParam("uuid") String uuid)
	{
		JSONObject jo = getNodeDetailsCommon(token, realIp, uuid);
		return new JSONtoYAML(jo).toString();
	}

	private JSONObject getNodeDetailsCommon(String token, String realIp, String uuid) {
		String method = "GET /api/v1/node/"+uuid;
		User u = checkToken(token, method, realIp);
		checkRBAC(u, NODE_READ_RBAC, method, realIp);

		if (uuid == null || "".equals(uuid)) {
			throw new BadRequestException("ARC-1028: bad UUID");
		}
		Node n = Node.getNodeByUUID(uuid);
		if (n == null) {
			throw new NotFoundException("ARC-4001: object not found");
		}
		return n.toJSON();
	}

	@PUT
	@Path("/{uuid}")
	@Consumes({APPLICATION_YAML, MediaType.APPLICATION_JSON})
	public Response putNodes(
		@HeaderParam(SESSION_TOKEN_HDR) final String token,
		@HeaderParam(REAL_IP_HDR)       final String realIp,
		@HeaderParam(CONTENT_TYPE_HDR)  final String ctype,
		@PathParam("uuid") String uuid,
		String content
	) {
		String method = "PUT /api/v1/node/"+uuid;
		User u = checkToken(token, method, realIp);
		checkRBAC(u, NODE_UPDATE_RBAC, method, realIp);

		Node n = Node.getNodeByUUID(uuid);
		if (n == null) {
			api_logger.info("{} user {}, realip {} => 404", method, u.getName(), realIp);
			throw new NotFoundException("ARC-4001: object not found");
		}

		try {
			// Can only change the name, description & YAML of the Node
			JSONObject jo = getContent(ctype, content);
			Set<String> keys = jo.keySet();
			if (keys.contains(BaseBean.UUID_TAG)) {
				throw new ForbiddenException("ARC-3008: Not allowed to modify the Node's UUID.");
			}
			boolean doupdate = false;
			if (keys.contains(BaseBean.NAME_TAG)) {
				String name = jo.getString(BaseBean.NAME_TAG);
				if (!name.equals(n.getName())) {
					n.setName(name);
					doupdate = true;
				}
			}
			if (keys.contains(BaseBean.DESCRIPTION_TAG)) {
				String description = jo.getString(BaseBean.DESCRIPTION_TAG);
				if (!description.equals(n.getDescription())) {
					n.setDescription(description);
					doupdate = true;
				}
			}
			if (keys.contains(BaseBean.YAML_TAG)) {
				JSONObject yaml = jo.getJSONObject(BaseBean.YAML_TAG);
				// Make sure this Node is not in use
				for (Edgesite es : Edgesite.getEdgesites()) {
					if (es.getNodes().contains(uuid)) {
						throw new ForbiddenException("ARC-3020: Not allowed to modify the YAML for a Node that is in use.");
					}
				}
				n.setYaml(yaml.toString());
				doupdate = true;
			}
			if (doupdate) {
				n.updateNode();
			}
			return Response.ok().build();
		} catch (JSONException e) {
			logger.warn(e.toString());
			throw new BadRequestException("ARC-1030: "+e.toString());
		}
	}

	@DELETE
	@Path("/{uuid}")
	public Response deleteNodeByUUID(
	  @HeaderParam(SESSION_TOKEN_HDR) final String token,
	  @HeaderParam(REAL_IP_HDR)       final String realIp,
	  @PathParam("uuid") String uuid)
	{
		String method = "DELETE /api/v1/node/"+uuid;
		User u = checkToken(token, method, realIp);
		checkRBAC(u, NODE_DELETE_RBAC, method, realIp);

		if (uuid == null || "".equals(uuid)) {
			throw new BadRequestException("ARC-1028: bad UUID");
		}
		Node n = Node.getNodeByUUID(uuid);
		if (n == null) {
			throw new NotFoundException("ARC-4001: object not found");
		}
		// Check if Node is in use (any Edgesites have Node == uuid), if so send a 409
		for (Edgesite e : Edgesite.getEdgesites()) {
			if (e.getNodes().contains(n.getUuid())) {
				throw new ClientErrorException("ARC-2003: This Node is still in use by Edgesite "+e.getUuid(), HttpServletResponse.SC_CONFLICT);
			}
		}
		try {
			DB db = DBFactory.getDB();
			db.deleteNode(n);
			return Response.noContent().build();
		} catch (SQLException e) {
			return Response.serverError().build();
		}
	}

	private String buildUrl(String uuid) {
		return String.format("/api/v1/%s/%s", NODE_PATH, uuid);
	}
}
