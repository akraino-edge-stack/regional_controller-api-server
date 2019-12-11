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

.. _exampleblueprint:

An Example Blueprint
====================
The following example shows what is required for a typical Blueprint (in this case,
for *Network Cloud - Rover*).  The *blueprint* attribute lists the version number
of the blueprint schema used by this blueprint.

The *input_schema* attributes, that describe the data that must be passed to the RC in
order to perform an operation, are in a format similar to https://tools.ietf.org/html/draft-wright-json-schema-00
The differences are to be described later (TODO).  Everything specified in the input_schema
is assumed to be required. Optional items should not be listed in the input_schema;
it is the responsibility of the individual workflows to validate the format of
optional data. The RC will validate that the YAML meets the schema when it is
uploaded.  Fields and attributes that are not in the schema will be ignored, which
means that a missing or empty schema will mean no validation will be done by the RC.

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
          - 'https://nexus.akraino.org/service/local/artifact/maven/redirect?r=snapshots&g=org.akraino.airshipinabottle_deploy&a=airshipinabottle_deploy&v=0.0.2-SNAPSHOT&e=tgz'
          - 'http://releases.ubuntu.com/16.04/ubuntu-16.04.6-server-amd64.iso'

      # Workflow that is invoked for updates on the POD
      update:
        url: http://workflows.akraino.org/rover-blueprint/rover_update.py
        input_schema:
          # to be provided later

      # Workflow that is invoked when the POD is deleted
      delete:
        url: http://workflows.akraino.org/rover-blueprint/rover_delete.py

Example YAML input for a Create POD Operation
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

An example of the YAML to be uploaded to the RC when a POD is created with the Rover
blueprint.  This YAML is required to match the *workflow.create.input_schema* in the
blueprint.

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
