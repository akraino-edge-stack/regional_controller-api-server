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

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.json.JSONObject;

@Provider
public class APIExceptionMapper implements ExceptionMapper<WebApplicationException> {

	@Override
	public Response toResponse(WebApplicationException ex) {
		Response r = ex.getResponse();
		String msg = ex.getMessage();
		String errid = "ARC-9999";
		int ix = msg.indexOf(':');
		if (ix > 0) {
			errid = msg.substring(0, ix).trim();
			msg = msg.substring(ix+1).trim();
		}
		String url = String.format("/docs/errors.html#%s", errid.toLowerCase());
		JSONObject jo = new JSONObject();
		jo.put("code", r.getStatus());
		jo.put("errorId", errid);
		jo.put("message", msg);
		jo.put("errorUrl", url);
		return Response
			.status(r.getStatus())
			.entity(jo.toString())
			.type(MediaType.APPLICATION_JSON).
			build();
	}
}
