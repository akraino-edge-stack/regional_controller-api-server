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
import java.util.List;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.akraino.regional_controller.beans.Blueprint;
import org.akraino.regional_controller.beans.Edgesite;
import org.akraino.regional_controller.beans.POD;
import org.akraino.regional_controller.beans.PODEvent;
import org.akraino.regional_controller.beans.User;
import org.akraino.regional_controller.utils.JSONtoYAML;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@Path(PODAPI.POD_PATH)
public class PODAPI extends APIBase {
	public static final String POD_PATH = "pod";

	protected static final String[] POD_CREATE_RBAC = { "create-*", "create-pod" };
	protected static final String[] POD_READ_RBAC   = { "read-*",   "read-pod" };
	protected static final String[] POD_UPDATE_RBAC = { "update-*", "update-pod" };
	protected static final String[] POD_DELETE_RBAC = { "delete-*", "delete-pod" };

	@POST
	@Consumes({APPLICATION_YAML, MediaType.APPLICATION_JSON})
	public Response createPOD(
		@HeaderParam(SESSION_TOKEN_HDR) final String token,
		@HeaderParam(REAL_IP_HDR)       final String realIp,
		@HeaderParam(CONTENT_TYPE_HDR)  final String ctype,
		@QueryParam("dryrun")           String dryrun,
		String content)
	{
		String method = "POST /api/v1/pod";
		User u = checkToken(token, method, realIp);
		checkRBAC(u, POD_CREATE_RBAC, method, realIp);

		try {
			JSONObject jo = getContent(ctype, content);

			// 1. Verify Blueprint is valid
			String blueprint = jo.optString("blueprint");
			if (blueprint == null || "".equals(blueprint)) {
				String msg = "Missing Blueprint UUID";
				logger.warn(msg);
				throw new BadRequestException(msg);
			}
			Blueprint bp = Blueprint.getBlueprintByUUID(blueprint);
			if (bp == null) {
				String msg = "No blueprint with UUID="+blueprint;
				logger.warn(msg);
				throw new BadRequestException(msg);
			}

			// 2. Verify Edgesite is valid and not in use
			String edgesite  = jo.optString("edgesite");
			if (edgesite == null || "".equals(edgesite)) {
				String msg = "Missing Edgesite UUID";
				logger.warn(msg);
				throw new BadRequestException(msg);
			}
			Edgesite e = Edgesite.getEdgesiteByUUID(edgesite);
			if (e == null) {
				String msg = "No Edgesite with UUID="+edgesite;
				logger.warn(msg);
				throw new NotFoundException(msg);
			}
			if (e.getPOD() != null) {
				String msg = "The Edgesite "+edgesite+" is already in use by POD "+e.getPOD().getUuid();
				logger.warn(msg);
				throw new BadRequestException(msg);
			}

			// 3. Verify that the Blueprint hardware profile allows deployment on this Edgesite
			List<String> errors = bp.isCompatibleHardware(e);
			if (! errors.isEmpty()) {
				String msg = "The Edgesite "+edgesite+" is not hardware compatible with the Blueprint "+blueprint;
				logger.warn(msg);
				for (String s : errors) {
					msg = msg + "\n" + s;
					logger.warn(s);
				}
				throw new BadRequestException(msg);
			}

			// 4. Verify that all data required from input schema in the Blueprint is in the uploaded file
			errors = bp.isCompatibleYAML("create", jo.optJSONObject("yaml"));
			if (! errors.isEmpty()) {
				String msg = "The uploaded YAML is not compatible with the Blueprint "+blueprint;
				logger.warn(msg);
				for (String s : errors) {
					msg = msg + "\n" + s;
					logger.warn(s);
				}
				throw new BadRequestException(msg);
			}

			// 5. Check that there is a "create" workflow in the Blueprint
			JSONObject create_wf = bp.getObjectStanza("workflow/create");
			if (create_wf == null) {
				String msg = "There is no 'create' workflow in the Blueprint "+blueprint;
				logger.warn(msg);
				throw new BadRequestException(msg);
			}

			// 6. If a dry run, then we are done!
			if (dryrun != null && dryrun.length() > 0) {
				// Verify only -- do not actually start the POD creation
				return Response.ok().build();
			}

			try {
				// 7. Create the POD in the DB.
				POD p = POD.createPod(jo);
				String uuid = p.getUuid();

				// 8. Run the "create" workflow
				if (!p.startWorkFlow(Blueprint.WF_CREATE)) {
					String msg = "Could not start workflow "+Blueprint.WF_CREATE+" for POD "+p.getUuid();
					logger.error(msg);
					throw new BadRequestException(msg);
				}

				return Response.created(new URI("/api/v1/pod/"+uuid))
					.build();
			} catch (URISyntaxException ex) {
				logger.warn(ex.toString());
				throw new BadRequestException(ex.toString());
			}
		} catch (JSONException e) {
			logger.warn("Invalid JSON object: "+e);
			throw new BadRequestException("createPOD: Invalid JSON object: "+e);
		}
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public String getPODsJSON(
		@HeaderParam(SESSION_TOKEN_HDR) String token,
		@HeaderParam(REAL_IP_HDR) String realIp
	) {
		JSONObject jo = getPODsCommon(token, realIp);
		return jo.toString();
	}

	@GET
	@Produces(APPLICATION_YAML)
	public String getPODsYAML(
		@HeaderParam(SESSION_TOKEN_HDR) String token,
		@HeaderParam(REAL_IP_HDR) String realIp
	) {
		JSONObject jo = getPODsCommon(token, realIp);
		return new JSONtoYAML(jo).toString();
	}

	private JSONObject getPODsCommon(String token, String realIp)
	{
		String method = "GET /api/v1/pod";
		User u = checkToken(token, method, realIp);
		checkRBAC(u, POD_READ_RBAC, method, realIp);

		JSONArray ja = new JSONArray();
		for (POD p : POD.getPods()) {
			JSONObject jo = p.toJSON();
			jo.put("url", "/api/v1/pod/" + jo.get("uuid"));
			ja.put(jo);
		}
		JSONObject jo = new JSONObject();
		jo.put("pods", ja);
		return jo;
	}

	@GET
	@Path("/{uuid}")
	@Produces(MediaType.APPLICATION_JSON)
	public String getPODDetailsJSON(
		@HeaderParam(SESSION_TOKEN_HDR) String token,
		@HeaderParam(REAL_IP_HDR) String realIp,
		@PathParam("uuid") String uuid)
	{
		JSONObject jo = getPODDetailsCommon(token, realIp, uuid);
		return jo.toString();
	}

	@GET
	@Path("/{uuid}")
	@Produces(APPLICATION_YAML)
	public String getPODDetailsYAML(
		@HeaderParam(SESSION_TOKEN_HDR) String token,
		@HeaderParam(REAL_IP_HDR) String realIp,
		@PathParam("uuid") String uuid)
	{
		JSONObject jo = getPODDetailsCommon(token, realIp, uuid);
		return new JSONtoYAML(jo).toString();
	}

	private JSONObject getPODDetailsCommon(String token, String realIp, String uuid) {
		String method = "GET /api/v1/pod/"+uuid;
		User u = checkToken(token, method, realIp);
		checkRBAC(u, POD_READ_RBAC, method, realIp);

		if (uuid == null || "".equals(uuid)) {
			throw new BadRequestException("bad uuid");
		}
		POD p = POD.getPodByUUID(uuid);
		if (p == null) {
			throw new NotFoundException();
		}
		JSONObject jo = p.toJSON();
		jo.put("url", "/api/v1/pod/" + jo.get("uuid"));
		JSONArray ja = new JSONArray();
		for (PODEvent pe : p.getPodEvents()) {
			ja.put(pe.toJSON());
		}
		jo.put("events", ja);
		return jo;
	}

	@PUT
	@Path("/{uuid}")
	@Consumes({APPLICATION_YAML, MediaType.APPLICATION_JSON})
	@Produces(MediaType.APPLICATION_JSON)
	public String putPODJSON(
		@HeaderParam(SESSION_TOKEN_HDR) final String token,
		@HeaderParam(REAL_IP_HDR)       final String realIp,
		@HeaderParam(CONTENT_TYPE_HDR)  final String ctype,
	  	@PathParam("uuid")              final String uuid,
		String content
	) {
		JSONObject jo = putPODCommon(token, realIp, uuid, null);
		return jo.toString();
	}

	@PUT
	@Path("/{uuid}")
	@Consumes({APPLICATION_YAML, MediaType.APPLICATION_JSON})
	@Produces(APPLICATION_YAML)
	public String putPODYAML(
		@HeaderParam(SESSION_TOKEN_HDR) final String token,
		@HeaderParam(REAL_IP_HDR)       final String realIp,
		@HeaderParam(CONTENT_TYPE_HDR)  final String ctype,
	  	@PathParam("uuid")              final String uuid,
	  	String content
	) {
		JSONObject jo = putPODCommon(token, realIp, uuid, null);
		return new JSONtoYAML(jo).toString();
	}

	@PUT
	@Path("/{uuid}/{wfname}")
	@Consumes({APPLICATION_YAML, MediaType.APPLICATION_JSON})
	@Produces(MediaType.APPLICATION_JSON)
	public String putPODWorkflowJSON(
		@HeaderParam(SESSION_TOKEN_HDR) final String token,
		@HeaderParam(REAL_IP_HDR)       final String realIp,
		@HeaderParam(CONTENT_TYPE_HDR)  final String ctype,
	  	@PathParam("uuid")              final String uuid,
		@PathParam("wfname")            final String wfname,
		String content
	) {
		JSONObject jo = putPODCommon(token, realIp, uuid, wfname);
		return jo.toString();
	}

	@PUT
	@Path("/{uuid}/{wfname}")
	@Consumes({APPLICATION_YAML, MediaType.APPLICATION_JSON})
	@Produces(APPLICATION_YAML)
	public String putPODWorkflowYAML(
		@HeaderParam(SESSION_TOKEN_HDR) final String token,
		@HeaderParam(REAL_IP_HDR)       final String realIp,
		@HeaderParam(CONTENT_TYPE_HDR)  final String ctype,
	  	@PathParam("uuid")              final String uuid,
		@PathParam("wfname")            final String wfname,
	  	String content
	) {
		JSONObject jo = putPODCommon(token, realIp, uuid, wfname);
		return new JSONtoYAML(jo).toString();
	}

	private JSONObject putPODCommon(String token, String realIp, String uuid, String wfname) {
		String method = "PUT /api/v1/pod/"+uuid;
		User u = checkToken(token, method, realIp);
		checkRBAC(u, POD_UPDATE_RBAC, method, realIp);

		POD p = POD.getPodByUUID(uuid);
		if (p == null) {
			api_logger.info("{} user {}, realip {} => 404", method, u.getName(), realIp);
			throw new NotFoundException();
		}
		// For now, we ALWAYS disallow this operation
		api_logger.info("{} user {}, realip {} => 403", method, u.getName(), realIp);
		throw new ForbiddenException("RBAC does not allow");
	}

	@DELETE
	@Path("/{uuid}")
	public Response deletePOD(
		@HeaderParam(SESSION_TOKEN_HDR) String token,
		@HeaderParam(REAL_IP_HDR)       String realIp,
		@PathParam("uuid") String uuid)
	{
		String method = "DELETE /api/v1/pod/"+uuid;
		User u = checkToken(token, method, realIp);
		checkRBAC(u, POD_DELETE_RBAC, method, realIp);

		POD p = POD.getPodByUUID(uuid);
		if (p == null) {
			api_logger.info("{} user {}, realip {} => 404", method, u.getName(), realIp);
			throw new NotFoundException();
		}

		// verify POD is in a state that can be deleted
		if (!p.isAlive()) {
			throw new NotAllowedException("POD is not alive.");
		}
		if (p.isWorkflowRunning()) {
			throw new NotAllowedException("A workflow is currently running on the POD.");
		}

		// Check that there is a "delete" workflow in the Blueprint
		JSONObject delete_wf = p.getBlueprintObject().getObjectStanza("workflow/delete");
		if (delete_wf == null) {
			// No, delete the POD straight away - Note: the POD is not actually removed
			// from the DB.  It is put into the DEAD state, from which the Edgesite may
			// be reused, and in which case the POD goes to the ZOMBIE state.
			// POD history may always be retrieved even after the POD is DELETE-ed.
			p.setState(POD.State.DEAD);
			return Response.ok().build();
		}

		// if so, start delete workflow
		if (!p.startWorkFlow(Blueprint.WF_DELETE)) {
			String msg = "Could not start workflow "+Blueprint.WF_DELETE+" for POD "+p.getUuid();
			logger.error(msg);
			throw new BadRequestException(msg);
		}

		// Return 202 - Accepted
		return Response.accepted().build();
	}
}
