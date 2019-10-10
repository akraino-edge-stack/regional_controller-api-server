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
set -ex

export   DROOT=${DROOT:-"/data"}
export  DOMAIN=${DOMAIN:-"akraino.demo"}
export  PREFIX=${PREFIX:-"arc"}
export      PW=${PW:-"abc123"}
export NETWORK=${NETWORK:-"${PREFIX}-net"}
export CERTDIR=${CERTDIR:-"${DROOT}/nginx/certs"}
export  NGCONF=${NGCONF:-"nginx.conf"}

# Container versions
export  API_IMAGE=${API_IMAGE:-"nexus3.akraino.org:10003/akraino/arc_api:0.0.1-SNAPSHOT"}
export   AF_IMAGE=${AF_IMAGE:-"nexus3.akraino.org:10003/akraino/airflow:0.0.1-SNAPSHOT"}
export   DB_IMAGE=${DB_IMAGE:-"mariadb:10.4"}
export LDAP_IMAGE=${LDAP_IMAGE:-"osixia/openldap:1.2.4"}

# Check user is running as root
if [ "$(id -u)" -ne 0 ]
then
		echo "$0: must be root to run this script."
		exit 1
fi

# Check Docker group
if [ "$(grep docker /etc/group | cut -d: -f3)" -ne 999 ]
then
		echo
		echo "$0: WARNING: the docker group is not gid = 999.  This may cause problems in some workflows."
		echo
fi

# Make external directories
[ ! -d "$DROOT/dags"  ]    && mkdir -p "$DROOT/dags"
[ ! -d "$DROOT/db"    ]    && mkdir -p "$DROOT/db"
[ ! -d "$DROOT/init"  ]    && mkdir -p "$DROOT/init"
[ ! -d "$DROOT/ldap"  ]    && mkdir -p "$DROOT/ldap"
[ ! -d "$DROOT/logs"  ]    && mkdir -p "$DROOT/logs"
[ ! -d "$DROOT/nginx" ]    && mkdir -p "$DROOT/nginx/certs"
[ ! -d "$DROOT/workflow" ] && mkdir -p "$DROOT/workflow"

chown 1000:1000 $DROOT/dags $DROOT/init $DROOT/logs $DROOT/workflow
chown 999:999   $DROOT/db $DROOT/ldap

# See https://airflow.readthedocs.io/en/stable/integration.html for how to proxy airflow

# Create network if it doesn't yet exist
docker network inspect $NETWORK >/dev/null 2>&1 ||
	docker network create $NETWORK

# Initialize $DROOT/init
docker run --rm --name "${PREFIX}-init" \
	--hostname api.$DOMAIN \
	--net=$NETWORK \
	--volume $DROOT/init:/init \
	$API_IMAGE /bin/cp -R init/ /init/..

# Fix up various things
ln $DROOT/init/akraino.demo.key $DROOT/nginx/certs/akraino.key
ln $DROOT/init/akraino.demo.crt $DROOT/nginx/certs/akraino.crt
ln $DROOT/init/${NGCONF}        $DROOT/nginx/nginx.conf
chmod 400 $DROOT/nginx/certs/akraino.*

# Start LDAP
docker run --detach --name "${PREFIX}-ldap" \
	--restart=always \
	--hostname ldap.$DOMAIN \
	--env LDAP_ORGANISATION="akraino" \
	--env LDAP_DOMAIN="$DOMAIN" \
	--env LDAP_ADMIN_PASSWORD="$PW" \
	--net=$NETWORK \
	--volume $DROOT/ldap:/var/lib/ldap \
	$LDAP_IMAGE \
	--loglevel debug

# Start DB - remove --publish option when not testing
#  Make sure DB initialization .sql files are in $DROOT/init
docker run --detach --name "${PREFIX}-db" \
	--restart=always \
	--hostname db.$DOMAIN \
	--env MYSQL_ROOT_PASSWORD="$PW" \
	--env MYSQL_DATABASE="AKRAINO" \
	--env MYSQL_USER="akraino" \
	--env MYSQL_PASSWORD="$PW" \
	--net=$NETWORK \
	--publish 3306:3306 \
	--volume $DROOT/db:/var/lib/mysql \
	--volume $DROOT/init:/docker-entrypoint-initdb.d \
	$DB_IMAGE

# Start API server
docker run --detach --name "${PREFIX}-api" \
	--restart=always \
	--hostname api.$DOMAIN \
	--net=$NETWORK \
	--volume $DROOT/logs:/usr/local/tomcat/logs \
	--volume $DROOT/dags:/dags \
	--volume $DROOT/workflow:/workflow \
	$API_IMAGE

# Start NGiNX
docker run --detach --name "${PREFIX}-nginx" \
	--restart=always \
	--hostname nginx.$DOMAIN \
	--net=$NETWORK \
	--publish 80:80 \
	--publish 443:443 \
	--volume $DROOT/nginx/nginx.conf:/etc/nginx/nginx.conf \
	--volume $CERTDIR:/etc/ssl/cert \
	nginx:1.14.2

# Start the Airflow containers
docker run --detach --name "${PREFIX}-airflow-redis" \
	--restart=always \
	--net=$NETWORK \
	redis:3.2.7 \
	redis-server --requirepass ${PW}

docker run --detach --name "${PREFIX}-airflow-postgres" \
	--restart=always \
	--env POSTGRES_USER=airflow \
	--env POSTGRES_PASSWORD=${PW} \
	--env POSTGRES_DB=airflow \
	--net=$NETWORK \
	postgres:9.6
	# Uncomment these lines to persist data on the local filesystem.
	#     - PGDATA=/var/lib/postgresql/data/pgdata
	# volumes:
	#     - ./pgdata:/var/lib/postgresql/data/pgdata

# Note: set LOGGING_USER/LOGGING_PASSWORD if you change the login for logging pod events
COMMON="
	--restart=always
	--env LOAD_EX=n
	--env FERNET_KEY=46BKJoQYlPPOexq0OhDZnIlNepKFf87WFwLbfzqDDho=
	--env EXECUTOR=Celery
	--env POSTGRES_HOST=${PREFIX}-airflow-postgres
	--env POSTGRES_USER=airflow
	--env POSTGRES_PASSWORD=${PW}
	--env POSTGRES_DB=airflow
	--env REDIS_HOST=${PREFIX}-airflow-redis
	--env REDIS_PASSWORD=${PW}
	--env LOGGING_USER=workflow
	--env LOGGING_PASSWORD=admin123
	--volume ${DROOT}/dags:/usr/local/airflow/dags
	--volume ${DROOT}/workflow:/workflow
	--volume ${DROOT}/init/airflow.cfg:/usr/local/airflow/airflow.cfg
	--volume /var/run/docker.sock:/var/run/docker.sock
	--net=${NETWORK}
"

docker run --detach --name "${PREFIX}-airflow-webserver" \
	$COMMON \
	--publish 8080:8080 \
	$AF_IMAGE webserver

docker run --detach --name "${PREFIX}-airflow-flower" \
	$COMMON \
	--publish 5555:5555 \
	$AF_IMAGE flower

docker run --detach --name "${PREFIX}-airflow-scheduler" \
	$COMMON \
	$AF_IMAGE scheduler

docker run --detach --name "${PREFIX}-airflow-worker" \
	$COMMON \
	$AF_IMAGE worker

echo "ARC started."
