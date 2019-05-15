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

package org.akraino.xportal.api.v1;

import static org.junit.Assert.assertTrue;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.akraino.regional_controller.api.v1.APIBase;
import org.akraino.regional_controller.api.v1.EdgesiteAPI;
import org.akraino.regional_controller.utils.JSONtoYAML;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class EdgesiteAPITest extends BaseAPITest {
	public static final String VALID_DEPLOY_JSON = "{ \"blueprint\": \"827cfe84-2e28-11e9-bb34-0017f20dbff8\", \"foo\": \"bar\" }";

	@BeforeClass
	public static void setUp() throws Exception {
		startServer();
	}

	@AfterClass
	public static void tearDown() throws Exception {
		stopServer();
	}

	@Test
	public void testGetEdgesites() {
    	testGet(EdgesiteAPI.EDGESITE_PATH, getLoginCookie(), MediaType.APPLICATION_JSON, HttpServletResponse.SC_OK);
	}

	@Test
	public void testGetEdgesiteByUUID() {
		String token = getLoginCookie();
		String es_id = getValidEdgesiteUUID(token);

		// Get a good Edgesite
		WebTarget target = ClientBuilder.newClient().target(TEST_URI).path(EdgesiteAPI.EDGESITE_PATH).path(es_id);
		Response r = target
		   	.request(MediaType.APPLICATION_JSON)
		   	.header(APIBase.SESSION_TOKEN_HDR, token)
		   	.header(APIBase.REAL_IP_HDR, "69.69.1.2")
			.buildGet()
			.invoke();
		log(r);
		String t = r.readEntity(String.class);
		log(t);
		JSONtoYAML jy = new JSONtoYAML(t);
		log(jy.toString());
		assertTrue(r.getStatus() == HttpServletResponse.SC_OK);
	}

	@Test
	public void testGetEdgesiteByBadUUID() {
    	testGet(EdgesiteAPI.EDGESITE_PATH + "/BADBADBAD", getLoginCookie(), MediaType.APPLICATION_JSON, HttpServletResponse.SC_NOT_FOUND);
	}

	@Test
	public void testBogusSite() {
		String token = getLoginCookie();
		String es_id = getValidEdgesiteUUID(token);

		// Get a good Edgesite - use valid JSON argument
		WebTarget target = ClientBuilder.newClient().target(TEST_URI).path(EdgesiteAPI.EDGESITE_PATH).path(es_id).path("bogus");
		Entity<?> entity = Entity.entity(VALID_DEPLOY_JSON, MediaType.APPLICATION_JSON);
		Response r = target
		   	.request(MediaType.APPLICATION_JSON)
		   	.header(APIBase.SESSION_TOKEN_HDR, token)
		   	.header(APIBase.REAL_IP_HDR, "69.69.1.2")
			.buildPut(entity)
			.invoke();
		log(r);
		log(r.readEntity(String.class));
		assertTrue(r.getStatus() == HttpServletResponse.SC_NOT_FOUND);
	}

	@Test
	public void testCreateEdgesite() {
		// Good request
		String token = getLoginCookie();
		WebTarget target = ClientBuilder.newClient().target(TEST_URI).path(EdgesiteAPI.EDGESITE_PATH);
		String json = "{ \"name\": \"newedge@@\", \"description\": \"testing only\", \"regions\": [ \"5c1e6560-2e33-11e9-821c-0017f20dbff8\" ], \"nodes\": [ \"ec399b9a-47fd-11e9-9f20-af67efa1a3dd\" ] }";
		json = json.replaceAll("@@", ""+System.currentTimeMillis());
		Entity<?> entity = Entity.entity(json, MediaType.APPLICATION_JSON);
		Response r = target
			.request(MediaType.APPLICATION_JSON)
			.header(APIBase.SESSION_TOKEN_HDR, token)
			.header(APIBase.REAL_IP_HDR, "69.69.1.2")
			.buildPost(entity)
			.invoke();
        log(r);
        assertTrue(r.getStatus() == HttpServletResponse.SC_CREATED);
	}

	@Test
	public void testCreateEdgesiteBadArgument() {
		// Bad request
		String token = getLoginCookie();
		WebTarget target = ClientBuilder.newClient().target(TEST_URI).path(EdgesiteAPI.EDGESITE_PATH);
		String bad_json = "{ \"namexx\": \"badedge\", \"description\": \"testing only\" }";
		Entity<?> entity = Entity.entity(bad_json, MediaType.APPLICATION_JSON);
		Response r = target
			.request(MediaType.APPLICATION_JSON)
			.header(APIBase.SESSION_TOKEN_HDR, token)
			.header(APIBase.REAL_IP_HDR, "69.69.1.2")
			.buildPost(entity)
			.invoke();
        log(r);
        assertTrue(r.getStatus() == HttpServletResponse.SC_BAD_REQUEST);
	}

	private String getValidEdgesiteUUID(String token) {
		String msg = testGet(EdgesiteAPI.EDGESITE_PATH, getLoginCookie(), MediaType.APPLICATION_JSON, HttpServletResponse.SC_OK);
		log(msg);
		JSONObject jo = new JSONObject(new JSONTokener(msg));
		JSONArray edgesites = jo.getJSONArray("edgesites");
		return edgesites.getJSONObject(0).getString("uuid");
	}
}
