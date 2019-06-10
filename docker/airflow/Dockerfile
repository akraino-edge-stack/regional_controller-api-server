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

# To build:  docker build -t akraino/airflow .
FROM puckel/docker-airflow:1.10.2
MAINTAINER Robert Eby <eby@research.att.com>

USER root
RUN \
	apt-get update -y && \
	apt-get install -yqq --no-install-recommends \
	apt-transport-https net-tools ssh python-pip gnupg software-properties-common lsb-release git jq && \
	curl -fsSL https://download.docker.com/linux/$(. /etc/os-release; echo "$ID")/gpg > /tmp/dkey; apt-key add /tmp/dkey && \
	add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/$(. /etc/os-release; echo "$ID") $(lsb_release -cs) stable" && \
	apt-get update -yqq && \
	apt-get upgrade -yqq  && \
	apt-get install -yqq --no-install-recommends docker-ce && \
	usermod -aG docker airflow && \
	pip install docker psycopg2 psycopg2-binary 'redis>=3.2.0'
USER airflow

# Note: the docker group id (999) needs to match both inside and outside of the container!
