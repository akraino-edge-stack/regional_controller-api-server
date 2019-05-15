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

INSERT INTO AKRAINO.ROLES (UUID, NAME, DESCRIPTION) VALUES
	('0a77bf16-445b-11e9-81f0-b703010027a6', 'readonly',     'Read only access to the ARC'),
	('0b4fb8e4-445b-11e9-b55e-3b725c041ee5', 'fullaccess',   'Full read/write access to the ARC'),
	('0db05ee0-445b-11e9-b61c-c38d36f06889', 'noaccess',     'No access at all'),
	('8119f7c4-61e3-11e9-ae51-ab0fe1d1c7f5', 'workflow',     'API calls from the Workflow Engine'),
	('21d5fb96-445b-11e9-8cce-bf9000b841a7', 'region_admin', 'Region Administrator');

INSERT INTO AKRAINO.ROLE_ATTRIBUTES (ROLE_UUID, ATTRIBUTE) VALUES
	('0a77bf16-445b-11e9-81f0-b703010027a6', 'read-*'),
	('0b4fb8e4-445b-11e9-b55e-3b725c041ee5', 'create-*'),
	('0b4fb8e4-445b-11e9-b55e-3b725c041ee5', 'read-*'),
	('0b4fb8e4-445b-11e9-b55e-3b725c041ee5', 'update-*'),
	('0b4fb8e4-445b-11e9-b55e-3b725c041ee5', 'delete-*'),
	('21d5fb96-445b-11e9-8cce-bf9000b841a7', 'create-region'),
	('21d5fb96-445b-11e9-8cce-bf9000b841a7', 'read-region'),
	('21d5fb96-445b-11e9-8cce-bf9000b841a7', 'update-region'),
	('21d5fb96-445b-11e9-8cce-bf9000b841a7', 'delete-region'),
	('8119f7c4-61e3-11e9-ae51-ab0fe1d1c7f5', 'read-*'),
	('8119f7c4-61e3-11e9-ae51-ab0fe1d1c7f5', 'create-podevent');

INSERT INTO AKRAINO.HARDWARE (UUID, NAME, DESCRIPTION, YAML) VALUES
	('c1dfa1ac-53e0-11e9-86c2-c313482f1fdb', 'Dell PowerEdge R740', 'Standard Dell configuration for Rover/Unicycle',
		'{ cpu: 2x22 Cores @ 2.1GHz Skylake 6152 CPU, ram: 12x32GB, lom: 4x10G Intel 710, nic: [ 2x1G LOM Intel 5xx, 2x25G PCI3 Intel 710 ], disk: [ 4x480G SSD, 6x2.4T HDD ], ps: 2 }'),
	('c7d2ce90-53e0-11e9-b15e-f72b1e9c4a2c', 'HPE DL380 G10', 'Standard HPE configuration for Rover/Unicycle',
		'{ cpu: 2x22 Cores @ 2.1GHz Skylake 6152 CPU, ram: 12x32GB, lom: 4x10G Intel 710, nic: [ 2x1G LOM Intel 5xx, 2x25G PCI3 Intel 710 ], disk: [ 4x480G SSD, 6x2.4T HDD ], ps: 2 }');

INSERT INTO AKRAINO.REGION (UUID, NAME, DESCRIPTION, PARENT) VALUES
	('00000000-0000-0000-0000-000000000000', 'Universal',    'The Parent of all Regions', '00000000-0000-0000-0000-000000000000');

INSERT INTO AKRAINO.USERS (UUID, NAME, DESCRIPTION, PWHASH) VALUES
	('38fdc60e-3dc2-11e9-9df9-53c00beebfb1', 'readonly', 'Read-only Access',   '240BE518FABD2724DDB6F04EEB1DA5967448D7E831C08C8FA822809F74C720A9'),
	('9ef95ad2-3150-11e9-98b6-0017f20dbff8', 'admin',    'Full Access Admin',  '240BE518FABD2724DDB6F04EEB1DA5967448D7E831C08C8FA822809F74C720A9'),
	('2d3a342e-6374-11e9-8b05-8333548995aa', 'workflow', 'Workflow Engine',    '240BE518FABD2724DDB6F04EEB1DA5967448D7E831C08C8FA822809F74C720A9'),
	('a16d969e-4081-11e9-ade7-d3a10ca6285d', 'noaccess', 'Joe Schmoe',         '240BE518FABD2724DDB6F04EEB1DA5967448D7E831C08C8FA822809F74C720A9');

INSERT INTO AKRAINO.USER_ROLES (USER_UUID, ROLE_UUID) VALUES
	('38fdc60e-3dc2-11e9-9df9-53c00beebfb1', '0a77bf16-445b-11e9-81f0-b703010027a6'),
	('9ef95ad2-3150-11e9-98b6-0017f20dbff8', '0b4fb8e4-445b-11e9-b55e-3b725c041ee5'),
	('2d3a342e-6374-11e9-8b05-8333548995aa', '8119f7c4-61e3-11e9-ae51-ab0fe1d1c7f5'),
	('a16d969e-4081-11e9-ade7-d3a10ca6285d', '0db05ee0-445b-11e9-b61c-c38d36f06889');
