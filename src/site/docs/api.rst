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

.. _api:

RC API - Version 1
===============================
The Akraino Regional Controller API is a RESTful interface used for accessing the
services provided by the Akraino Regional Controller (the RC).
For version 1 of the API, all endpoints are located under ``/api/v1/``.

The API runs on a CRUD model; Create (POST), Read (GET), Update (PUT), Delete (DELETE)
operations may be performed on high level objects within the Regional Controller's database.
Not all operations may be allowed for all objects.

The objects that the API manipulate are defined on the :ref:`objects` page.

All content provided to the API must be in either YAML form (with a Content-Type
of ``application/yaml``), or JSON form (with a Content-Type of ``application/json``).
All output provided by the API will be in YAML form (with a Content-Type
of ``application/yaml``), or JSON form (with a Content-Type of ``application/json``),
depending upon the setting of the request ``Accept:`` header.

The API will support an RBAC model, to restrict its capabilities to those with
the proper role. All APIs, with the exception of the Login API, require that the
user have the correct role attributes to perform the operation.

In general, the APIs fall into one of three categories:

- The Login API, which is concerned solely with user authentication and session management.
- The POD API, which is the sole API that can cause workflows to be scheduled and
  actions to occur on clusters outside of the RC.
- All remaining APIs are used to manipulate the object model.

.. _login-api:

Login API
---------
The Login API allows a user to identify themselves, and determine the roles enabled
for their ID. A successful login is required before any other APIs are used.
The token returned from the login call identifies the user's Session, and is
**required** for all other calls in the API.

POST /api/v1/login/
^^^^^^^^^^^^^^^^^^^

Provide a user name and password.  If successful, a token is returned which
identifies the user Session to be used for all other API calls.  The token will be
returned in the ``X-ARC-Token:`` header, which is the same header that should be included
in all following API calls.

Sample JSON input content:

.. code-block:: json

  {
    "name": "admin",
    "password": "abc123"
  }

The token that is returned should be provided to all other API calls in the
``X-ARC-Token:`` header.

===========  ======================================================================
Return Code  Reason
===========  ======================================================================
201          Successful login.
400          Invalid content supplied.
500          Internal error.
===========  ======================================================================

GET /api/v1/login/
^^^^^^^^^^^^^^^^^^

Retrieve information about the logged in user, including the roles and role attributes
assigned to the user.

In JSON:

.. code-block:: json

  {
    "expiration_time": "2019-04-21 15:51:45",
    "expires": 1555861905218,
    "token": "YWRtaW4gICAgICAgIDE1NTU4NTgzMDUyMTdjMGE4MDFiMQ==",
    "user": {
      "uuid": "9ef95ad2-3150-11e9-98b6-0017f20dbff8",
      "name": "admin",
      "description": "Joe Q. Administrator",
      "roles": [
        {
          "attributes": [ "create-*", "delete-*", "read-*", "update-*" ],
          "description": "Full read/write access to the RC",
          "name": "fullaccess",
          "uuid": "0b4fb8e4-445b-11e9-b55e-3b725c041ee5"
        }
      ]
    }
  }

In YAML:

.. code-block:: yaml

  expires: 1555861905218
  expiration_time: '2019-04-21 15:51:45'
  user:
    roles:
    - name: fullaccess
      description: Full read/write access to the RC
      attributes: [create-*, delete-*, read-*, update-*]
      uuid: 0b4fb8e4-445b-11e9-b55e-3b725c041ee5
    name: admin
    description: Full Access Admin
    uuid: 9ef95ad2-3150-11e9-98b6-0017f20dbff8
  token: YWRtaW4gICAgICAgIDE1NTU4NTgzMDUyMTdjMGE4MDFiMQ==

===========  ======================================================================
Return Code  Reason
===========  ======================================================================
200          Successful execution.
401          Invalid session or session token.
===========  ======================================================================

DELETE /api/v1/login/
^^^^^^^^^^^^^^^^^^^^^

Invalidate the token provided by the API, and the Session tied to the token.

===========  ======================================================================
Return Code  Reason
===========  ======================================================================
200          Successful execution.
401          Invalid session or session token.
===========  ======================================================================

.. hardware-api:

Hardware Profile API
--------------------
The Hardware API is used to maintain the list of Hardware Profiles available in
the system.  Each Hardware Profile identifies one specific type of hardware that
the RC may use.

POST /api/v1/hardware
^^^^^^^^^^^^^^^^^^^^^

Create a new Hardware Profile.  The request is required to provide at least a *name*
(the *description* and *yaml* fields are optional).

.. code-block:: yaml

  name: Nixdorf 8870
  description: Nixdorf system family 8870
  yaml:
    rack: 5
    notes: good luck getting anything working on this!

===========  ======================================================================
Return Code  Reason
===========  ======================================================================
201          Hardware Profile created.
400          Invalid content supplied.
401          Invalid session or session token.
403          User does not have RBAC rights to create a Hardware Profile.
===========  ======================================================================

GET /api/v1/hardware
^^^^^^^^^^^^^^^^^^^^

Return a list of all Hardware Profiles in the system.

.. code-block:: yaml

  hardware:
  - name: Dell PowerEdge R740 Gen 14
    description: Standard Dell configuration for Rover/Unicycle
    uuid: c1dfa1ac-53e0-11e9-86c2-c313482f1fdb
    url: /api/v1/hardware/c1dfa1ac-53e0-11e9-86c2-c313482f1fdb
    yaml:
      disk: [4x480G SSD, 6x2.4T HDD]
      ps: 2
      cpu: 2x22 Cores @ 2.1GHz Skylake 6152 CPU
      nic: [2x1G LOM Intel 5xx, 2x25G PCI3 Intel 710]
      lom: 4x10G Intel 710
      ram: 12x32GB
  - name: HPE DL380 Gen 10
    description: Standard HPE configuration for Rover/Unicycle
    uuid: c7d2ce90-53e0-11e9-b15e-f72b1e9c4a2c
    url: /api/v1/hardware/c7d2ce90-53e0-11e9-b15e-f72b1e9c4a2c
    yaml:
      disk: [4x480G SSD, 6x2.4T HDD]
      ps: 2
      cpu: 2x22 Cores @ 2.1GHz Skylake 6152 CPU
      nic: [2x1G LOM Intel 5xx, 2x25G PCI3 Intel 710]
      lom: 4x10G Intel 710
      ram: 12x32GB

===========  ======================================================================
Return Code  Reason
===========  ======================================================================
200          Successful execution.
401          Invalid session or session token.
403          User does not have RBAC rights to read the content.
===========  ======================================================================

GET /api/v1/hardware/``{uuid}``
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Returns the specific Hardware Profile identified by ``uuid``.

===========  ======================================================================
Return Code  Reason
===========  ======================================================================
200          Successful execution.
400          The UUID provided is not a valid UUID.
401          Invalid session or session token.
403          User does not have RBAC rights to read the content.
404          The UUID provided does not refer to a Hardware Profile.
===========  ======================================================================

PUT /api/v1/hardware/``{uuid}``
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Update the Hardware Profile identified by ``uuid``.
The user is allowed to modify the name, description and yaml fields of the Hardware Profile.
The *yaml* field may not be modified if the Hardware Profile is in use by a Node.
The content provided to the PUT operation, in either YAML or JSON form, should consist
of just those fields to be modified.

===========  ======================================================================
Return Code  Reason
===========  ======================================================================
200          Successful execution.
400          The UUID provided is not a valid UUID.
401          Invalid session or session token.
403          User does not have RBAC rights to modify the content.
404          The UUID provided does not refer to a Hardware Profile.
409          The object is in use and cannot be modified.
===========  ======================================================================

DELETE /api/v1/hardware/``{uuid}``
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Delete the Hardware Profile identified by ``uuid``.  The Hardware Profile may
not be referred to by any Node in the system.

===========  ======================================================================
Return Code  Reason
===========  ======================================================================
200          Successful execution.
400          The UUID provided is not a valid UUID.
401          Invalid session or session token.
403          User does not have RBAC rights to delete the content.
404          The UUID provided does not refer to a Hardware Profile.
409          The object is in use and cannot be deleted.
===========  ======================================================================

.. _node-api:

Node API
--------
The Node API is used to maintain the list of Nodes (i.e. machines) available in
the system.

POST /api/v1/node
^^^^^^^^^^^^^^^^^

Create a new Node.  The request is required to provide at least a *name*, and
a Hardware Profile UUID (in the *hardware* field). The *description* and *yaml*
fields are optional.

.. code-block:: yaml

  name: aknode99
  description: The 99th Aknode
  hardware: c1dfa1ac-53e0-11e9-86c2-c313482f1fdb
  yaml:
    rack: 9

===========  ======================================================================
Return Code  Reason
===========  ======================================================================
201          Node created.
400          Invalid content supplied.
401          Invalid session or session token.
403          User does not have RBAC rights to create a Node.
===========  ======================================================================

GET /api/v1/node
^^^^^^^^^^^^^^^^

Return a list of all Nodes in the system.

===========  ======================================================================
Return Code  Reason
===========  ======================================================================
200          Successful execution.
401          Invalid session or session token.
403          User does not have RBAC rights to read the content.
===========  ======================================================================

GET /api/v1/node/``{uuid}``
^^^^^^^^^^^^^^^^^^^^^^^^^^^

Returns the specific Node identified by ``uuid``.

===========  ======================================================================
Return Code  Reason
===========  ======================================================================
200          Successful execution.
400          The UUID provided is not a valid UUID.
401          Invalid session or session token.
403          User does not have RBAC rights to read the content.
404          The UUID provided does not refer to a Node.
===========  ======================================================================

PUT /api/v1/node/``{uuid}``
^^^^^^^^^^^^^^^^^^^^^^^^^^^

Update the Node identified by ``uuid``.
The user is allowed to modify the name, description, and yaml fields of the Node.
The *yaml* field may not be modified if the Node is in use by an Edgesite.
The content provided to the PUT operation, in either YAML or JSON form, should consist
of just those fields to be modified.

===========  ======================================================================
Return Code  Reason
===========  ======================================================================
200          Successful execution.
400          The UUID provided is not a valid UUID.
401          Invalid session or session token.
403          User does not have RBAC rights to modify the content.
404          The UUID provided does not refer to a Node.
409          The object is in use and cannot be modified.
===========  ======================================================================

DELETE /api/v1/node/``{uuid}``
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Delete the Node identified by ``uuid``.  The Node must not be assigned to any
Edge Site.

===========  ======================================================================
Return Code  Reason
===========  ======================================================================
200          Successful execution.
400          The UUID provided is not a valid UUID.
401          Invalid session or session token.
403          User does not have RBAC rights to delete the content.
404          The UUID provided does not refer to a Node.
409          The object is in use and cannot be deleted.
===========  ======================================================================

.. _region-api:

Region API
----------
The Region API is used for creating and listing Regions controlled by the RC.

POST /api/v1/region/
^^^^^^^^^^^^^^^^^^^^

Creates a new region.
If created correctly, a code of 201 - Created is returned, and the Location: header
specifies the URL of the new region.
The structure of the JSON to be passed to the POST request is:

.. code-block:: json

  {
    "name": "region name",
    "description": "a description of the region (optional)"
  }

===========  ======================================================================
Return Code  Reason
===========  ======================================================================
201          Region created.
400          Invalid content supplied.
401          Invalid session or session token.
403          User does not have RBAC rights to the content.
===========  ======================================================================

GET /api/v1/region
^^^^^^^^^^^^^^^^^^

Returns a list of regions.

.. code-block:: yaml

  regions:
  - {parent: 00000000-0000-0000-0000-000000000000, name: US Northeast, description: NYNEX/SNET
      Area, uuid: 59b9daca-2e33-11e9-8b69-0017f20dbff8, url: /api/v1/region/59b9daca-2e33-11e9-8b69-0017f20dbff8}
  - {parent: 00000000-0000-0000-0000-000000000000, name: US South, description: BellSouth
      Area, uuid: 680ce7ca-2e33-11e9-972f-0017f20dbff8, url: /api/v1/region/680ce7ca-2e33-11e9-972f-0017f20dbff8}
  - {parent: 00000000-0000-0000-0000-000000000000, name: US Central, description: Ameritech
      Area, uuid: a591446c-313d-11e9-b4cb-0017f20dbff8, url: /api/v1/region/a591446c-313d-11e9-b4cb-0017f20dbff8}
  - {parent: 00000000-0000-0000-0000-000000000000, name: new_region, description: testing
      only, uuid: f2932a38-b335-4aef-afc3-2ff800763a15, url: /api/v1/region/f2932a38-b335-4aef-afc3-2ff800763a15}
  - {parent: 00000000-0000-0000-0000-000000000000, name: US West, description: Pacific
      Telesis Area, uuid: 015589b6-2fd1-11e9-8de4-07a20e8ebae1, url: /api/v1/region/015589b6-2fd1-11e9-8de4-07a20e8ebae1}
  - {parent: 00000000-0000-0000-0000-000000000000, name: US Northwest, description: USWest
      Area, uuid: 5c1e6560-2e33-11e9-821c-0017f20dbff8, url: /api/v1/region/5c1e6560-2e33-11e9-821c-0017f20dbff8}
  - {parent: 00000000-0000-0000-0000-000000000000, name: Universal, description: The Parent of all Regions,
      uuid: 00000000-0000-0000-0000-000000000000, url: /api/v1/region/00000000-0000-0000-0000-000000000000}

===========  ======================================================================
Return Code  Reason
===========  ======================================================================
200          Successful execution.
401          Invalid session or session token.
403          User does not have RBAC rights to the content.
===========  ======================================================================

GET /api/v1/region/``{uuid}``
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Returns details about the specific region identified by ``UUID``,
including a list of Edge Sites assigned to that region.

.. code-block:: json

  {
    "name": "US Northwest",
    "uuid": "5c1e6560-2e33-11e9-821c-0017f20dbff8",
    "url": "/api/v1/region/5c1e6560-2e33-11e9-821c-0017f20dbff8",
    "edgesites": [
      "5c1e6560-2e33-11e9-821c-0017f20dbff8",
      "5e814bf6-2e33-11e9-89e0-0017f20dbff8",
      "60e4268e-2e33-11e9-b838-0017f20dbff8",
      "63471562-2e33-11e9-ba55-0017f20dbff8"
    ]
  }

===========  ======================================================================
Return Code  Reason
===========  ======================================================================
200          Successful execution.
401          Invalid session or session token.
403          User does not have RBAC rights to the content.
===========  ======================================================================

PUT /api/v1/region/``{uuid}``
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Update the Region identified by ``uuid``.
The user is allowed to modify the name, description and parent fields of the Region.
The user is not allowed to change any fields of the ``universal region`` (with UUID
00000000-0000-0000-0000-000000000000).  If the parent region is changed, it must refer to
another valid region's UUID, and cannot be self-referential.
The content provided to the PUT operation, in either YAML or JSON form, should consist
of just those fields to be modified.

===========  ======================================================================
Return Code  Reason
===========  ======================================================================
200          Successful execution.
400          The UUID provided is not a valid UUID, or the parent UUID is invalid.
401          Invalid session or session token.
403          User does not have RBAC rights to modify the Region.
404          The UUID provided does not refer to a Region.
===========  ======================================================================

DELETE /api/v1/region/``{uuid}``
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Delete the Region identified by ``uuid``.  The Region must not be in use by any
other Regions, or any Edge Sites.

===========  ======================================================================
Return Code  Reason
===========  ======================================================================
200          Successful execution.
400          The UUID provided is not a valid UUID.
401          Invalid session or session token.
403          User does not have RBAC rights to delete the content.
404          The UUID provided does not refer to a Region.
409          The object is in use and cannot be deleted.
===========  ======================================================================

.. _edgesite-api:

Edge Site API
-------------
An Edge Site is one or more Nodes, treated as a unit for purposes of deployment, and
assigned to one or more regions.  Most operations provided by the Regional Controller
operate at the unit of an Edge Site.

POST /api/v1/edgesite
^^^^^^^^^^^^^^^^^^^^^

Create a new Edge Site, assigned to a region.

.. code-block:: json

  {
    "name": "MT-Cluster-1",
    "regions": [
       "59b9daca-2e33-11e9-8b69-0017f20dbff8"
    ],
    "nodes": [
       "node_id_1", "node_id_2", "node_id_3", "node_id_4"
    ]
  }

===========  ======================================================================
Return Code  Reason
===========  ======================================================================
201          Region created.
400          Invalid content supplied.
401          Invalid session or session token.
403          User does not have RBAC rights to the content.
===========  ======================================================================

GET /api/v1/edgesite{?region={regionuuid}}
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Returns a list of Edge Sites assigned to the region identified by ``regionuuid``.
If no ``regionuuid`` is provided, then all Edge Sites are returned.

===========  ======================================================================
Return Code  Reason
===========  ======================================================================
200          Successful execution.
400          The region UUID provided is not a valid UUID.
401          Invalid session or session token.
403          User does not have RBAC rights to the content.
404          The region UUID provided does not refer to a region.
===========  ======================================================================

GET /api/v1/edgesite/``{uuid}``
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Returns details about the specific Edge Site identified by ``UUID.``

===========  ======================================================================
Return Code  Reason
===========  ======================================================================
200          Successful execution.
401          Invalid session or session token.
403          User does not have RBAC rights to the content.
===========  ======================================================================

PUT /api/v1/edgesite/``{uuid}``
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Update the Edge Site identified by ``uuid``.
The user is allowed to modify the name, description, list of regions, or list of nodes in
the Edge Site.  If the Edge Site is currently being used by a POD, then the list of
nodes may only be expanded, and nodes currently in use may not be removed.
The content provided to the PUT operation, in either YAML or JSON form, should consist
of just those fields to be modified.

===========  ======================================================================
Return Code  Reason
===========  ======================================================================
200          Successful execution.
400          The UUID provided is not a valid UUID.
401          Invalid session or session token.
403          User does not have RBAC rights to modify the Edge Site.
404          The UUID provided does not refer to a Edge Site.
===========  ======================================================================

DELETE /api/v1/edgesite/``{uuid}``
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Delete the Edge Site identified by ``uuid``.  The Edge Site must not be in use
by any POD in the system.

===========  ======================================================================
Return Code  Reason
===========  ======================================================================
200          Successful execution.
400          The UUID provided is not a valid UUID.
401          Invalid session or session token.
403          User does not have RBAC rights to delete the content.
404          The UUID provided does not refer to a Edge Site.
409          The object is in use and cannot be deleted.
===========  ======================================================================

.. _blueprint-api:

Blueprint API
-------------
The Blueprint API is used for maintain the list of Blueprints installed in the RC.

POST /api/v1/blueprint
^^^^^^^^^^^^^^^^^^^^^^

Creates a new Blueprint.
If created correctly, a code of 201 - Created is returned, and the Location: header
specifies the URL of the new blueprint.
The structure of the JSON to be passed to the POST request is:

.. code-block:: json

  {
    "blueprint": "1.0.0",
    "name": "Hello World Blueprint",
    "version": "1.0.0",
    "description": "a simple Blueprint designed to show off the features of Akraino",
    "yaml": {
      "workflow": {
        "create": {
          "comment": "the workflow to create a POD with this blueprint goes here"
        }
        "update": {
          "comment": "the workflow to update a POD with this blueprint goes here.
              There may be multiple update workflows"
        }
        "delete": {
          "comment": "the workflow to delete a POD with this blueprint goes here"
        }
      }
    }
  }

===========  ======================================================================
Return Code  Reason
===========  ======================================================================
201          Blueprint created.
400          Invalid content supplied.
401          Invalid session or session token.
403          User does not have RBAC rights to create a Blueprint.
===========  ======================================================================

GET /api/v1/blueprint
^^^^^^^^^^^^^^^^^^^^^

Returns a list of available Blueprints.

.. code-block:: json

  {
    "blueprints": [
      {
        "name": "Network Cloud - Rover",
        "uuid": "827cfe84-2e28-11e9-bb34-0017f20dbff8",
        "url": "/api/v1/blueprint/827cfe84-2e28-11e9-bb34-0017f20dbff8",
        "version": "0.0.2"
      },
      {
        "name": "Network Cloud - Unicycle",
        "uuid": "82f490de-2e28-11e9-a0e0-0017f20dbff8",
        "url": "/api/v1/blueprint/82f490de-2e28-11e9-a0e0-0017f20dbff8",
        "version": "0.0.2"
      }
    ]
  }

===========  ======================================================================
Return Code  Reason
===========  ======================================================================
200          Successful execution.
401          Invalid session or session token.
403          User does not have RBAC rights to the content.
===========  ======================================================================

GET /api/v1/blueprint/``{uuid}``
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Returns details about the specific Blueprint identified by ``UUID.``

.. code-block:: json

  {
    "name": "Network Cloud - Rover",
    "uuid": "827cfe84-2e28-11e9-bb34-0017f20dbff8",
    "url": "/api/v1/blueprint/827cfe84-2e28-11e9-bb34-0017f20dbff8",
    "version": "0.0.2"
  }

===========  ======================================================================
Return Code  Reason
===========  ======================================================================
200          Successful execution.
400          The specified UUID is invalid.
401          Invalid session or session token.
403          User does not have RBAC rights to the content.
===========  ======================================================================


PUT /api/v1/blueprint/``{uuid}``
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Update the Blueprint identified by ``uuid``.
As Blueprints are *supposed* to be mostly static, you may only change the description of
a Blueprint.

===========  ======================================================================
Return Code  Reason
===========  ======================================================================
400          The UUID provided is not a valid UUID.
401          Invalid session or session token.
403          User does not have RBAC rights to modify the Blueprint.
404          The UUID provided does not refer to a Blueprint.
===========  ======================================================================

DELETE /api/v1/blueprint/``{uuid}``
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Delete the Blueprint identified by ``uuid``.  The Blueprint must not be in use
by any POD in the system.

===========  ======================================================================
Return Code  Reason
===========  ======================================================================
200          Successful execution.
400          The UUID provided is not a valid UUID.
401          Invalid session or session token.
403          User does not have RBAC rights to delete the content.
404          The UUID provided does not refer to a Blueprint.
409          The object is in use and cannot be deleted.
===========  ======================================================================

.. _pod-api:

POD API
-------
POST /api/v1/pod/{?dryrun=true}
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Start to create a POD by combining a Blueprint, and Edgesite, and an input file in JSON
or YAML format. The Blueprint and Edgesite to use are specified by the (required)
``blueprint`` and ``edgesite`` fields in the input. The API server will verify that the
input contains all of the fields required by the Blueprint, and that the workflows and
components specified in the Blueprint are available.

If the ``dryrun`` parameter is specified (with any value), and the verification succeeds,
then the process ends here, with a 200 return code. The POD is not actually created when
the dryrun parameter is included.

If the ``dryrun`` parameter is not specified, then the POD is created, and the deployment
of the Blueprint on the Edge Site is started. The workflow specified in the ``create``
section of the Blueprint is invoked to initiate the deployment. The deployment happens
asynchronously.

If the input is provided in JSON form, the *yaml* parameter may be provided as a string
(in which case it is interpreted as YAML), or as a JSON object (in which case it is
converted to YAML).

The Blueprint and Edge Site to verify against are identified by UUIDs in the POST-ed JSON.
The parameters needed to deploy the Edge Site are passed as content to this URL.

Upon a successful completion, a ``201 - created`` is returned, and a reference to the
created POD is returned.  This newly created POD object can be used to monitor the status
of the deployment.

An example of YAML input:

.. code-block:: yaml

    name: Aknode44 Rover
    blueprint: 827cfe84-2e28-11e9-bb34-0017f20dbff8
    edgesite: 2d35351a-3dcb-11e9-9535-e36fdca4d937
    yaml:
      server: aknode44
      oem: dell
      password: secret
      build_script: script-hwe-16.04.6-amd64.ipxe
      build_interface: enp94s0f0
      oob:
        ip: 192.168.41.254
        user: root
        password: calvin
      boot_device: sda
      bios_template: dell_r740_g14_uefi_base.xml.template
      boot_template: dell_r740_g14_uefi_httpboot.xml.template
      firstboot_template: firstboot.sh.template
      boot_dev: NIC.Slot.3-1-1
      ipxe_interface: net4
      network:
        mtu: 9000
        ip: 192.168.2.44
        subnet: 192.168.2.0
        netmask: 255.255.255.0
        gateway: 192.168.2.200
        dns: 192.168.2.85
        domain: lab.akraino.org
        dnssearch: lab.akraino.org
        ntp: ntp.ubuntu.org
        bond: bond0
        bond_slaves: [ enp94s0f0, enp94s0f1 ]
        vlan: 41

===========  ======================================================================
Return Code  Reason
===========  ======================================================================
200          Successful execution. The Edge Site/Blueprint/JSON combination is valid.  Returned if the dryrun parameter is passed.
201          Successful execution, the deployment has started.  A reference to the created POD is returned.
400          The UUID provided is not a valid UUID.
400          There is no Blueprint UUID in the JSON, or it does not refer to a valid Blueprint.
400          There is no Edge Site UUID in the JSON, or it does not refer to a valid Edge Site.
400          The verification failed for some other reason specified in the returned content.
401          Invalid session or session token.
403          User does not have RBAC rights to the content.
404          The UUID provided does not refer to a valid Edge Site.
===========  ======================================================================

GET /api/v1/pod
^^^^^^^^^^^^^^^

Returns a list of currently running POD UUIDs.

.. code-block:: json

  {
    "pods": [
      {
        "blueprint": "827cfe84-2e28-11e9-bb34-0017f20dbff8",
        "edgesite": "2d35351a-3dcb-11e9-9535-e36fdca4d937",
        "name": "testpod",
        "state": "INIT",
        "url": "/api/v1/pod/f067d2db-12b3-40c6-9210-1edb917cfe0e",
        "uuid": "f067d2db-12b3-40c6-9210-1edb917cfe0e",
        "yaml": {}
      },
      {
        "blueprint": "827cfe84-2e28-11e9-bb34-0017f20dbff8",
        "edgesite": "2d353736-3dcb-11e9-9539-43216df93629",
        "name": "testpod",
        "state": "INIT",
        "url": "/api/v1/pod/de69d841-cebc-4bd2-9577-f0eec7703bd0",
        "uuid": "de69d841-cebc-4bd2-9577-f0eec7703bd0",
        "yaml": {}
      }
    ]
  }

===========  ======================================================================
Return Code  Reason
===========  ======================================================================
200          Successful execution.
401          Invalid session or session token.
403          User does not have RBAC rights to the content.
===========  ======================================================================

GET /api/v1/pod/``{uuid}``
^^^^^^^^^^^^^^^^^^^^^^^^^^

Returns the details about a specific POD identified by ``UUID``, including the current
deployment and/or update status of the POD.

Example YAML output from the API:

.. code-block:: yaml

  uuid: b9eb21c8-556c-11e9-a199-affa3402765c
  url: /api/v1/pod/b9eb21c8-556c-11e9-a199-affa3402765c
  name: Aknode44 Rover
  blueprint: 827cfe84-2e28-11e9-bb34-0017f20dbff8
  edgesite: 2d35351a-3dcb-11e9-9535-e36fdca4d937
  state: WORKFLOW
  events:
  - { time: "Apr 02, 2019 10:57:56 AM", level: info, message: POD created. }
  - { time: "Apr 02, 2019 10:57:58 AM", level: info, message: Workflow fetched. }
  - { time: "Apr 02, 2019 10:57:59 AM", level: info, message: Workflow initiated. }
  - { time: "Apr 02, 2019 10:58:04 AM", level: info, message: Artifacts fetched. }
  - { time: "Apr 02, 2019 10:58:32 AM", level: info, message: REDFISH started. }

===========  ======================================================================
Return Code  Reason
===========  ======================================================================
200          Successful execution.
401          Invalid session or session token.
403          User does not have RBAC rights to the content.
===========  ======================================================================

PUT /api/v1/pod/``{uuid}``
^^^^^^^^^^^^^^^^^^^^^^^^^^

Update the POD identified by ``uuid``. You may only change the description of a POD
using this method; all other fields are considered "read-only."  In addition, this method
does not run a workflow.

===========  ======================================================================
Return Code  Reason
===========  ======================================================================
400          The UUID provided is not a valid UUID.
401          Invalid session or session token.
403          User does not have RBAC rights to modify the POD.
404          The UUID provided does not refer to a POD.
===========  ======================================================================

PUT /api/v1/pod/``{uuid}``/``{workflow}``
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Update the POD identified by ``uuid`` by running a named workflow.  The name of the workflow
to run is specified in the URL (``workflow``) and must match a name listed under the
``workflow`` stanza in the POD's Blueprint.

As with the POST operation, you are allowed to pass a ``dryrun`` query parameter, which
indicates that you want to test whether the PUT operation will work without actually
starting the associated workflow.

===========  ======================================================================
Return Code  Reason
===========  ======================================================================
400          The UUID provided is not a valid UUID.
401          Invalid session or session token.
403          User does not have RBAC rights to modify the POD.
404          The UUID provided does not refer to a POD.
===========  ======================================================================

DELETE /api/v1/pod/``{uuid}``
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Delete the POD identified by ``uuid``.
This initiates a removal of the Blueprint from the Edge Site that this POD is using,
by running a workflow named ``delete`` (if one exists) form the PODs Blueprint.
The operation happens asynchronously. The progress of the deletion can be monitored
by issuing GETs against the POD URL, and looking at the ``state`` field.  When the
delete workflow finishes, the value of the state field will change from WORKFLOW to DEAD.

===========  ======================================================================
Return Code  Reason
===========  ======================================================================
202          Operation started.
400          The UUID provided is not a valid UUID.
401          Invalid session or session token.
403          User does not have RBAC rights to delete the content.
404          The UUID provided does not refer to a Blueprint.
409          The object is in use and cannot be deleted.
===========  ======================================================================

DELETE /api/v1/pod/``{uuid}``/force
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Delete the POD identified by ``uuid`` immediately.
This moves the POD identified by ``uuid`` immediately into the DEAD state, without running
any workflow.  This is used when you want to forcibly remove a POD from the system, and
should be used with care.

===========  ======================================================================
Return Code  Reason
===========  ======================================================================
202          Operation started.
400          The UUID provided is not a valid UUID.
401          Invalid session or session token.
403          User does not have RBAC rights to delete the content.
404          The UUID provided does not refer to a Blueprint.
409          The object is in use and cannot be deleted.
===========  ======================================================================

.. _version-api:

Version API
-----------
The Version API is used for to retrieve the versions of the various running containers
that comprise the Regional Controller, as well as the versions of any blueprints or add-on
software modules installed on the Regional Controller.

GET /api/v1/version
^^^^^^^^^^^^^^^^^^^
Get a list of the versions.

.. code-block:: json

  {
    "blueprints": {
        "Radio Edge Cloud": "1.0.0",
        "Network Cloud - Rover": "0.0.2",
        "Network Cloud - Unicycle": "0.0.2"
    },
    "containers": {
        "akraino/rc_api": "1.0.0",
        "osixia/openldap": "1.2.3",
        "postgres": "9.6.9",
        "nginx": "1.14.2"
    }
    "addons": {
    }
  }

===========  ======================================================================
Return Code  Reason
===========  ======================================================================
200          Successful execution.
401          Invalid session or session token.
403          User does not have RBAC rights to the content.
===========  ======================================================================

.. _addon-api:

Add-on API
----------
.. warning::

  This API is to be provided later.
