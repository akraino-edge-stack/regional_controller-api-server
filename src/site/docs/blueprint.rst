..
      Copyright (c) 2019, 2020 AT&T Intellectual Property. All Rights Reserved.

      Licensed under the Apache License, Version 2.0 (the "License");
      you may not use this file except in compliance with the License.
      You may obtain a copy of the License at

          http://www.apache.org/licenses/LICENSE-2.0

      Unless required by applicable law or agreed to in writing, software
      distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
      WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
      License for the specific language governing permissions and limitations
      under the License.

.. _exampleblueprint:

Blueprints
==========

What is a Blueprint
^^^^^^^^^^^^^^^^^^^
A Blueprint, as used by the Regional Controller, is a YAML file that contains
all the information needed to instantiate an Akraino Blueprint on an Edgesite
(a cluster of nodes), and to monitor and regulate the lifecycle of the POD.
This includes:

1. A name and version for the blueprint.

2. A formalized description of the hardware required in order to instantiate the
   Blueprint.

3. One or more *workflows* required to create and regulate the lifecycle of the
   blueprint.

4. Workflows, in the form of Python or bash scripts that can be run by the RC.

5. Lists of other components required in order to run the workflows.

Blueprint Validation
^^^^^^^^^^^^^^^^^^^^
When a Blueprint is added to the database of Blueprints maintained by a Regional
Controller, it is validated against a JSON schema.  There is one JSON schema
defined for each Blueprint version.  They are downloadable for use with other
software, and are available here:

* *Blueprint Schema Version 1.0.0* <http://REGIONAL_CONTROLLER/schemas/blueprint_schema-1.0.0.json>

The following Python program demonstrates how the Blueprint schema file can
be used to *pre-validate* a Blueprint.

.. code-block:: python

  #!/usr/bin/python
  import sys, json, yaml, jsonschema

  SCHEMA = "blueprint_schema-1.0.0.json"
  with open(SCHEMA, "r") as myfile:
      schema = json.loads(myfile.read())

  with open(sys.argv[1], "r") as myfile:
      doc = yaml.safe_load(myfile)
      jsonschema.validate(instance=doc, schema=schema)

  print(sys.argv[1] + " passes validation.")
  sys.exit(0)

Blueprint Hierarchies
^^^^^^^^^^^^^^^^^^^^^
Blueprints may inherit from other Blueprints in order to create hierarchies of
Blueprints.  These hierarchies allow a newer Blueprint to add and/or override
workflows to a Blueprint which may already be in use for instantiated PODs.
An example of why you might want to do this is when a new version of the Blueprint
is released.  Blueprint\ :sub:`2`\  may be released after Blueprint\ :sub:`1`\ ,
and may inherit from Blueprint\ :sub:`1`\  as well as providing a workflow to
update existing PODs from release 1 to release 2.

Blueprints may only inherit from a single other Blueprint; thus Blueprint
hierarchies form trees with a single root.  When a Blueprint is derived from
another Blueprint, it indicates this by containing a *parent* property in the
*yaml* dict.  This property indicates the UUID of the parent Blueprint (which
must already have previously been installed in the Regional Controller).
A Blueprint without a *parent* property is the root of a (potentially childless)
Blueprint tree.

An Example Blueprint
^^^^^^^^^^^^^^^^^^^^
The following example shows what is required for a typical Blueprint (in this case,
for *Network Cloud - Rover*).  The *blueprint* attribute lists the version number
of the blueprint schema used by this blueprint.

The *input_schema* attributes, which describe the data that must be passed to the
Regional Controller in order to perform an operation, are in a format described
by the Blueprint schema file.
Every item specified in the input_schema is assumed to be required.
Optional items should not be listed in the input_schema; it is the
responsibility of the individual workflows to validate the format of optional data.
The Regional Controller will validate that the YAML meets the syntax requirements
of the schema when it is uploaded.  Fields and attributes that are not in the
schema will be ignored, which means that a missing or empty schema will mean
no validation will be done by the Regional Controller.

.. code-block:: yaml

  # All Blueprints MUST have a blueprint, name, version and yaml attribute.
  # The description is optional.
  blueprint: 1.0.0
  name: Network Cloud - Rover
  version: 1.0.0
  description: This Blueprint defines an instance of the Network Cloud family of blueprints
    that may be deployed on a single node Edgesite.
  yaml:
    # Required hardware profiles (can match on either UUID or name)
    # Note: UUIDs would likely require a global registry of HW profiles.
    hardware_profile:
      or:
        - { uuid: c1dfa1ac-53e0-11e9-86c2-c313482f1fdb }
        - { name: 'HPE DL380.*' }
    workflow:
      # Workflow that is invoked when the POD is created
      create:
        url: http://workflows.akraino.org/rover-blueprint/rover_create.py
        input_schema:
          server:
            type: object
            properties:
              name: { type: string }
              password: { type: string }
              oem: { type: string }
              build_script: { type: string }
              build_interface: { type: string }
              boot_device: { type: string }
              bios_template: { type: string }
              boot_template: { type: string }
              firstboot_template: { type: string }
              boot_dev: { type: string }
              ipxe_interface: { type: string }
              iso: { type: string }
              oob:
                type: object
                properties:
                  ip: { type: ipaddress }
                  user: { type: string }
                  password: { type: string }
              network:
                type: object
                properties:
                  mtu: { type: integer }
                  ip: { type: ipaddress }
                  subnet: { type: ipaddress }
                  netmask: { type: ipaddress }
                  gateway: { type: ipaddress }
                  dns: { type: ipaddress }
                  domain: { type: string }
                  dnssearch: { type: string }
                  ntp: { type: string }
                  vlan: { type: integer }
                  bond:
                    type: object
                    properties:
                      name: { type: string }
                      slaves: { type: array, items: { type: string } }
        components:
          - 'https://www.nytimes.com/'
          - 'http://releases.ubuntu.com/16.04/ubuntu-16.04.6-server-amd64.iso'

      # Workflow that is invoked for updates on the POD
      update:
        url: http://workflows.akraino.org/rover-blueprint/rover_update.py
        input_schema:
          # to be provided later

      # Workflow that is invoked when the POD is deleted
      delete:
        url: http://workflows.akraino.org/rover-blueprint/rover_delete.py

Note: it is also possible to add extra fields to each dictionary in the input_schema
as an aid for any GUI which may need to prompt for input data.  For example, a
workflow may just require a password in order to do its work, in which case all
that is required in the input_schema is

.. code-block:: yaml

  admin_pw: { type: string }

But the GUI which prompts for this information may require extra information
such as a description, a regular expression to verify the input data, a label, etc.
To handle this, extra fields can be added which are not used or checked by the
Regional Controller, e.g.

.. code-block:: yaml

  admin_pw:
    type: string
    description:
      An password to be used for the administrator.  The password must be between
      8 and 32 chars long.
    label: Administrator Password
    minlength: 8
    maxlength: 32

Example YAML input for a Create POD Operation
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

An example of the YAML to be uploaded to the RC when a POD is created with the
Rover blueprint shown above.
This YAML is required to match the *workflow.create.input_schema* in the blueprint.

.. code-block:: yaml

  name: MT cluster 1 rover
  description: Deployment of Rover on MT cluster 1 (nodes akmt1/2/3/4)
  blueprint: 827cfe84-2e28-11e9-bb34-0017f20dbff8
  edgesite: 2d353736-3dcb-11e9-9539-43216df93629
  yaml:
    server:
      name: aknode44
      password: akraino,d
      oem: dell
      build_script: script-hwe-16.04.6-amd64.ipxe
      build_interface: enp94s0f0
      boot_device: sda
      bios_template: dell_r740_g14_uefi_base.xml.template
      boot_template: dell_r740_g14_uefi_httpboot.xml.template
      firstboot_template: firstboot.sh.template
      boot_dev: NIC.Slot.3-1-1
      ipxe_interface: net4
      iso: http://releases.ubuntu.com/16.04/ubuntu-16.04.6-server-amd64.iso
      oob:
        ip: 192.168.41.254
        user: root
        password: calvin
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
        bond:
          name: bond0
          slaves: [ enp94s0f0, enp94s0f1 ]
        vlan: 41
