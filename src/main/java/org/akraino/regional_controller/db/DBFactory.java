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

package org.akraino.regional_controller.db;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.akraino.regional_controller.beans.Blueprint;
import org.akraino.regional_controller.beans.Edgesite;
import org.akraino.regional_controller.beans.Node;
import org.akraino.regional_controller.beans.Region;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The DBFactory class builds a DB provider based upon properties file settings.
 */
public class DBFactory {
	public static final String PROPERTIES_FILE = "api.properties";
	private static final Logger logger = LogManager.getLogger();

	private static DB singleton = null;

	public static synchronized DB getDB() {
		if (singleton == null) {
			singleton = getDBinternal();
		}
		return singleton;
	}
	private static DB getDBinternal() {
		Properties props = new Properties();
		InputStream is = null;
		try {
			is = DBFactory.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE);
			props.load(is);
			String flav = props.getProperty("db.flavor", "StandardDB");
			switch (flav) {
			case "StandardDB":
			case "org.akraino.regional_controller.db.StandardDB":
				return new StandardDB(props);

			case "StandardDBWithLDAP":
			case "org.akraino.regional_controller.db.StandardDBWithLDAP":
				return new StandardDBWithLDAP(props);

			case "PropertiesDB":
			case "org.akraino.regional_controller.db.PropertiesDB":
			default:
				return new PropertiesDB(props);
			}
		} catch (IOException e) {
			logger.error(e);
			return null;
		} catch (ClassNotFoundException e) {
			logger.error(e);
			return null;
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
	}

	private DBFactory() {}

	public static void main(String[] a) {
		DB db = DBFactory.getDB();
		if (db == null) {
			DBFactory.logger.error("Cannot get DB!");
			System.exit(1);
		}
		for (Blueprint bp : db.getBlueprints()) {
			DBFactory.logger.info(bp);
		}
		DBFactory.logger.info("");
		for (Edgesite e : db.getEdgesites()) {
			DBFactory.logger.info(e);
		}
		DBFactory.logger.info("");
		for (Node n : db.getNodes()) {
			DBFactory.logger.info(n);
		}
		DBFactory.logger.info("");
		for (Region r : db.getRegions()) {
			DBFactory.logger.info(r);
		}
		DBFactory.logger.info("");
		DBFactory.logger.info(db.getUser("admin"));

		DBFactory.logger.info(System.currentTimeMillis());
	}
}
