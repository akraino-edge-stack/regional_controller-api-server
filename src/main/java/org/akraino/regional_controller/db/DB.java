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

import java.sql.SQLException;
import java.util.List;

import org.akraino.regional_controller.beans.Blueprint;
import org.akraino.regional_controller.beans.Edgesite;
import org.akraino.regional_controller.beans.Hardware;
import org.akraino.regional_controller.beans.Node;
import org.akraino.regional_controller.beans.POD;
import org.akraino.regional_controller.beans.PODEvent;
import org.akraino.regional_controller.beans.Region;
import org.akraino.regional_controller.beans.User;
import org.akraino.regional_controller.beans.UserSession;

public interface DB {
	// BLUEPRINTS
	public void createBlueprint(final Blueprint b) throws SQLException;
	public List<Blueprint> getBlueprints();
	public void deleteBlueprint(Blueprint b) throws SQLException;

	// EDGESITES
	public void createEdgesite(final Edgesite e) throws SQLException;
	public List<Edgesite> getEdgesites();
	public void deleteEdgesite(Edgesite e) throws SQLException;

	// HARDWARE
	public void createHardware(final Hardware h) throws SQLException;
	public List<Hardware> getHardware();
	public void deleteHardware(Hardware h) throws SQLException;

	// NODES
	public void createNode(final Node n) throws SQLException;
	public List<Node> getNodes();
	public void deleteNode(final Node n) throws SQLException;

	// PODS
	public void createPod(final POD p) throws SQLException;
	public List<POD> getPods();
	public void updatePod(final POD p) throws SQLException;
	public void deletePod(final POD p) throws SQLException;

	// PODS_EVENTS
	public void createPodEvent(final PODEvent pe) throws SQLException;
	public List<PODEvent> getPODEvents(String uuid);

	// REGIONS
	public void createRegion(final Region r) throws SQLException;
	public List<Region> getRegions();
	public void deleteRegion(final Region r) throws SQLException;

	// SESSIONS
	public void createSession(final UserSession us);
	public UserSession getSession(final String token);
	public void invalidateSession(final UserSession us);

	// USERS
	public User getUser(final String name);
}
