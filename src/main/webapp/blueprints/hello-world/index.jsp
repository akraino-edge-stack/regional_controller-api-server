<!--
 Copyright (c) 2019 AT&T Intellectual Property. All rights reserved.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<title>Hello World Blueprint</title>
</head>
<body>

<pre>
#
#      Copyright (c) 2019 AT&amp;T Intellectual Property. All Rights Reserved.
#
#      Licensed under the Apache License, Version 2.0 (the "License");
#      you may not use this file except in compliance with the License.
#      You may obtain a copy of the License at
#
#          http://www.apache.org/licenses/LICENSE-2.0
#
#      Unless required by applicable law or agreed to in writing, software
#      distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
#      WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
#      License for the specific language governing permissions and limitations
#      under the License.
#
---
blueprint: 1.0.0
name: Hello World Blueprint
version: 1.0.0
uuid: 16f4caa8-abbd-11e9-9eb9-4b1f17da8d63
description: This Blueprint demonstrates what is needed in a minimal ARC
  blueprint.
yaml:
  # Allow deployment to any hardware, max of 4 nodes
  hardware_profile: {name: '.*', min: 1, max: 4}
  workflow:
    # The create workflow is run when the POD is initially created
    create:
      url: 'http://arc-api:8080/blueprints/hello-world/hw_create_wf.py'
      components:
        - 'https://www.nytimes.com/'
        - 'https://assets.amuniversal.com/4b9def60e14e0137cc29005056a9545d'
      input_schema:
        sleep: {type: integer}
        returncode: {type: integer}

    # This update workflow would be invoked via
    # "PUT /api/v1/pod/<POD UUID>/update_type_1"
    update_type_1:
      url: 'http://arc-api:8080/blueprints/hello-world/hw_update_wf.py'
      input_schema:
        sleep: {type: integer}
        returncode: {type: integer}

    # The delete workflow is invoked when the POD is DELETE-ed
    delete:
      url: 'http://arc-api:8080/blueprints/hello-world/hw_delete_wf.sh'
</pre>
</body>
</html>
