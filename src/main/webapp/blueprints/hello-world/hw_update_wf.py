"""
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

This is the 'create' workflow associated with the "Hello World" blueprint.
"""

import os, sys, time, yaml
import POD

WORKDIR = os.path.abspath(os.path.dirname(__file__))

def start(ds, **kwargs):
	print('Running the Hello World update workflow!')
	print('POD ID is ' + POD.POD)

	yaml      = read_yaml(WORKDIR + '/INPUT.yaml')
	sleeptime = yaml['sleep']
	rc        = yaml['returncode']

	if sleeptime > 0:
		print(' ... sleeping for %d seconds.' % (sleeptime))
		time.sleep(sleeptime)

	if rc > 0:
		print(' ... exiting with RC=%d.  This will cause the workflow to fail.' % (rc))
		sys.exit(rc)

	return 'Done.'

def read_yaml(input_file):
	print('Reading '+input_file+' ...')
	with open(input_file, 'r') as stream:
		try:
			return yaml.safe_load(stream)
		except yaml.YAMLError as exc:
			print(exc)
			sys.exit(1)
