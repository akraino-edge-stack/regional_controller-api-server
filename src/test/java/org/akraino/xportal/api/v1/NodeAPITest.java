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
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.akraino.regional_controller.api.v1.APIBase;
import org.akraino.regional_controller.api.v1.NodeAPI;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class NodeAPITest extends BaseAPITest {

	@BeforeClass
    public static void setUp() throws Exception {
        startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        stopServer();
    }

    @Test
	public void testGetNodes() {
    	String token = getLoginCookie();
        WebTarget target = ClientBuilder.newClient().target(TEST_URI).path(NodeAPI.NODE_PATH);
        Response r = target
           	.request(MediaType.APPLICATION_JSON)
           	.header(APIBase.SESSION_TOKEN_HDR, token)
           	.header(APIBase.REAL_IP_HDR, "69.69.1.2")
            .buildGet()
            .invoke();
        log(r);
        log(r.readEntity(String.class));

        assertTrue(r.getStatus() == HttpServletResponse.SC_OK);
	}

	@Test
	public void testGetNodeByUUID() {
//	   	Cookie cookie = getLoginCookie();
//        WebTarget target = ClientBuilder.newClient().target(TEST_URI).path(NodeAPI.NODE_PATH);
//        Response r = target
//           	.request(MediaType.APPLICATION_JSON)
//           	.cookie(cookie)
//            .buildGet()
//            .invoke();
//        String msg = r.readEntity(String.class);
//        JSONObject jo = new JSONObject(new JSONTokener(msg));
//        JSONArray regions = jo.getJSONArray("regions");
//        String region_id = regions.getJSONObject(0).getString("uuid");
//
//        // Get a good region
//        target = ClientBuilder.newClient().target(TEST_URI).path(RegionAPI.REGION_PATH).path(region_id);
//        r = target
//           	.request(MediaType.APPLICATION_JSON)
//           	.cookie(cookie)
//            .buildGet()
//            .invoke();
//        log(r);
//        assertTrue(r.getStatus() == HttpServletResponse.SC_OK);
//
//        // Get a bad region
//        target = ClientBuilder.newClient().target(TEST_URI).path(RegionAPI.REGION_PATH).path("BADBADBAD");
//        r = target
//           	.request(MediaType.APPLICATION_JSON)
//           	.cookie(cookie)
//            .buildGet()
//            .invoke();
//        log(r);
//        assertTrue(r.getStatus() == HttpServletResponse.SC_NOT_FOUND);
	}

}
