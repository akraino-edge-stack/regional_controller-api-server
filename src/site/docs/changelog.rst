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

.. _changelog:

Changelog
=========

0.0.1-SNAPSHOT
^^^^^^^^^^^^^^

The initial code, plus all modifications, up to and including commit *18763b4*
(Dec 11, 2019).

0.0.2-SNAPSHOT
^^^^^^^^^^^^^^

- Added

  - A new POD API endpoint (**GET /api/v1/pod/{uuid}/{workflow_instance}**) was
    added to retrieve information about a specific workflow instance.
  - A new POD API endpoint (**GET /api/v1/pod/{uuid}/{workflow_instance}/logs**)
    was added to retrieve the logs from a specific workflow instance.

- Changed

  - Apache Tomcat\ |reg| in the *arc-api* container was updated to version
    8.5.50.
  - The POD API for POD retrieval (**GET /api/v1/pod/{uuid}**) was modified to
    add a list of the URLs of all workflow instances run on behalf of the POD.
  - The POD API for POD update (**PUT /api/v1/pod/{uuid}/{workflow}**) was
    modified in order to return the URL of the workflow instance being run.
  - The *start_arc.sh* script was updated to start the newest version of the
    *arc-api* container.

- Removed

  - Removed the Add-on API from the documentation.

0.0.3-SNAPSHOT
^^^^^^^^^^^^^^

- Added

  - Blueprint 1.0.0 JSON schema for download.
  - Instructions on using the JSON schema to pre-verify a blueprint.
    The description of format and usage of Blueprints has also been fleshed out
    more.

- Changed

  - Apache Tomcat\ |reg| in the *arc-api* container was updated to version
    8.5.53.
  - The *create blueprint* API (POST /api/v1/blueprint) now verifies the Blueprint
    for correctness against the appropriate Blueprint JSON schema.
  - The documentation for the *modify POD* API (PUT /api/v1/pod/{uuid}) has been
    updated to indicate that a POD's blueprint may be modified.

.. |reg|    unicode:: U+000AE .. REGISTERED SIGN
