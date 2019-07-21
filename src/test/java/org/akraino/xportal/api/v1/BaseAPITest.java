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

import java.net.URI;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.akraino.regional_controller.api.v1.APIBase;
import org.akraino.regional_controller.api.v1.BlueprintAPI;
import org.akraino.regional_controller.api.v1.EdgesiteAPI;
import org.akraino.regional_controller.api.v1.HardwareAPI;
import org.akraino.regional_controller.api.v1.LoginAPI;
import org.akraino.regional_controller.api.v1.NodeAPI;
import org.akraino.regional_controller.api.v1.PODAPI;
import org.akraino.regional_controller.api.v1.PODEventAPI;
import org.akraino.regional_controller.api.v1.RegionAPI;
import org.akraino.regional_controller.api.v1.UserAPI;
import org.akraino.regional_controller.api.v1.VersionAPI;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

public class BaseAPITest {
	public static final String TEST_URI            = "http://localhost:6969/api/v1/";
	public static final String VALID_LOGIN_JSON    = "{ \"name\": \"admin\",    \"password\": \"admin123\" }";
	public static final String VALID_LOGIN_YAML    = "name: admin\npassword: admin123\n";
	public static final String INVALID_LOGIN_JSON  = "{ \"name\": \"foo\",      \"password\": \"bar\" }";
	public static final String NOACCESS_LOGIN_JSON = "{ \"name\": \"noaccess\", \"password\": \"noaccess\" }";

	private static HttpServer server;

	public static void startServer() throws Exception {
		// Start the HTTP server
		final ResourceConfig rc = new ResourceConfig(
			BlueprintAPI.class,
			EdgesiteAPI.class,
			HardwareAPI.class,
			LoginAPI.class,
			NodeAPI.class,
			PODAPI.class,
			PODEventAPI.class,
			RegionAPI.class,
			UserAPI.class,
			VersionAPI.class
		);
		server = GrizzlyHttpServerFactory.createHttpServer(URI.create(TEST_URI), rc);
	}

	public static void stopServer() throws Exception {
		if (server != null) {
			server.shutdownNow();
		}
	}

	private String valid_token = null;
	private String noaccess_token = null;

	public synchronized String getLoginCookie() {
		if (valid_token == null) {
			valid_token = getToken(VALID_LOGIN_JSON);
		}
		return valid_token;
	}
	public synchronized String getNoAccessLoginCookie() {
		if (noaccess_token == null) {
			noaccess_token = getToken(NOACCESS_LOGIN_JSON);
		}
		return noaccess_token;
	}

	/**
	 * Test a POST request against the API.
	 * @param path the path to append to the API base path
	 * @param token the login token to use. If null, no token header is used.
	 * @param mtype the value to use for the Content-Type: header.
	 * @param content the content to pass to the POST
	 * @param accept the value to use for the "Accept" header. If null, no Accept; header is used.
	 * @param rcode the expected response code
	 * @return the content returned from the request
	 */
	protected String testPost(String path, String token, String mtype, String content, String accept, int rcode) {
		WebTarget target = ClientBuilder.newClient().target(TEST_URI).path(path);
		Entity<?> entity = Entity.entity(content, mtype);
	    Builder b = (accept == null) ? target.request() : target.request(accept);
	    b = b.header(APIBase.REAL_IP_HDR, "69.69.1.2");
	    if (token != null)
	    	b = b.header(APIBase.SESSION_TOKEN_HDR, token);
	    Response r = b.buildPost(entity).invoke();
	    String rv = r.readEntity(String.class);
	    log(rv);
	    assertTrue(r.getStatus() == rcode);
	    return rv;
	}

	/**
	 * Test a GET request against the API.
	 * @param path the path to append to the API base path
	 * @param token the login token to use. If null, no token header is used.
	 * @param accept the value to use for the "Accept" header. If null, no Accept; header is used.
	 * @param rcode the expected response code
	 * @return the content returned from the request
	 */
	protected String testGet(String path, String token, String accept, int rcode) {
	    WebTarget target = ClientBuilder.newClient().target(TEST_URI).path(path);
	    Builder b = (accept == null) ? target.request() : target.request(accept);
	    b = b.header(APIBase.REAL_IP_HDR, "69.69.1.2");
	    if (token != null)
	    	b = b.header(APIBase.SESSION_TOKEN_HDR, token);
	    Response r = b.buildGet().invoke();
	    log(r);
	    String rv = r.readEntity(String.class);
	    log(rv);
	    assertTrue(r.getStatus() == rcode);
	    return rv;
	}

	/**
	 * Test a PUT request against the API.
	 * @param path the path to append to the API base path
	 * @param token the login token to use. If null, no token header is used.
	 * @param mtype the value to use for the Content-Type: header.
	 * @param content the content to pass to the POST
	 * @param accept the value to use for the "Accept" header. If null, no Accept; header is used.
	 * @param rcode the expected response code
	 * @return the content returned from the request
	 */
   protected String testPut(String path, String token, String mtype, String content, String accept, int rcode) {
		WebTarget target = ClientBuilder.newClient().target(TEST_URI).path(path);
		Entity<?> entity = Entity.entity(content, mtype);
	    Builder b = (accept == null) ? target.request() : target.request(accept);
	    b = b.header(APIBase.REAL_IP_HDR, "69.69.1.2");
	    if (token != null)
	    	b = b.header(APIBase.SESSION_TOKEN_HDR, token);
	    Response r = b.buildPut(entity).invoke();
	    String rv = r.readEntity(String.class);
	    log(rv);
	    assertTrue(r.getStatus() == rcode);
	    return rv;
	}

	/**
	 * Test a DELETE request against the API.
	 * @param path the path to append to the API base path
	 * @param token the login token to use. If null, no token header is used.
	 * @param rcode the expected response code
	 * @return the content returned from the request
	 */
	protected String testDelete(String path, String token, int rcode) {
	    WebTarget target = ClientBuilder.newClient().target(TEST_URI).path(path);
	    Builder b = target.request();
	    b = b.header(APIBase.REAL_IP_HDR, "69.69.1.2");
	    if (token != null)
	    	b = b.header(APIBase.SESSION_TOKEN_HDR, token);
	    Response r = b.buildDelete().invoke();
	    log(r);
	    String rv = r.readEntity(String.class);
	    log(rv);
	    assertTrue(r.getStatus() == rcode);
	    return rv;
	}

	private String getToken(String json) {
		WebTarget target = ClientBuilder.newClient().target(TEST_URI).path(LoginAPI.LOGIN_PATH);
		Entity<?> entity = Entity.entity(json, MediaType.APPLICATION_JSON);
		Response r = target
			.request(MediaType.APPLICATION_JSON)
			.header(APIBase.REAL_IP_HDR, "69.69.1.2")
			.buildPost(entity)
			.invoke();
		if (r.getStatus() == HttpServletResponse.SC_CREATED) {
	     String token = r.getHeaderString(APIBase.SESSION_TOKEN_HDR);
			return token;
		}
		return null;
	}

	protected void log(Object s) {
		System.err.println(s.toString());
	}
}
