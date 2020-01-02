..
      Copyright (c) 2019 AT&T Intellectual Property. All Rights Reserved.

      Licensed under the Apache License, Version 2.0 (the "License");
      you may not use this file except in compliance with the License.
      You may obtain a copy of the License at

          http://www.apache.org/licenses/LICENSE-2.0

      Unless required by applicable law or agreed to in writing, software
      distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
      WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
      License for the specific language governing permissions and limitations
      under the License.

.. _tables:

RC Database Tables
===========================================
The RC makes use of the following tables to maintain its state.
These tables have a fairly obvious connection to the Object Model.

===============  ===============================================================
Table Name       Description
===============  ===============================================================
BLUEPRINT        The list of Blueprints installed on this RC.
EDGESITE         The list of Edgesites that this RC knows about.
EDGESITE_ARRAYS  A map of Regions and Nodes to Edgesites.
HARDWARE         The list of Hardware profiles that this RC knows about.
NODE             The list of Nodes that this RC knows about.
POD              The list of PODs that this RC knows about.
POD_EVENTS       A list of events that occur per POD during the lifecycle of each POD.
POD_WORKFLOWS    Details about each specific workflow that is run on behalf of a POD.
REGION           The list of Regions that this RC knows about.
ROLES            The roles that are configured in this system.
ROLE_ATTRIBUTES  Role attributes that map to each role in the system.
SESSIONS         The list of user sessions that this RC knows about.
USERS            Entries for each user allowed to access the API.
USER_ROLES       A table to associate roles to each user.
===============  ===============================================================

The USERS table is only used in the database if you are using a database
based user authentication.  If not, then the equivalent data is located on an
LDAP server.  The current LDAP implementation makes the following assumptions:

1. LDAP uses the *inetOrgPerson* objectClass to store users.
2. The user's login name is stored as the *cn* attribute in LDAP.
3. The user's UUID is stored as the *uid* attribute in LDAP.
4. User passwords are stored in the clear (base64 encoded) in LDAP.  The hash of
   the password is computed in the API code.
