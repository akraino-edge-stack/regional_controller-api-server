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

import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.akraino.regional_controller.beans.POD;
import org.akraino.regional_controller.beans.PODEvent;
import org.akraino.regional_controller.beans.User;
import org.akraino.regional_controller.utils.JSONtoYAML;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * The PODEventAPI is used (primarily) to allow the Workflow Engine to store POD events in the DB through direct
 * POST calls to the API server.  It is mainly a write-only interface, and is not intended to be publicly used.
 */
@Path(PODEventAPI.PODEVENT_PATH)
public class PODEventAPI extends APIBase {
	public static final String PODEVENT_PATH = "podevent";

	protected static final String[] PODEVENT_CREATE_RBAC = { "create-*", "create-podevent" };
	protected static final String[] PODEVENT_READ_RBAC   = { "read-*", "read-podevent" };

	@POST
	@Consumes({APPLICATION_YAML, MediaType.APPLICATION_JSON})
	public Response createPODEvent(
		@HeaderParam(SESSION_TOKEN_HDR) final String token,
		@HeaderParam(REAL_IP_HDR)       final String realIp,
		@HeaderParam(CONTENT_TYPE_HDR)  final String ctype,
		String content)
	{
		String method = "POST /api/v1/podevent";
		User u = checkToken(token, method, realIp);
		checkRBAC(u, PODEVENT_CREATE_RBAC, method, realIp);

		try {
			JSONObject jo = getContent(ctype, content);
			PODEvent.createPodEvent(jo);
			return Response
				.ok()
				.build();
		} catch (JSONException e) {
			throw new BadRequestException("Invalid JSON object");
		}
	}

	@GET
	@Path("/{uuid}")
	@Produces(MediaType.APPLICATION_JSON)
	public String getPODEventsDetailsJSON(
		@HeaderParam(SESSION_TOKEN_HDR) String token,
		@HeaderParam(REAL_IP_HDR) String realIp,
		@PathParam("uuid") String uuid
	) {
		JSONObject jo = getPODEventsDetailsCommon(token, realIp, uuid);
		return jo.toString();
	}

	@GET
	@Path("/{uuid}")
	@Produces(APPLICATION_YAML)
	public String getPODEventsDetailsYAML(
		@HeaderParam(SESSION_TOKEN_HDR) String token,
		@HeaderParam(REAL_IP_HDR) String realIp,
		@PathParam("uuid") String uuid
	) {
		JSONObject jo = getPODEventsDetailsCommon(token, realIp, uuid);
		return new JSONtoYAML(jo).toString();
	}

	private JSONObject getPODEventsDetailsCommon(String token, String realIp, String uuid) {
		String method = "GET /api/v1/podevent/"+uuid;
		User u = checkToken(token, method, realIp);
		checkRBAC(u, PODEVENT_READ_RBAC, method, realIp);

		POD p = POD.getPodByUUID(uuid);
		if (p == null) {
			throw new NotFoundException();
		}
		JSONArray ja = new JSONArray();
		for (PODEvent pe : p.getPodEvents()) {
			ja.put(pe.toJSON());
		}
		JSONObject jo = new JSONObject();
		jo.put("events", ja);
		return jo;
	}
}
