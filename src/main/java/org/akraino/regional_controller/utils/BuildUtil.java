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

package org.akraino.regional_controller.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BuildUtil {
	private static final String PROPERTIES_FILE = "api.properties";
	private static final Logger logger = LogManager.getLogger();
	private static String buildVersion = "";
	private static String buildDate = "";

	private BuildUtil() {}

	/**
	 * Get the version of this ARC build build.
	 * @return the version
	 */
	public static String getVersion() {
		if (buildVersion.equals("")) {
			readBuildProperties();
		}
		return buildVersion;
	}
	/**
	 * Get the build date of this ARC build.
	 * @return the build date
	 */
	public static String getBuildDate() {
		if (buildDate.equals("")) {
			readBuildProperties();
		}
		return buildDate;
	}
	private static void readBuildProperties() {
		Properties props = new Properties();
		try (InputStream is = BuildUtil.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
			props.load(is);
			buildVersion = props.getProperty("build.version", "**unknown**");
			buildDate    = props.getProperty("build.date", "**unknown**");
		} catch (IOException e) {
			logger.error(e);
		}
	}
	/**
	 * A hack to squirrel away the server URL from the first HTTP request we get,
	 * if we do not already have a server URL property defined.  This is needed to
	 * correctly build URLs for some of the REST requests.
	 * @param request the HTTP request
	 */
//	public static void saveRequestUrl(HttpServletRequest request) {
//		if (props.getProperty("application.server_url") == null) {
//			String host  = request.getHeader("Host");
//			String proto = request.isSecure() ? "https://" : "http://";
//			if (host != null && !host.startsWith("127.0.0.1")) {
//				props.put("application.server_url", proto + host);
//			} else {
//				String name = request.getLocalName();
//				int port    = request.getLocalPort();
//				if (!name.startsWith("127.0.0.1")) {
//					props.put("application.server_url", String.format("%s%s:%d", proto, name, port));
//				}
//			}
//		}
//	}
}
