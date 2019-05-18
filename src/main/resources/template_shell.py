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

This workflow is defined by the ARC and describes the high-level workflow
for a user workflow using shell.  The user's workflow is the second task (maintask),
and may define further additions to the DAG, if required.

"""
from airflow import DAG
from airflow.operators.python_operator import PythonOperator
from airflow.operators.bash_operator import BashOperator
from datetime import datetime
from datetime import timedelta
import sys
import requests

DAG_NAME = '##PHASE##-##UUID##'

default_args = {
    'owner': 'airflow',
    'depends_on_past': False,
    'start_date': datetime.now() - timedelta(0,300),
    'email': ['akraino@akraino.org'],
    'email_on_failure': False,
    'email_on_retry': False,
    'retries': 0
}

dag = DAG(
	DAG_NAME,
	description='Create POD ##UUID##',
	default_args=default_args,
	schedule_interval='@once'
)

API_HOST = 'http://arc-api:8080'

def create_podevent(msg='Default msg', level='INFO'):
	payload  = {'name': 'workflow', 'password': 'admin123'}
	response = requests.post(API_HOST+'/api/v1/login', json=payload)
	token    = response.headers['X-ARC-Token']
	headers  = {'X-ARC-Token': token}
	payload  = {'uuid': '##UUID##', 'level': level, 'message': msg}
	response = requests.post(API_HOST+'/api/v1/podevent', headers=headers, json=payload)

def preamble(ds, **kwargs):
	print('PREAMBLE ------------------------------------------------------------------------')
	create_podevent('Starting ##PHASE## workflow for POD ##UUID##')

def postamble(ds, **kwargs):
	print('POSTAMBLE ------------------------------------------------------------------------')
	create_podevent('Finishing ##PHASE## workflow for POD ##UUID##')
	create_podevent('State changed to: ACTIVE', level='STATUS')

t1 = PythonOperator(task_id='preamble', provide_context=True, python_callable=preamble, dag=dag)

# Note the space at the end of the bash_command value is REQUIRED
t2 = BashOperator(task_id='maintask',
	bash_command='chmod +x /workflow/##PHASE##-##UUID##/##WFNAME##; /bin/bash /workflow/##PHASE##-##UUID##/##WFNAME## ',
	dag=dag)

t3 = PythonOperator(task_id='postamble', provide_context=True, python_callable=postamble, dag=dag)

t2.set_upstream(t1)
t3.set_upstream(t2)
