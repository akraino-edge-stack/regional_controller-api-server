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

An Example Use of the API
=========================
An example of how the API would be used to deploy a POD on a bunch of new machines
follows.  These examples use *curl* as that is the most compact way to show all of the
required headers and data elements required.  Of course, any other programming language
may also be used.

Get a Login Token
----------------------
You will need an account that has permissions to create Nodes, Edgesites, and PODs
(and possibly Blueprints too, if you are defining a new Blueprint).  To do this use the
:ref:`login-api` as follows (assuming the login is *admin* and password *abc123*):

.. code-block:: bash

  curl -v -k -H 'Content-Type: application/yaml' --data '{ name: admin, password: abc123 }' \
    https://arc.akraino.demo/api/v1/login

The API will return a login token (which will expire in one hour) in the *X-ARC-Token* header.
This should be passed to all subsequent API calls.

.. code-block:: bash

  X-ARC-Token: YWRtaW4gICAgICAgIDE1NTY1NTgyMjExMzQ4N2NmMGUwNQ==

Enumerate the Machines
----------------------
Assuming the machines to deploy on have not yet been made known to the RC,
you would need to use the :ref:`node-api` to add them to the RC database.

Do this with the following API call, once per node:

.. code-block:: bash

  YAML='{
    name: nodename,
    description: a description of the node,
    hardware: c1dfa1ac-53e0-11e9-86c2-c313482f1fdb
  }'
  curl -v -k -H 'Content-Type: application/yaml' \
    -H 'X-ARC-Token: YWRtaW4gICAgICAgIDE1NTY1NTgyMjExMzQ4N2NmMGUwNQ==' \
    --data "$YAML" \
    https://arc.akraino.demo/api/v1/node

Keep track of the UUID of each node that is returned from the API.

Create an Edge Site
----------------------
Once the nodes are defined, you need to create an Edge Site (a cluster of nodes).
Do this:

.. code-block:: bash

  YAML='{
    name: edgesitename,
    description: description of the Edgesite,
    nodes: [ <list of node UUIDs> ],
    regions: [ <list of region UUIDs> ]
  }'
  curl -v -k -H 'Content-Type: application/yaml' \
    -H 'X-ARC-Token: YWRtaW4gICAgICAgIDE1NTY1NTgyMjExMzQ4N2NmMGUwNQ==' \
    --data "$YAML" \
    https://arc.akraino.demo/api/v1/edgesite

Create/Verify the Blueprint
---------------------------
To get the UUID of the Blueprint, you would need to see which Blueprints are installed:

.. code-block:: bash

  curl -v -k -H 'Accept: application/yaml' \
    -H 'X-ARC-Token: YWRtaW4gICAgICAgIDE1NTY1NTgyMjExMzQ4N2NmMGUwNQ==' \
    https://arc.akraino.demo/api/v1/blueprint

If the Blueprint you want is missing, you may need to create it:

.. code-block:: bash

  YAML='{
    blueprint: 1.0.0,
    name: my new blueprint,
    version: 1.0.0,
    description: description of the blueprint,
    yaml:  ....
  }'
  curl -v -k -H 'Content-Type: application/yaml' \
    -H 'X-ARC-Token: YWRtaW4gICAgICAgIDE1NTY1NTgyMjExMzQ4N2NmMGUwNQ==' \
    --data "$YAML" \
    https://arc.akraino.demo/api/v1/blueprint

An example blueprint (for Rover) is available here (:ref:`exampleblueprint`).

Start the Deployment (Create the POD)
-------------------------------------
Start the deployment by creating a POD:

.. code-block:: bash

  YAML='{
    name: my new POD,
    description: description of this POD,
    blueprint: 827cfe84-2e28-11e9-bb34-0017f20dbff8,
    edgesite: 2d3533e4-3dcb-11e9-9533-87ac04f6a7e6
  }'
  curl -v -k -H 'Content-Type: application/yaml' \
    -H 'X-ARC-Token: YWRtaW4gICAgICAgIDE1NTY1NTgyMjExMzQ4N2NmMGUwNQ==' \
    --data "$YAML" \
    https://arc.akraino.demo/api/v1/pod

Make note of the UUID that is returned.  You will need it to monitor the deployment.

Monitor the deployment by monitoring the *POD Event* URL for the newly created POD:

.. code-block:: bash

  curl -v -k -H 'Accept: application/yaml' \
    -H 'X-ARC-Token: YWRtaW4gICAgICAgIDE1NTY1NTgyMjExMzQ4N2NmMGUwNQ==' \
    https://arc.akraino.demo/api/v1/podevent/56b365a0-d6a2-4d12-8f02-e2fc2671573e

This will return a list of events related to the POD similar to:

.. code-block:: bash

  events:
  - {level: INFO, time: '2019-04-29 18:15:28.0', message: Pod created.}
  - {level: INFO, time: '2019-04-29 18:15:28.0', message: 'Starting workflow: create'}
  - {level: INFO, time: '2019-04-29 18:15:28.0', message: 'Workflow directory created:
      $DROOT/workflow/create-56b365a0-d6a2-4d12-8f02-e2fc2671573e'}
  - {level: WARN, time: '2019-04-29 18:17:38.0', message: 'Could not fetch the workflow
      file http://example.com/blueprints/create.py'}

You can also monitor the POD itself, to see when its state changes to ACTIVE, which indicates
that the *create* workflow has finished successfully.
