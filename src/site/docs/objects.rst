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

.. _objects:

RC Object Model
========================================
The RC performs its work by maintaining an Object Model.
The Object Model consists of a number of objects, described below, which model the
machines, software, and lifecycles of the various Akraino deployments that the RC
controls. The collection of all the objects that the RC maintains in this model,
as well as their relationships is know as the *Akraino Universe*.

Most objects in the Object Model have the following in common:

- A *uuid*, meant to be a unique, internal identifier for the object. The *uuid* is
  assigned by the RC.
- A *name*, meant for human consumption.  The *name* is required, and will normally
  be unique for any class of object.
- A *description*, also for human consumption, to provide more detail about the object.
  The *description* is optional.

Hardware Profiles
-----------------
A Hardware Profile describes a specific model of hardware that will be used within
an Akraino Universe (for example *Dell PowerEdge R740 Gen 14*).  Because the amount
and type of information that can describe a particular piece of hardware can vary widely,
a Hardware Profile provide a *yaml* attribute where this information can be stored.

Nodes
-----------------
A single, identifiable machine.  A node may be unassigned, or it may belong to one
and only one Edge Site. A node must make reference to a hardware profile that
describes the hardware of which the node consists. In addition, each Node has a
*yaml* attribute, which can be used to store other information about the node
(e.g. the nodes lat/long location for use in a GUI, or the rack ID of the rack
the node is mounted on).

EdgeSites
-----------------
A collection of one or more Nodes upon which a POD may be installed.
Any specific Node may belong to one and only one Edge Site.
Edge Sites in turn must belong to at least one, and potentially several *Regions*.

There is no requirement that an Edge Site consist of homogeneous hardware.
However, most Blueprints are likely to require that all nodes in an Edge Site be the
same hardware.  This would be detected at the point in time where a POD is created
(see below).

Regions
-----------------
A region is a grouping mechanism to collect one or more Edge Sites, or other Regions,
together for management purposes.
Regions may be used to group Edge Sites by physical location (e.g. *US NorthEast*)
together. They may also be used to perform logical groupings (e.g. *RAN Sites*).

Regions form a tree, similar to a UNIX filesystem, with the topmost Region being
the *Universal Region*.  A Region may have only one parent Region.
The Universal Region has itself as its parent.

Blueprints
-----------------
A collection of software that can be deployed to an *empty* Edge Site, as well
as the supporting software that the RC uses to manage the lifecycle of the Edge Site.
A Blueprint in the Object Model consists of:

- a *version* attribute, a string in *Semantic Versioning* (X.X.X) form, describing
  the version of the Blueprint. This is required.
- a *yaml* attribute describing everything else.  This is required.
  This is very lightly defined at the moment, but is expected to include:

  - locations (URLs) of various pieces of software required to deploy the Blueprint
  - locations of downloadable workflows to run inside the RC
  - definitions of any hardware requirements that a specific Blueprint may have

An example blueprint (for Rover) is available here (:ref:`exampleblueprint`).

PODs
-----------------
(Point of Delivery) A specific deployment of a *Blueprint* upon an *Edge Site*.
The act of creating a POD, causes the Blueprint to be deployed on the Edge Site.
The Blueprint must be valid for the Hardware Profiles and other characteristics
of the Edge Site, in order to be deployed.  In addition, the Edge Site must not
be in use by another POD at the time this POD is created.  At the time of creation,
the YAML that is uploaded by the user is verified against the schema that is specified
in the Blueprint.  The RC then tells the WorkFlow Engine to deploy the Blueprint
on the Edge Site using, as parameters:

- the UUID of the newly created POD
- the Blueprint
- the definition of the Edge Site in the database
- the YAML content from the POST request
- any other parameters from the POST request

to control the deployment.

Updates to an existing POD are performed via PUT requests to the POD's URL.
There is a separate section in the Blueprint specifying the input schema, workflow, and
data file components required for an update.

Deleting a POD causes the the Blueprint to be removed from the Edge Site, and places
the Edge Site back in an *unused* state.

If any operation (create/update/delete) is missing from the specification in the
Blueprint, the corresponding operation is disallowed by the RC.  Naturally, if
*create* is missing, then the Blueprint can never be deployed.

User
-----------------
An individual user of the RC.  A user is identified by a user name, a password,
and a list of roles.  All RC API operations are logged with an indication of the
user who requested the operation.

Because the user database is likely to be maintained externally (e.g. in an LDAP server
shared with other services), there is no API to perform the CRUD operations on users.

Session
-----------------
An authenticated instance of a user connection to the RC. There may be many Sessions
for one User. Sessions have a limited lifetime.

Almost all operations within the API require a session token, which identifies the user
and the users' roles to the API.  As such, the very first operation a user of the API will
perform will be the *Create Session* (POST /api/v1/login) call in the :ref:`login-api`.
This creates the Session and its corresponding token.

Role
-----------------
A set of functionality that can be assigned to one or more Users.
Roles allow users to perform specific functions within the API.
The roles are hard-coded into the RC, and are not expected to change often, if
at all; as such, there are no CRUD operations defined for the roles.  A user of
the API can discover what roles s/he has been given via the Login API.
