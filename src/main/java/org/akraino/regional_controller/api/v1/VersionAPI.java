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

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.akraino.regional_controller.beans.Blueprint;
import org.akraino.regional_controller.beans.User;
import org.akraino.regional_controller.utils.BuildUtil;
import org.akraino.regional_controller.utils.JSONtoYAML;
import org.json.JSONObject;

@Path(VersionAPI.VERSION_PATH)
public class VersionAPI extends APIBase {
	public static final String VERSION_PATH = "version";

	protected static final String[] VERSION_READ_RBAC = { "read-*", "read-version" };

	/**
	 * Retrieve a list of component versions for the Regional Controller.
	 * This method is called for the request:
	 *
	 *     GET /api/v1/version
	 *
	 * @param cookie cookie identifying the user's session
	 * @return 200 - OK with JSON describing versions of all the components
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public String getVersionsJSON(
		@HeaderParam(SESSION_TOKEN_HDR) String token,
		@HeaderParam(REAL_IP_HDR) String realIp
	) {
		JSONObject jo = getVersionsCommon(token, realIp);
		return jo.toString();
	}

	@GET
	@Produces(APPLICATION_YAML)
	public String getVersionsYAML(
		@HeaderParam(SESSION_TOKEN_HDR) String token,
		@HeaderParam(REAL_IP_HDR) String realIp
	) {
		JSONObject jo = getVersionsCommon(token, realIp);
		return new JSONtoYAML(jo).toString();
	}

	private JSONObject getVersionsCommon(String token, String realIp) {
		String method = "GET /api/v1/version";
		User u = checkToken(token, method, realIp);
		checkRBAC(u, VERSION_READ_RBAC, method, realIp);

		JSONObject blueprints = new JSONObject();
		for (Blueprint bp : Blueprint.getBlueprints()) {
			blueprints.put(bp.getName(), bp.getVersion());
		}

		JSONObject jo = new JSONObject();
		jo.put("api_version", BuildUtil.getVersion());
		jo.put("api_builddate", BuildUtil.getBuildDate());
		jo.put("blueprints", blueprints);
		return jo;
	}
}
