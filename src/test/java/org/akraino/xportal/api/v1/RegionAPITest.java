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

import org.akraino.regional_controller.api.v1.APIBase;
import org.akraino.regional_controller.api.v1.RegionAPI;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class RegionAPITest extends BaseAPITest {

	@BeforeClass
    public static void setUp() throws Exception {
        startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        stopServer();
    }

	@Test
	public void testCreateRegionJSON() {
		String json = "{ \"name\": \"new_region\", \"description\": \"testing only\" }";
		testPost(RegionAPI.REGION_PATH, getLoginCookie(), MediaType.APPLICATION_JSON, json, MediaType.APPLICATION_JSON, HttpServletResponse.SC_CREATED);
	}

	@Test
	public void testCreateRegionJSONBadContent() {
		String bad_json = "{ \"namexx\": \"badregion\", \"description\": \"testing only\" }";
		testPost(RegionAPI.REGION_PATH, getLoginCookie(), MediaType.APPLICATION_JSON, bad_json, MediaType.APPLICATION_JSON, HttpServletResponse.SC_BAD_REQUEST);
	}

	@Test
    public void testGetRegionJSON() {
    	testGet(RegionAPI.REGION_PATH, getLoginCookie(), MediaType.APPLICATION_JSON, HttpServletResponse.SC_OK);
    }

	@Test
    public void testGetRegionYAML() {
    	testGet(RegionAPI.REGION_PATH, getLoginCookie(), APIBase.APPLICATION_YAML, HttpServletResponse.SC_OK);
    }

	@Test
    public void testGetRegionXML() {
    	testGet(RegionAPI.REGION_PATH, getLoginCookie(), MediaType.APPLICATION_XML, HttpServletResponse.SC_NOT_ACCEPTABLE);
    }

	@Test
    public void testGetRegionNoAccess() {
    	testGet(RegionAPI.REGION_PATH, getNoAccessLoginCookie(), MediaType.APPLICATION_JSON, HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    public void testGetSpecificRegion() {
        String msg = testGet(RegionAPI.REGION_PATH, getLoginCookie(), MediaType.APPLICATION_JSON, HttpServletResponse.SC_OK);
        log(msg);
        JSONObject jo = new JSONObject(new JSONTokener(msg));
        JSONArray regions = jo.getJSONArray("regions");
        String region_id = regions.getJSONObject(0).getString("uuid");

        // Test get a good Hardware UUID
        testGet(RegionAPI.REGION_PATH + "/" + region_id, getLoginCookie(), MediaType.APPLICATION_JSON, HttpServletResponse.SC_OK);
    }

    @Test
    public void testGetSpecificRegionNoAccess() {
        String msg = testGet(RegionAPI.REGION_PATH, getLoginCookie(), MediaType.APPLICATION_JSON, HttpServletResponse.SC_OK);
        log(msg);
        JSONObject jo = new JSONObject(new JSONTokener(msg));
        JSONArray regions = jo.getJSONArray("regions");
        String region_id = regions.getJSONObject(0).getString("uuid");

        // Test get a good Hardware UUID, but with no access login
        testGet(RegionAPI.REGION_PATH + "/" + region_id, getNoAccessLoginCookie(), MediaType.APPLICATION_JSON, HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    public void testGetSpecificBadRegion() {
        // Get a bad region
        testGet(RegionAPI.REGION_PATH + "/BADBADBAD", getLoginCookie(), MediaType.APPLICATION_JSON, HttpServletResponse.SC_NOT_FOUND);
    }
}
