#
# Copyright (c) 2019 AT&T Intellectual Property. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#        http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# To build:  docker build -f Dockerfile -t akraino/arc_api .

# Use the standard Tomcat container, with the following additions:
# 1. add netstat and ps
# 2. add our files and remove demo webapps
# 3. run as user tomcat (=1000)

FROM tomcat:8.5.49
RUN \
  apt-get update -y && \
  apt-get install -yqq --no-install-recommends net-tools procps && \
  rm -fr webapps/ROOT webapps/docs webapps/examples webapps/host-manager webapps/manager && \
  useradd -c TOMCAT -s /bin/bash -d /usr/local/tomcat -u 1000 tomcat && \
  mkdir /usr/local/tomcat/init /usr/local/tomcat/init/ldap

COPY 001_create_tables.sql 002_required_data.sql airflow.cfg akraino.demo.* nginx*.conf init/
COPY standard_users.ldif init/ldap/
COPY regional_controller-*.war webapps/ROOT.war
RUN chown -R tomcat /usr/local/tomcat
HEALTHCHECK --interval=60s CMD curl -f http://localhost:8080/ || exit 1
USER tomcat
