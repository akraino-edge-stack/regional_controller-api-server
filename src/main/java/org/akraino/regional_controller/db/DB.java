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
import org.akraino.regional_controller.beans.PODWorkflow;
import org.akraino.regional_controller.beans.Region;
import org.akraino.regional_controller.beans.User;
import org.akraino.regional_controller.beans.UserSession;

public interface DB {
	// BLUEPRINTS
	public void createBlueprint(final Blueprint b) throws SQLException;
	public List<Blueprint> getBlueprints();
	public void updateBlueprint(final Blueprint b) throws SQLException;
	public void deleteBlueprint(final Blueprint b) throws SQLException;

	// EDGESITES
	public void createEdgesite(final Edgesite e) throws SQLException;
	public List<Edgesite> getEdgesites();
	public void updateEdgesite(final Edgesite e) throws SQLException;
	public void deleteEdgesite(final Edgesite e) throws SQLException;

	// HARDWARE
	public void createHardware(final Hardware h) throws SQLException;
	public List<Hardware> getHardware();
	public void updateHardware(final Hardware h) throws SQLException;
	public void deleteHardware(final Hardware h) throws SQLException;

	// NODES
	public void createNode(final Node n) throws SQLException;
	public List<Node> getNodes();
	public void updateNode(final Node n) throws SQLException;
	public void deleteNode(final Node n) throws SQLException;

	// PODS
	public void createPod(final POD p) throws SQLException;
	public List<POD> getPods();
	public void updatePod(final POD p) throws SQLException;
	public void deletePod(final POD p) throws SQLException;

	// PODS_EVENTS
	public void createPodEvent(final PODEvent pe) throws SQLException;
	public List<PODEvent> getPODEvents(final String uuid);

	// PODS_WORKFLOWS
	public void createPodWorkflow(final PODWorkflow pw) throws SQLException;
	public List<PODWorkflow> getPODWorkflows(final String uuid);
	public void updatePodWorkflow(final PODWorkflow pw) throws SQLException;

	// REGIONS
	public void createRegion(final Region r) throws SQLException;
	public List<Region> getRegions();
	public void updateRegion(final Region r) throws SQLException;
	public void deleteRegion(final Region r) throws SQLException;

	// SESSIONS
	public void createSession(final UserSession us);
	public UserSession getSession(final String token);
	public void invalidateSession(final UserSession us);

	// USERS
	public void createUser(final User u) throws SQLException;
	public User getUser(final String name);
	public List<User> getUsers();
	public void updateUser(final User u) throws SQLException;
	public void deleteUser(final User u) throws SQLException;
}
