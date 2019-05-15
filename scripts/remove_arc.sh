#!/bin/bash
#
# Copyright (c) 2019 AT&T Intellectual Property. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#        https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
#  start_arc - Start the Akraino Regional Controller network and containers.
#  Override any of the following environment values by setting and exporting it in your
#  environment before running this script.
#
#  remove_arc - Stop and remove all the Docker containers comprising the ARC.
#
set -x

export   DROOT=${DROOT:-"/data"}
export  PREFIX=${PREFIX:-"arc"}
export NETWORK=${NETWORK:-"${PREFIX}-net"}

CONTAINERS="
${PREFIX}-camunda
${PREFIX}-airflow-redis
${PREFIX}-airflow-postgres
${PREFIX}-airflow-webserver
${PREFIX}-airflow-flower
${PREFIX}-airflow-scheduler
${PREFIX}-airflow-worker
${PREFIX}-nginx
${PREFIX}-api
${PREFIX}-volume_init
${PREFIX}-db
${PREFIX}-ldap"

docker stop $CONTAINERS
docker rm   $CONTAINERS
docker network rm $NETWORK

[ "$1" == "--clobber" ] && rm -fr ${DROOT}/*
