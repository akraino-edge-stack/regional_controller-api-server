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
import static org.junit.Assert.fail;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.akraino.regional_controller.api.v1.APIBase;
import org.akraino.regional_controller.api.v1.LoginAPI;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class LoginAPITest extends BaseAPITest {

	@BeforeClass
    public static void setUp() throws Exception {
        startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        stopServer();
    }

	// Test LoginAPI with good credentials
    @Test
    public void testValidLogin() {
        WebTarget target = getTarget();
        Entity<?> entity = Entity.entity(VALID_LOGIN_JSON, MediaType.APPLICATION_JSON);
        Response r = target
           	.request(MediaType.APPLICATION_JSON)
           	.buildPost(entity)
        	.invoke();
        // Expect 201 CREATED, with cookie
        log(r);

        assertTrue(r.getStatus() == HttpServletResponse.SC_CREATED);
        String token = r.getHeaderString(APIBase.SESSION_TOKEN_HDR);
        if (token == null) {
        	fail("no token!");
        }
        log("token = "+token);
    }

	// Test LoginAPI with good YAML credentials
    @Test
    public void testValidLoginYAML() {
        WebTarget target = getTarget();
        Entity<?> entity = Entity.entity(VALID_LOGIN_YAML, APIBase.APPLICATION_YAML);
        Response r = target
        	.request(APIBase.APPLICATION_YAML)
        	.buildPost(entity)
        	.invoke();
        // Expect 201 CREATED, with cookie
        log(r);

        assertTrue(r.getStatus() == HttpServletResponse.SC_CREATED);
        String token = r.getHeaderString(APIBase.SESSION_TOKEN_HDR);
        if (token == null) {
        	fail("no token!");
        }
        log("token = "+token);
    }

	// Test LoginAPI with bad credentials
    @Test
    public void testInvalidLogin() {
        WebTarget target = getTarget();
        Entity<?> entity = Entity.entity(INVALID_LOGIN_JSON, MediaType.APPLICATION_JSON);
        Response r = target
       		.request(MediaType.APPLICATION_JSON)
        	.buildPost(entity)
        	.invoke();
        // Expect 404 NOT FOUND, with no cookie
        log(r);

        assertTrue(r.getStatus() == HttpServletResponse.SC_NOT_FOUND);
        String token = r.getHeaderString(APIBase.SESSION_TOKEN_HDR);
        if (token != null) {
        	fail("unexpected token!");
        }
        log("token = "+token);
    }

    // Test LoginAPI with bad content type
    @Test
    public void testLoginBadContent() {
        WebTarget target = getTarget();
        Entity<?> entity = Entity.form(new Form()
    		.param("name", "admin")
    		.param("password", "admin123")
    	);
        Response r = target
       		.request(MediaType.APPLICATION_JSON)
        	.buildPost(entity)
        	.invoke();
        // Expect 415 SC_UNSUPPORTED_MEDIA_TYPE
        log(r);

        assertTrue(r.getStatus() == HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
    }

	// Test getroles works when logged in
    @Test
    public void testGetRoles() {
        WebTarget target = getTarget();
        Entity<?> entity = Entity.entity(VALID_LOGIN_JSON, MediaType.APPLICATION_JSON);
        Response r = target
       		.request(MediaType.APPLICATION_JSON)
        	.buildPost(entity)
        	.invoke();
        assertTrue(r.getStatus() == HttpServletResponse.SC_CREATED);
        String token = r.getHeaderString(APIBase.SESSION_TOKEN_HDR);
        if (token == null) {
        	fail("unexpected token!");
        }
        r = target
           	.request(MediaType.APPLICATION_JSON)
           	.header(APIBase.SESSION_TOKEN_HDR, token)
           	.header(APIBase.REAL_IP_HDR, "69.69.1.2")
            .buildGet()
            .invoke();
        log(r);
        log(r.readEntity(String.class));

        assertTrue(r.getStatus() == HttpServletResponse.SC_OK);
    }

    // Test get roles works when not logged in
    @Test
    public void testGetRolesNotLoggedin() {
    	testGet(LoginAPI.LOGIN_PATH, null, MediaType.APPLICATION_JSON, HttpServletResponse.SC_UNAUTHORIZED);
    }

    // Test logout works
    @Test
    public void testLogout() {
    	testDelete(LoginAPI.LOGIN_PATH, getLoginCookie(), HttpServletResponse.SC_OK);
    }

    @Test
    public void testLogoutBadToken() {
    	testDelete(LoginAPI.LOGIN_PATH, "faketoken", HttpServletResponse.SC_UNAUTHORIZED);
    }

    private WebTarget getTarget() {
        return ClientBuilder.newClient().target(TEST_URI).path(LoginAPI.LOGIN_PATH);
    }
}
// https://dzone.com/articles/how-test-rest-api-junit
// https://github.com/zapodot/embedded-db-junit
// https://github.com/zapodot/embedded-ldap-junit
