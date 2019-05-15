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
import org.akraino.regional_controller.api.v1.PODAPI;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class PODAPITest extends BaseAPITest {
	public static final String VALID_DEPLOY_JSON =
		"{ \"name\": \"testpod\", \"blueprint\": \"827cfe84-2e28-11e9-bb34-0017f20dbff8\", \"edgesite\": \"2d35351a-3dcb-11e9-9535-e36fdca4d937\" }";

	@BeforeClass
    public static void setUp() throws Exception {
        startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        stopServer();
    }

    @Test
	public void testCreatePODDryRun() {
		// Get a good Edgesite - use valid JSON argument - dryrun
    	String token = getLoginCookie();
		WebTarget target = ClientBuilder.newClient().target(TEST_URI)
			.path(PODAPI.POD_PATH)
			.queryParam("dryrun", "1");
		Entity<?> entity = Entity.entity(VALID_DEPLOY_JSON, MediaType.APPLICATION_JSON);
		Response r = target
		   	.request(MediaType.APPLICATION_JSON)
		   	.header(APIBase.SESSION_TOKEN_HDR, token)
		   	.header(APIBase.REAL_IP_HDR, "69.69.1.2")
			.buildPost(entity)
			.invoke();
		log(r);
		log(r.readEntity(String.class));
		assertTrue(r.getStatus() == HttpServletResponse.SC_OK);
    }

//    @Test
//	public void testCreatePOD() {
//		// Get a good Edgesite - use valid JSON argument - without the dryrun
//    	String token = getLoginCookie();
//		WebTarget target = ClientBuilder.newClient().target(TEST_URI)
//			.path(PODAPI.POD_PATH);
//		Entity<?> entity = Entity.entity(VALID_DEPLOY_JSON, MediaType.APPLICATION_JSON);
//		Response r = target
//		   	.request(MediaType.APPLICATION_JSON)
//		   	.header(APIBase.SESSION_TOKEN_HDR, token)
//		   	.header(APIBase.REAL_IP_HDR, "69.69.1.2")
//			.buildPost(entity)
//			.invoke();
//		log(r);
//		log(r.readEntity(String.class));
//		assertTrue(r.getStatus() == HttpServletResponse.SC_CREATED);
//    }

    @Test
	public void testCreatePODBadCredentials() {
		// Get a good Edgesite - use valid JSON argument but no cookie
    	WebTarget target = ClientBuilder.newClient().target(TEST_URI).path(PODAPI.POD_PATH);
    	Entity<?> entity = Entity.entity(VALID_DEPLOY_JSON, MediaType.APPLICATION_JSON);
    	Response r = target
		   	.request(MediaType.APPLICATION_JSON)
		   	.header(APIBase.REAL_IP_HDR, "69.69.1.2")
			.buildPost(entity)
			.invoke();
		log(r);
		log(r.readEntity(String.class));
		assertTrue(r.getStatus() == HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
	public void testCreatePODBadArgument() {
		// Get a good Edgesite - use bad JSON argument
    	String token = getLoginCookie();
    	WebTarget target = ClientBuilder.newClient().target(TEST_URI).path(PODAPI.POD_PATH);
    	Entity<?> entity = Entity.entity("BADBADBAD", MediaType.APPLICATION_JSON);
    	Response r = target
		   	.request(MediaType.APPLICATION_JSON)
		   	.header(APIBase.SESSION_TOKEN_HDR, token)
		   	.header(APIBase.REAL_IP_HDR, "69.69.1.2")
			.buildPost(entity)
			.invoke();
		log(r);
		log(r.readEntity(String.class));
		assertTrue(r.getStatus() == HttpServletResponse.SC_BAD_REQUEST);
	}

    @Test
	public void testGetPODsJSON() {
    	testGet(PODAPI.POD_PATH, getLoginCookie(), MediaType.APPLICATION_JSON, HttpServletResponse.SC_OK);
	}

    @Test
	public void testGetPODsYAML() {
    	testGet(PODAPI.POD_PATH, getLoginCookie(), APIBase.APPLICATION_YAML, HttpServletResponse.SC_OK);
	}

    @Test
	public void testGetPODsXML() {
    	testGet(PODAPI.POD_PATH, getLoginCookie(), MediaType.APPLICATION_XML, HttpServletResponse.SC_NOT_ACCEPTABLE);
	}

    @Test
	public void testGetPODsJSONNoAccess() {
    	testGet(PODAPI.POD_PATH, getNoAccessLoginCookie(), MediaType.APPLICATION_JSON, HttpServletResponse.SC_FORBIDDEN);
	}
}
