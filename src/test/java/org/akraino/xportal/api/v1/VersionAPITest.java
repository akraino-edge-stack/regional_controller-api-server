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
import org.akraino.regional_controller.api.v1.VersionAPI;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class VersionAPITest extends BaseAPITest {

	@BeforeClass
    public static void setUp() throws Exception {
        startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        stopServer();
    }

    @Test
	public void testGetVersionsJSON() {
    	testGet(VersionAPI.VERSION_PATH, getLoginCookie(), MediaType.APPLICATION_JSON, HttpServletResponse.SC_OK);
	}

    @Test
	public void testGetVersionsYAML() {
		testGet(VersionAPI.VERSION_PATH, getLoginCookie(), APIBase.APPLICATION_YAML, HttpServletResponse.SC_OK);
	}
}
