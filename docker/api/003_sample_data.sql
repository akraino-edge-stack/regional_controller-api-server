--
-- Copyright (c) 2019 AT&T Intellectual Property. All rights reserved.
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--        https://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--

INSERT INTO AKRAINO.REGION (UUID, NAME, DESCRIPTION, PARENT) VALUES
	('59b9daca-2e33-11e9-8b69-0017f20dbff8', 'US Northeast', 'NYNEX/SNET Area',           '00000000-0000-0000-0000-000000000000'),
	('5c1e6560-2e33-11e9-821c-0017f20dbff8', 'US Northwest', 'USWest Area',               '00000000-0000-0000-0000-000000000000'),
	('a591446c-313d-11e9-b4cb-0017f20dbff8', 'US Central',   'Ameritech Area',            '00000000-0000-0000-0000-000000000000'),
	('015589b6-2fd1-11e9-8de4-07a20e8ebae1', 'US West',      'Pacific Telesis Area',      '00000000-0000-0000-0000-000000000000'),
	('680ce7ca-2e33-11e9-972f-0017f20dbff8', 'US South',     'BellSouth Area',            '00000000-0000-0000-0000-000000000000');

INSERT INTO AKRAINO.BLUEPRINT (UUID, NAME, DESCRIPTION, VERSION, YAML) VALUES
	('827cfe84-2e28-11e9-bb34-0017f20dbff8', 'Rover',    'Single node cluster using Airship-in-a-bottle for deployment', '0.0.2', '{ workflow: { create: { url: http://example.com/blueprints/create.py }}}'),
	('82f490de-2e28-11e9-a0e0-0017f20dbff8', 'Unicycle', 'Multi node cluster using Airship for deployment', '0.0.2', '{ workflow: { create: { url: http://example.com/blueprints/create.py }}}'),
	('e17d25f6-3dcf-11e9-ad7a-770ce7e08f5e', 'REC',      'Radio Edge Cloud (NCIR)', '1.0.0', '{ workflow: { create: { url: http://example.com/blueprints/create.py }}}');

INSERT INTO AKRAINO.EDGESITE (UUID, NAME, DESCRIPTION) VALUES
	('2d35307e-3dcb-11e9-9532-27ce192bb5c9', 'MT-Cluster-1', 'Middletown #1'),
	('2d3533e4-3dcb-11e9-9533-87ac04f6a7e6', 'MT-Cluster-2', 'Middletown #2'),
	('2d35348e-3dcb-11e9-9534-2365184b56d9', 'PA-Cluster-1', 'Palo Alto #1'),
	('2d35351a-3dcb-11e9-9535-e36fdca4d937', 'PA-Cluster-2', 'Palo Alto #2'),
	('2d35359c-3dcb-11e9-9536-dfcac5928c4d', 'Chicago-1',    'Chicago #1'),
	('2d353628-3dcb-11e9-9537-0b640b1bffec', 'Chicago-2',    'Chicago #2'),
	('2d3536aa-3dcb-11e9-9538-9f041af1eeb8', 'Chicago-3',    'Chicago #3'),
	('2d353736-3dcb-11e9-9539-43216df93629', 'Chicago-4',    'Chicago #4'),
	('2d3537c2-3dcb-11e9-953a-9bd0c95c8e50', 'Atlanta-1',    'Atlanta #1'),
	('2d353844-3dcb-11e9-953b-8b0554de4c14', 'Atlanta-2',    'Atlanta #2'),
	('2d3538c6-3dcb-11e9-953c-cf5ab5df3fc8', 'Atlanta-3',    'Atlanta #3'),
	('2d353952-3dcb-11e9-953d-73acafa46a9f', 'Atlanta-4',    'Atlanta #4');

INSERT INTO AKRAINO.EDGESITE_ARRAYS (UUID, FKEY, TYPE) VALUES
	('2d35307e-3dcb-11e9-9532-27ce192bb5c9', '59b9daca-2e33-11e9-8b69-0017f20dbff8', 'R'),
	('2d3533e4-3dcb-11e9-9533-87ac04f6a7e6', '59b9daca-2e33-11e9-8b69-0017f20dbff8', 'R'),
	('2d35348e-3dcb-11e9-9534-2365184b56d9', '015589b6-2fd1-11e9-8de4-07a20e8ebae1', 'R'),
	('2d35351a-3dcb-11e9-9535-e36fdca4d937', '015589b6-2fd1-11e9-8de4-07a20e8ebae1', 'R'),
	('2d35359c-3dcb-11e9-9536-dfcac5928c4d', '59b9daca-2e33-11e9-8b69-0017f20dbff8', 'R'),
	('2d353628-3dcb-11e9-9537-0b640b1bffec', '59b9daca-2e33-11e9-8b69-0017f20dbff8', 'R'),
	('2d3536aa-3dcb-11e9-9538-9f041af1eeb8', '59b9daca-2e33-11e9-8b69-0017f20dbff8', 'R'),
	('2d353736-3dcb-11e9-9539-43216df93629', '59b9daca-2e33-11e9-8b69-0017f20dbff8', 'R'),
	('2d3537c2-3dcb-11e9-953a-9bd0c95c8e50', '680ce7ca-2e33-11e9-972f-0017f20dbff8', 'R'),
	('2d353844-3dcb-11e9-953b-8b0554de4c14', '680ce7ca-2e33-11e9-972f-0017f20dbff8', 'R'),
	('2d3538c6-3dcb-11e9-953c-cf5ab5df3fc8', '680ce7ca-2e33-11e9-972f-0017f20dbff8', 'R'),
	('2d353952-3dcb-11e9-953d-73acafa46a9f', '680ce7ca-2e33-11e9-972f-0017f20dbff8', 'R'),

	('2d35307e-3dcb-11e9-9532-27ce192bb5c9', '011bc90c-47fe-11e9-a04c-4f6070745df6', 'N'),
	('2d35307e-3dcb-11e9-9532-27ce192bb5c9', '0417fc5c-47fe-11e9-a33d-937b5486310c', 'N'),
	('2d35307e-3dcb-11e9-9532-27ce192bb5c9', '0714f40a-47fe-11e9-b1a7-cf7d5cdb612e', 'N'),
	('2d35307e-3dcb-11e9-9532-27ce192bb5c9', '0a106d4c-47fe-11e9-95ee-b783665f2118', 'N');

INSERT INTO AKRAINO.NODE (UUID, NAME, DESCRIPTION, HARDWARE) VALUES
	('e63f27e6-47fd-11e9-b4b5-2bcc0d21031b', 'node01', 'Node 1', 'c1dfa1ac-53e0-11e9-86c2-c313482f1fdb'),
	('e93df346-47fd-11e9-8eef-4f92eac06ca2', 'node02', 'Node 2', 'c1dfa1ac-53e0-11e9-86c2-c313482f1fdb'),
	('ec399b9a-47fd-11e9-9f20-af67efa1a3dd', 'node03', 'Node 3', 'c1dfa1ac-53e0-11e9-86c2-c313482f1fdb'),
	('ef35175c-47fd-11e9-b69c-1f9861d8a906', 'node04', 'Node 4', 'c1dfa1ac-53e0-11e9-86c2-c313482f1fdb'),
	('f23095e4-47fd-11e9-96ac-3fd64fcc2792', 'node05', 'Node 5', 'c1dfa1ac-53e0-11e9-86c2-c313482f1fdb'),
	('f52c21b4-47fd-11e9-aaee-0b3beccb8442', 'node06', 'Node 6', 'c1dfa1ac-53e0-11e9-86c2-c313482f1fdb'),
	('f827d12e-47fd-11e9-ba83-1f2645e15e53', 'node07', 'Node 7', 'c1dfa1ac-53e0-11e9-86c2-c313482f1fdb'),
	('fb24902e-47fd-11e9-a6e6-dbb9904f6eea', 'node08', 'Node 8', 'c1dfa1ac-53e0-11e9-86c2-c313482f1fdb'),
	('fe200ccc-47fd-11e9-9efb-9bfd41625790', 'node09', 'Node 9', 'c1dfa1ac-53e0-11e9-86c2-c313482f1fdb'),
	('011bc90c-47fe-11e9-a04c-4f6070745df6', 'node10', 'Node 10', 'c1dfa1ac-53e0-11e9-86c2-c313482f1fdb'),
	('0417fc5c-47fe-11e9-a33d-937b5486310c', 'node11', 'Node 11', 'c7d2ce90-53e0-11e9-b15e-f72b1e9c4a2c'),
	('0714f40a-47fe-11e9-b1a7-cf7d5cdb612e', 'node12', 'Node 12', 'c7d2ce90-53e0-11e9-b15e-f72b1e9c4a2c'),
	('0a106d4c-47fe-11e9-95ee-b783665f2118', 'node13', 'Node 13', 'c7d2ce90-53e0-11e9-b15e-f72b1e9c4a2c'),
	('0d0c4f48-47fe-11e9-9204-d7daec38a2f8', 'node14', 'Node 14', 'c7d2ce90-53e0-11e9-b15e-f72b1e9c4a2c'),
	('10081fd8-47fe-11e9-820d-372ca4a11e2a', 'node15', 'Node 15', 'c7d2ce90-53e0-11e9-b15e-f72b1e9c4a2c'),
	('1303aca2-47fe-11e9-b680-eb477aa82825', 'node16', 'Node 16', 'c7d2ce90-53e0-11e9-b15e-f72b1e9c4a2c'),
	('15ff23dc-47fe-11e9-a7a8-4398edc4664f', 'node17', 'Node 17', 'c7d2ce90-53e0-11e9-b15e-f72b1e9c4a2c'),
	('18fb1bb8-47fe-11e9-b0a6-4710b55fad5c', 'node18', 'Node 18', 'c7d2ce90-53e0-11e9-b15e-f72b1e9c4a2c'),
	('1bf6c29a-47fe-11e9-a236-1f1ebfcdb341', 'node19', 'Node 19', 'c7d2ce90-53e0-11e9-b15e-f72b1e9c4a2c'),
	('1ef2a4a0-47fe-11e9-ac4c-1b881663eaff', 'node20', 'Node 20', 'c7d2ce90-53e0-11e9-b15e-f72b1e9c4a2c');
