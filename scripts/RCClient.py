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

"""
RC client

A very simple Python client library to use to access the RC API.
"""

import requests
import urllib3

__author__ = 'Robert Eby <eby@research.att.com>'

APIPATH   = '/api/v1/'
TOKEN_HDR = 'X-ARC-Token'
JSON      = 'application/json'
YAML      = 'application/yaml'

class RCClient(object):
    def __init__(self, server='localhost', port=443, login=None, password=None):
        """
        Create a client for the server at the specified location (default = localhost:443)
        """
        urllib3.disable_warnings()
        self._server = server
        self._port   = port
        self._login  = login
        self._pswd   = password
        self._token  = None
        self._accept = JSON + ', ' + YAML
        self._url    = 'http://' + server + ':' + str(port)
        if (port % 1000) == 443:
            self._url = 'https://' + server + ':' + str(port)

    def prefer_yaml(self):
        """ Set the Accept: header to prefer YAML over JSON """
        self._accept = YAML + ', ' + JSON

    def login(self, login, password):
        """ Login to the RC, and save the login token for future calls """
        payload = {'name': self._login, 'password': self._pswd}
        response = requests.post(self._url+APIPATH+'login', json=payload, verify=False)
        self._token = response.headers[TOKEN_HDR]

    def list_login(self):
       """ List login details for this login """
       return self.list_of_type('login')

    def list_blueprints(self):
        """ List the Blueprints available on the RC """
        return self.list_of_type('blueprint')

    def list_edgesites(self):
        """ List the Edgesites available on the RC """
        return self.list_of_type('edgesite')

    def list_hardwares(self):
        """ List the Hardware profiles available on the RC """
        return self.list_of_type('hardware')

    def list_nodes(self):
        """ List the Nodes available on the RC """
        return self.list_of_type('node')

    def list_pods(self):
        """ List the PODs available on the RC """
        return self.list_of_type('pod')

    def list_regions(self):
        """ List the Regions available on the RC """
        return self.list_of_type('region')

    def list_versions(self):
        """ List the Version info available on the RC """
        return self.list_of_type('version')

    def list_of_type(self, type):
        """ List the object of ''type'' available on the RC """
        if self._token == None:
            self.login(self._login, self._pswd)
        headers = {'Accept': self._accept, TOKEN_HDR: self._token}
        response = requests.get(self._url+APIPATH+ type, headers=headers, verify=False)
        self._laststatus = response.status_code
        self._lastreason = response.reason
        return response.text

    def create_blueprint(self, content, ctype=JSON):
        return self.create_of_type('blueprint', ctype, content)

    def create_edgesite(self, content, ctype=JSON):
        return self.create_of_type('edgesite', ctype, content)

    def create_hardware(self, content, ctype=JSON):
        return self.create_of_type('hardware', ctype, content)

    def create_node(self, content, ctype=JSON):
        return self.create_of_type('node', ctype, content)

    def create_pod(self, content, ctype=JSON):
        return self.create_of_type('pod', ctype, content)

    def create_region(self, content, ctype=JSON):
        return self.create_of_type('region', ctype, content)

    def create_of_type(self, type, ctype, content):
        if self._token == None:
            self.login(self._login, self._pswd)
        headers = {'Content-type': ctype, 'Accept': self._accept, TOKEN_HDR: self._token}
        response = requests.post(self._url+APIPATH+type, json=content, headers=headers, verify=False)
        self._laststatus = response.status_code
        self._lastreason = response.reason
        return response.text

    def delete_blueprint(self, uuid, ctype=JSON):
        return self.delete_of_type('blueprint', ctype, uuid)

    def delete_edgesite(self, uuid, ctype=JSON):
        return self.delete_of_type('edgesite', ctype, uuid)

    def delete_hardware(self, uuid, ctype=JSON):
        return self.delete_of_type('hardware', ctype, uuid)

    def delete_node(self, uuid, ctype=JSON):
        return self.delete_of_type('node', ctype, uuid)

    def delete_pod(self, uuid, ctype=JSON):
        return self.delete_of_type('pod', ctype, uuid)

    def delete_region(self, uuid, ctype=JSON):
        return self.delete_of_type('region', ctype, uuid)

    def delete_of_type(self, type, ctype, uuid):
        if self._token == None:
            self.login(self._login, self._pswd)
        headers = {'Content-type': ctype, 'Accept': self._accept, TOKEN_HDR: self._token}
        response = requests.delete(self._url+APIPATH+type+'/'+uuid, headers=headers, verify=False)
        self._laststatus = response.status_code
        self._lastreason = response.reason
        return response.text
