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

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;

import org.akraino.regional_controller.api.v1.UserAPI;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class UserAPITest extends BaseAPITest {

	@BeforeClass
    public static void setUp() throws Exception {
        startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        stopServer();
    }

    @Test
    public void testGetUsers() {
    	testGet(UserAPI.USER_PATH, getLoginCookie(), MediaType.APPLICATION_JSON, HttpServletResponse.SC_OK);
    }

    @Test
    public void testGetUsersNoAccess() {
    	testGet(UserAPI.USER_PATH, getNoAccessLoginCookie(), MediaType.APPLICATION_JSON, HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    public void testGetSpecificUser() {
    	String msg = testGet(UserAPI.USER_PATH, getLoginCookie(), MediaType.APPLICATION_JSON, HttpServletResponse.SC_OK);
        log(msg);
        JSONObject jo = new JSONObject(new JSONTokener(msg));
        JSONArray users = jo.getJSONArray("users");
        String user_id = users.getJSONObject(0).getString("uuid");

        // Test get a good User UUID
        testGet(UserAPI.USER_PATH + "/" + user_id, getLoginCookie(), MediaType.APPLICATION_JSON, HttpServletResponse.SC_OK);
    }

    @Test
    public void testGetBadUser() {
    	testGet(UserAPI.USER_PATH + "/BADBADBAD", getLoginCookie(), MediaType.APPLICATION_JSON, HttpServletResponse.SC_NOT_FOUND);
    }
}
