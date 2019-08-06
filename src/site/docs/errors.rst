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

.. _errors:

List of API Error Codes
=======================

When an API call fails, it will return a JSON object describing the error similar to the
following:

.. code-block:: json

  {
    "errorUrl":"/docs/errors.html#arc-4001",
    "code":404,
    "errorId":"ARC-4001",
    "message":"object not found"
  }

The *errorId* field refers to a section below.

ARC-1001
--------

When creating a Node, the UUID used to identify the Hardware Profile of the node does
not refer to a valid Hardware Profile.

ARC-1002
--------

There was a problem parsing the JSON content of a request. Verify that the content is
valid JSON.

ARC-1003
--------

When creating or modifying an Edgesite, the UUID used to identify one of the Nodes does
not refer to a valid Node.

ARC-1004
--------

When creating or modifying an Edgesite, the UUID used to identify one of the Regions does
not refer to a valid Region.

ARC-1006
--------

When creating a PODEvent, the timestamp passed in the *time* field could not be parsed.
It must be passed in the format *yyyy-MM-dd HH:mm:ss*

ARC-1007
--------

An attempt was made to create a PODEvent with no POD UUID specified. This field is
required.

ARC-1008
--------

An attempt was made to create a Blueprint with no YAML field specified. This field is
required.

ARC-1009
--------

An attempt was made to create a POD with no Blueprint UUID specified. This field is
required.

ARC-1010
--------

An attempt was made to create a POD with no Edgesite UUID specified. This field is
required.

ARC-1011
--------

An attempt was made to create a PODEvent with no *level* specified. This field is
required.

ARC-1012
--------

An attempt was made to create a PODEvent with no *message* specified. This field is
required.

ARC-1013
--------

When creating an object, the *name* field was missing from the input YAML.

ARC-1014
--------

When using the Login API, the *password* field was missing from the input YAML.

ARC-1015
--------

There were no regions listed when attempting to create an Edgesite.
At least one region must be provided.

ARC-1016
--------

When creating a Blueprint, the *blueprint* (schema version) field was missing from the
input YAML.

ARC-1017
--------

When creating a Blueprint, the *version* field was missing from the input YAML.

ARC-1018
--------

When creating a PODEvent, the POD UUID was invalid.

ARC-1019
--------

An attempt was made to create or modify an Edgesite with no nodes listed.
An Edgesite must contain at least one Node.

ARC-1020
--------

An attempt was made to create or modify an Edgesite with a node that is already the member
of another Edgesite. A Node may exist in at most one Edgesite at a time.

ARC-1022
--------

Because required information was missing, the Region was not created.

ARC-1023
--------

When creating a new user using the User API, the requesting user did not possess the roles
required to give the new user the specific role.  Requesting users may not create new users
with roles that they themselves do not possess.

ARC-1024
--------

When creating a Blueprint, the *blueprint* (schema version) field in the input YAML
contained an invalid value.  Presently the only valid value is *1.0.0*.

ARC-1025
--------

The Blueprint specified to use when creating a POD does not exist.

ARC-1026
--------

The Edgesite specified to use when creating a POD does not exist.

ARC-1027
--------

When creating an object with a pre-specified UUID, another existing object with the same
UUID was found.  It is possible the object was already created by a separate call to the API.

ARC-1028
--------

The UUID of an object was not specified, or was null, when it was required.

ARC-1030
--------

This indicates an internal error constructing a URL -- notify the developer.

ARC-1031
--------

When creating a Blueprint, an invalid workflow name was detected.
Workflow names must be alphanumeric, and no longer than 36 characters long.

ARC-1032
--------

When creating a User, an invalid password was detected.  Passwords must contain
alphanumerics, spaces or dashes, and be a minimum of 22 characters long.

ARC-2001
--------

This Blueprint cannot be DELETE-ed because it is still in use by POD listed in the message.

ARC-2002
--------

This Edgesite cannot be DELETE-ed because it is still in use by POD listed in the message.
Delete the POD first.

ARC-2003
--------

This Node cannot be DELETE-ed because it is still in use by Edgesite listed in the message.
Delete or modify the Edgesite first.

ARC-2004
--------

This Region cannot be DELETE-ed because it is still in use by Edgesite listed in the message.
Delete or modify the Edgesite first.

ARC-2005
--------

This Region cannot be DELETE-ed because it is still in use by another Region (it is an
ancestor).  Delete or modify all dependent regions first.

ARC-2006
--------

This hardware profile cannot be DELETE-ed because it is still in use by Node listed in the
message.  Delete the Node first.

ARC-2007
--------

A PUT or POST request was made with an unsupported media type.
This should not happen -- notify the developer.

ARC-3001
--------

An attempt was made to set the parent of a region to a non-existing region.
No regions exists with requested UUID.

ARC-3002
--------

You are not allowed to modify the UUID of a Blueprint.

ARC-3003
--------

You are not allowed to modify the YAML field of a Blueprint.
Instead, create a new Blueprint with the new YAML.

ARC-3004
--------

You are not allowed to modify the name of a Blueprint after it has been created.

ARC-3005
--------

You are not allowed to modify the version of a Blueprint after it has been created.

ARC-3006
--------

You are not allowed to modify the UUID of an Edgesite.

ARC-3007
--------

You are not allowed to modify the UUID of a Hardware Profile.

ARC-3008
--------

You are not allowed to modify the UUID of a Node.

ARC-3009
--------

You are not allowed to modify the UUID of a POD.

ARC-3010
--------

You are not allowed to modify the YAML of a POD.

ARC-3011
--------

You are not allowed to modify the Blueprint of a POD, unless the new Blueprint is derived
from the old one.

ARC-3012
--------

You are not allowed to modify the Edgesite of a POD.

ARC-3013
--------

You are not allowed to modify the name of a POD.

ARC-3014
--------

You are not allowed to modify the state of a POD via direct access to the POD.
POD states are changed solely by issuing appropriately formatted PODEvents referring
to the POD.

ARC-3015
--------

You are not allowed to modify the UUID of a Region.

ARC-3016
--------

You are not allowed to modify any attributes of the Universal region.  This reqion (with
UUID *00000000-0000-0000-0000-000000000000*) is hard-wired into the Regional Controller.

ARC-3017
--------

You are not allowed to modify the UUID of a User object.

ARC-3018
--------

You are not allowed to modify the name of a User object.

ARC-3019
--------

You are not allowed to modify the YAML for a Hardware profile that is in use.

ARC-3020
--------

You are not allowed to modify the YAML for a Node that is in use.
The specified Node must be associated with an active Edgesite.

ARC-3021
--------

An attempt was made to perform an operation that the RBAC (Role-based access control)
does not allow based upon your login.

ARC-3022
--------

You are not allowed to make the parent of a region be itself.
Only the universal region may have itself as its parent.

ARC-4001
--------

An attempt was made to perform a PUT or DELETE operation upon an object that does not
exist (invalid UUID).

ARC-4002
--------

PODWorkflow objects do not have descriptions; hence you may not change the description of
a PODWorkflow.

ARC-4003
--------

An internal database error has occurred -- notify the developer.

ARC-4004
--------

A workflow is currently running on the POD specified in the request.  You may not start a
new workflow by PUT-ing to the workflow's URL, or delete a POD, while there is a workflow
running.

ARC-4005
--------

The POD specified in the request is in either the DEAD or ZOMBIE state.  You may not start
a new workflow by PUT-ing to the workflow's URL, or delete a POD, after the POD is in a
DEAD or ZOMBIE state.

ARC-4006
--------

You cannot run the *create* or *delete* workflows via this API call (by issuing a PUT on
the workflow URL).   These must be invoked via the POST or DELETE HTTP methods.

ARC-4007
--------

The user is not authorized to perform the requested operation, either because no API token
was provided, or the token is for a user who does not have the required RBAC roles and
role attributes required for the operation.

ARC-4008
--------

The user is not authorized to perform the requested operation because the session token
has expired.  The user needs to obtain a new token by using the :ref:`login-api`.

ARC-4011
--------

You cannot change the name of a PODWorkflow.

ARC-9999
--------

An exception was caught that does not have an assigned error code -- notify the developer.
