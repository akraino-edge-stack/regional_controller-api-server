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
    """ This class represents a connection to a Regional Controller via the API. """

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
        if ':' in server:
            index = server.index(':')
            self._server = server[:index]
            self._port   = int(server[index+1:])
        method = 'http'
        if (self._port % 1000) == 443:
            method = 'https'
        self._url = '%s://%s:%d' % (method, self._server, self._port)
        self._lastresponse = None
        self._laststatus = 0
        self._lastreason = ''

    def prefer_yaml(self):
        """ Set the Accept: header to prefer YAML over JSON """
        self._accept = YAML + ', ' + JSON

    def login(self, login, password):
        """ Login to the RC, and save the login token for future calls """
        payload = {'name': login, 'password': password}
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

    def list_users(self):
        """ List the User available on the RC """
        return self.list_of_type('user')

    def list_versions(self):
        """ List the Version info available on the RC """
        return self.list_of_type('version')

    def list_of_type(self, type):
        """ List the object of ''type'' available on the RC """
        if self._token is None:
            self.login(self._login, self._pswd)
        headers = {'Accept': self._accept, TOKEN_HDR: self._token}
        response = requests.get(self._url+APIPATH+ type, headers=headers, verify=False)
        self._lastresponse = response
        self._laststatus   = response.status_code
        self._lastreason   = response.reason
        return response.text

    def create_blueprint(self, content, ctype=JSON):
        """ Create a Blueprint on the RC """
        return self.create_of_type('blueprint', ctype, content)

    def create_edgesite(self, content, ctype=JSON):
        """ Create an Edgesite on the RC """
        return self.create_of_type('edgesite', ctype, content)

    def create_hardware(self, content, ctype=JSON):
        """ Create a Hardware Profile on the RC """
        return self.create_of_type('hardware', ctype, content)

    def create_node(self, content, ctype=JSON):
        """ Create a Node on the RC """
        return self.create_of_type('node', ctype, content)

    def create_pod(self, content, ctype=JSON):
        """ Create a POD on the RC """
        return self.create_of_type('pod', ctype, content)

    def create_region(self, content, ctype=JSON):
        """ Create a Region on the RC """
        return self.create_of_type('region', ctype, content)

    def create_user(self, content, ctype=JSON):
        """ Create a User on the RC """
        return self.create_of_type('user', ctype, content)

    def create_of_type(self, type, ctype, content):
        """ Create an arbitrary object defined by ''type'' on the RC """
        if self._token is None:
            self.login(self._login, self._pswd)
        headers = {'Content-type': ctype, 'Accept': self._accept, TOKEN_HDR: self._token}
        response = requests.post(self._url+APIPATH+type, json=content, headers=headers, verify=False)
        self._lastresponse = response
        self._laststatus   = response.status_code
        self._lastreason   = response.reason
        return response.text

    def delete_blueprint(self, uuid, ctype=JSON):
        """ Delete a Blueprint on the RC """
        return self.delete_of_type('blueprint', ctype, uuid)

    def delete_edgesite(self, uuid, ctype=JSON):
        """ Delete an Edgesite on the RC """
        return self.delete_of_type('edgesite', ctype, uuid)

    def delete_hardware(self, uuid, ctype=JSON):
        """ Delete a Hardware Profile on the RC """
        return self.delete_of_type('hardware', ctype, uuid)

    def delete_node(self, uuid, ctype=JSON):
        """ Delete a Node on the RC """
        return self.delete_of_type('node', ctype, uuid)

    def delete_pod(self, uuid, ctype=JSON, force=False):
        """ Delete a POD on the RC """
        return self.delete_of_type('pod', ctype, uuid, force)

    def delete_region(self, uuid, ctype=JSON):
        """ Delete a Region on the RC """
        return self.delete_of_type('region', ctype, uuid)

    def delete_user(self, uuid, ctype=JSON):
        """ Delete a User on the RC """
        return self.delete_of_type('user', ctype, uuid)

    def delete_of_type(self, type, ctype, uuid, force=False):
        """ Delete an arbitrary object defined by ''type'' on the RC """
        if self._token is None:
            self.login(self._login, self._pswd)
        myurl = self._url+APIPATH+type+'/'+uuid
        if force:
            myurl = myurl + "/force"
        headers = {'Content-type': ctype, 'Accept': self._accept, TOKEN_HDR: self._token}
        response = requests.delete(myurl, headers=headers, verify=False)
        self._lastresponse = response
        self._laststatus   = response.status_code
        self._lastreason   = response.reason
        return response.text

    def show_blueprint(self, uuid, ctype=JSON):
        """ Show details of a Blueprint on the RC """
        return self.show_of_type('blueprint', ctype, uuid)

    def show_edgesite(self, uuid, ctype=JSON):
        """ Show details of an Edgesite on the RC """
        return self.show_of_type('edgesite', ctype, uuid)

    def show_hardware(self, uuid, ctype=JSON):
        """ Show details of a Hardware Profile on the RC """
        return self.show_of_type('hardware', ctype, uuid)

    def show_node(self, uuid, ctype=JSON):
        """ Show details of a Node on the RC """
        return self.show_of_type('node', ctype, uuid)

    def show_pod(self, uuid, ctype=JSON):
        """ Show details of a POD on the RC """
        return self.show_of_type('pod', ctype, uuid)

    def show_region(self, uuid, ctype=JSON):
        """ Show details of a Region on the RC """
        return self.show_of_type('region', ctype, uuid)

    def show_user(self, uuid, ctype=JSON):
        """ Show details of a User on the RC """
        return self.show_of_type('user', ctype, uuid)

    def show_of_type(self, type, ctype, uuid):
        """ Show details of an arbitrary object defined by ''type'' on the RC """
        if self._token is None:
            self.login(self._login, self._pswd)
        headers = {'Content-type': ctype, 'Accept': self._accept, TOKEN_HDR: self._token}
        response = requests.get(self._url+APIPATH+type+'/'+uuid, headers=headers, verify=False)
        self._lastresponse = response
        self._laststatus   = response.status_code
        self._lastreason   = response.reason
        return response.text

    def update_blueprint(self, uuid, content, ctype=JSON):
        """ Update details of a Blueprint on the RC """
        return self.update_of_type('blueprint', ctype, uuid, content)

    def update_edgesite(self, uuid, content, ctype=JSON):
        """ Update details of an Edgesite on the RC """
        return self.update_of_type('edgesite', ctype, uuid, content)

    def update_hardware(self, uuid, content, ctype=JSON):
        """ Update details of a Hardware Profile on the RC """
        return self.update_of_type('hardware', ctype, uuid, content)

    def update_node(self, uuid, content, ctype=JSON):
        """ Update details of a Node on the RC """
        return self.update_of_type('node', ctype, uuid, content)

    def update_pod(self, uuid, content, ctype=JSON, wflow=None):
        """ Update details of a POD on the RC """
        return self.update_of_type('pod', ctype, uuid, content, wflow)

    def update_region(self, uuid, content, ctype=JSON):
        """ Update details of a Region on the RC """
        return self.update_of_type('region', ctype, uuid, content)

    def update_user(self, uuid, content, ctype=JSON):
        """ Update details of a User on the RC """
        return self.update_of_type('user', ctype, uuid, content)

    def update_of_type(self, type, ctype, uuid, content, wflow=None):
        """ Update details of an arbitrary object defined by ''type'' on the RC """
        if self._token is None:
            self.login(self._login, self._pswd)
        myurl = self._url+APIPATH+type+'/'+uuid
        if wflow != None:
            myurl = "%s/%s" % (myurl, wflow)
        headers = {'Content-type': ctype, 'Accept': self._accept, TOKEN_HDR: self._token}
        response = requests.put(myurl, json=content, headers=headers, verify=False)
        self._lastresponse = response
        self._laststatus   = response.status_code
        self._lastreason   = response.reason
        return response.text
