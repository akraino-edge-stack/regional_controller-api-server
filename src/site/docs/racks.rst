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

.. _racks:

Conventions for Storing Rack Layout Information
===============================================
The object model does not directly address how nodes are laid out on racks (or indeed if
the user is using any racks).  What follows are some suggested conventions for how this
information can be stored in the *yaml* of Hardware Profiles and Nodes

Example Hardware Profile YAML
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The following shows what could be stored in the yaml stanza for a Hardware Profile for a
normal 1U rack-mounted server.

.. code-block:: yaml

  name: HPE DL380 Gen 10
  description: HPE DL380 Gen 10
  yaml:
    rack_layout:
      height: 1U

The following shows what could be stored in the yaml stanza for a Hardware Profile for a
server that is normally mounted within a chassis on the rack.

.. code-block:: yaml

  name: Nokia Airframe
  description: Nokia Airframe Open Edge Server
  yaml:
    rack_layout:
      height: 1U
      chassis:
        layout: airframe (or vertical or some other predefined layout)
        height: 3U
        units: 5

Note: there are two heights specified here:

- the height of the server unit itself (1U),
- and the height of the chassis (3U).

A GUI interpreting this data will need to know how to draw the various forms of layouts
based upon the *layout* value.  In this example, *airframe* would be drawn something like

+--------+--------+
| unit 4 | unit 1 |
+--------+--------+
| unit 5 | unit 2 |
+--------+--------+
| POWER  | unit 3 |
+--------+--------+


Example Node YAML
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The following yaml stanza would be included in a Node object to describe the location of
the node in a rack, given the hardware profiles above.
For an HP server, we only need to know the rack and the position of the server in the rack.

.. code-block:: yaml

  name: mtexample01
  description: Example HP server
  hardware: <UUID of the HPE hardware profile>
  yaml:
    rack_location:
      # This node is in slot 12 of the rack "MT_RACK07"
      name: MT_RACK07
      slot: 12

For an Airframe server, we need to know the rack, the position of the chassis in the rack,
and the position of the server (the unit number) in the chassis.

.. code-block:: yaml

  name: mtexample02
  description: Example Airframe server
  hardware: <UUID of the Nokia Airflow profile>
  yaml:
    rack_location:
      # This node is the 3rd unit in the chassis in slot 20 of the rack "MT_RACK07"
      name: MT_RACK07
      slot: 20
      unit: 3

Th GUI would now to draw a 3U chassis (since an Airframe unit requires a 3U chassis),
and would display *mtexample02* as unit #3 in that chassis.
