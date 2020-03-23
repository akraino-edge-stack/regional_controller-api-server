<!--
 Copyright (c) 2019, 2020 AT&T Intellectual Property. All rights reserved.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="org.akraino.regional_controller.utils.BuildUtil" %>
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<title>ARC - Akraino Regional Controller</title>
<style type="text/css">
html {
	margin: 0;
	padding: 0;
}
body {
	text-align: left;
	padding: 0;
	margin: 0;
	background-color: #FFFFEE;
	font: 0.8em Verdana, Arial, Helvetica, sans-serif;
}
div {
	margin: 0;
	padding: 0;
}
p {
	margin: 0px 0px 8px 0px;
	padding: 0;
}
span {
	margin: 0;
	padding: 0;
}
img {
	border: none;
	padding: 0;
	margin: 0;
	background-color: transparent;
}
a img {
	border: none;
}
hr {
	background: #cdcfd0;
	border: 0;
	color: #cdcfd0;
	height: 1px;
	margin: 0 0 10px 0;
}
ul {
	list-style-type: square;
	list-style-position: inside;
}
a {
	color: #1a1a1a;
	text-decoration: none;
}
a:hover {
	color: #003366;
	text-decoration: underline;
	font-weight: normal;
}
#header {
	width: 1000px;
	margin: 0 auto;
}
#content {
	background-color: white;
	width: 1000px;
	margin: 0 auto;
	margin-top: 10px;
	border: 1px solid gray;
	border-radius: 5px;
	-moz-border-radius: 5px;
}
.inner {
	padding: 15px;
}
#footer {
	width: 1000px;
	margin: 0 auto;
	font-size: 10px;
	color: #666;
	padding-right: 2px;
	text-align: right;
}
#extnav {
	font-family: Helvetica, Arial;
	font-size: 12px;
	font-weight: bold;
	height: 24px;
}
#extnav li {
	cursor: pointer;
	float: left;
	list-style-type: none;
}
#extnav li a {
	padding: 0 10px 0 10px;
	color: gray;
	height: 24px;
	line-height: 24px;
	text-decoration: none;
}
#extnav li a:hover {
	color: #FF7200;
	font-weight: bold;
}
#banner {
	height: 52px;
	position: relative;
	padding-top: 20px;
}
#logo-left {
	position: absolute;
	bottom: 0;
}
#logo-right {
	float: right;
}
/* Nav bar (menu) */
#navbar {
	background-color: #067ab4;
	font-family: Helvetica, Arial, sans-serif;
	font-size: 16px;
	font-weight: bold;
	height: 45px;
	border-radius: 5px;
	-moz-border-radius: 5px;
}
#navbar li {
	cursor: pointer;
	float: left;
	list-style-type: none;
}
#navbar li a {
	padding: 0 10px 0 10px;
	border-left: 1px solid white;
	color: white;
	height: 45px;
	line-height: 45px;
	text-decoration: none;
}
#navbar li a.first {
	border-left: 0px solid white;
}
#navbar li a:hover {
	color: #FF7200;
	font-weight: bold;
}
.submenu {
	display: none;
}
ul.dropdown li ul.submenu {
	list-style: none;
	position: absolute; /* Important - Keeps subnav from affecting main navigation flow */
	background: #067AB4;
	margin: 0;
	padding: 0;
	display: none;
	float: left;
	width: 170px;
	border-bottom-left-radius: 5px 5px;
	border-bottom-right-radius: 5px 5px;
	-moz-border-bottom-left-radius: 5px 5px;
	-moz-border-bottom-right-radius: 5px 5px;
}
ul.dropdown li ul.submenu li {
	margin: 0; padding: 0;
	border-top: 1px dotted #FFFFFF;
	clear: both;
	width: 170px;
}
ul.dropdown li ul.submenu li a {
	border-left: 0px; /* cancel white divider in parent menu */
	float: left;
	width: 145px;
	background: #067AB4;
	padding-left: 20px;
}
h1 {
	background-color: #d9c2f2;
	color: black;
	padding: 5px;
	font-size: 14px;
	width: auto;
	border-radius: 5px;
	-moz-border-radius: 5px;
	text-transform: uppercase;
	overflow: hidden;
}
/* re-usable visible table style, 1px gray border */
.table-style {
	background-color: white;
	color: black;
	padding: 0;
	margin: 0 0 13px 0;
	border-collapse: collapse;
	width: 100%;
	clear: both;	/* for clearing floated images */
}
.table-style th, .table-style td {
	background-color: white;
	margin: 0;
	border: 1px solid #cdcfd0;
	padding: 3px;
	color: #666666;
}
.table-style th h5 {
	margin-bottom: 0;
}
td.center {
	text-align: center;
}
</style>
<script>
function airflow(){
    window.location.href = 'http://' + window.location.hostname + ':8080';
}
</script>
</head>
<body>

<div id="header">
	<div id="banner">
		<img alt="Akraino Edge Stack" src="https://wiki.akraino.org/download/thumbnails/327703/image2018-7-18_14-48-13.png"/>
	</div>
</div>

<div id="content">
<div class="inner">
<div style="width: 100%">
	<h1>Regional Controller</h1>
	<ul>
		<li><a href="docs/">Documentation</a></li>
		<li><a onclick="airflow();">AirFlow Console</a></li>
		<li>Sample Blueprints:
			<ul>
			<li><a href="blueprints/hello-world/">Hello World Blueprint</a></li>
			</ul>
		</li>
		<li>Schemas:
			<ul>
			<li><a href="schemas/blueprint_schema-1.0.0.json">Blueprint Schema 1.0.0</a></li>
			</ul>
		</li>
	</ul>
	<h1>Build Information</h1>
	<ul>
		<li>Version: <%= BuildUtil.getVersion() %></li>
		<li>Build date: <%= BuildUtil.getBuildDate() %></li>
	</ul>
</div>
</div>
</div>
<div id="footer">Last update: 2/10/2020</div>
</body>
</html>
