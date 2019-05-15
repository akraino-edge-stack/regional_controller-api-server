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
<html>
<head>
<meta charset="UTF-8">
<title>Hello World Blueprint</title>
</head>
<body>

<pre>
#
#      Copyright (c) 2019 AT&T Intellectual Property. All Rights Reserved.
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

blueprint: 1.0.0
name: Hello World Blueprint
version: 1.0.0
description: This Blueprint demonstrates what is needed in a minimal ARC blueprint.
yaml:
  # Allow deployment to any hardware, max of 4 nodes
  hardware_profile: { name: '.*', min: 1, max: 4 }
  workflow:
    create:
      url: 'http://arc-api:8080/blueprints/hello-world/hw_create_wf.py'
      components:
        - 'https://www.google.com/images/branding/googlelogo/2x/googlelogo_color_272x92dp.png'
        - 'https://dilbert.com/assets/dilbert_character_top-8c372237e95037529b2eb865829ee93214d7575811f4a197b2ebe43966cda5fa.png'
      input_schema:
        name: { type: string }
        region: { type: string }
    update_type_1:
    update_type_2:
    delete:
      url: 'http://arc-api:8080/blueprints/hello-world/hw_delete_wf.sh'
</pre>
</body>
</html>