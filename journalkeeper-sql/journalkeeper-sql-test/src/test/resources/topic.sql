--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--

CREATE TABLE IF NOT EXISTS `topic` (
	`id` varchar(255) NOT NULL,
	`code` varchar(255),
	`namespace` varchar(255),
	`partitions` int(11),
	`priority_partitions` varchar(255),
	`type` tinyint(1),
	`update_time` datetime,
	`create_time` datetime,
	PRIMARY KEY (`id`)
);

CREATE INDEX IF NOT EXISTS idx_code_namespace ON topic(`code`, `namespace`);
CREATE INDEX IF NOT EXISTS idx_create_time ON topic(`create_time`);