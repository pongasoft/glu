%{-- - Copyright (c) 2010-2010 LinkedIn, Inc - - Licensed under the
Apache License, Version 2.0 (the "License"); you may not - use this file
except in compliance with the License. You may obtain a copy of - the
License at - - http://www.apache.org/licenses/LICENSE-2.0 - - Unless
required by applicable law or agreed to in writing, software -
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT - WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied. See the - License for the specific language governing
permissions and limitations under - the License. --}%

<html>
<head>
<title>GLU Console - ${user.username.encodeAsHTML()}
</title>
<meta name="layout" content="main" />
</head>
<body>
	<h2>Graph: Versions</h2>
	<div id='chart' style='width: 100%; height: 700px;'></div>
	<div id='table' style='margin-top: 20px'></div>
	<script type="text/javascript" src="http://www.google.com/jsapi"></script>
	<script type="text/javascript">
   google.load('visualization', '1', {packages: ['corechart', 'table']});
  </script>
	<script type="text/javascript">
function drawVisualization() {
  // Populate the data table.
  var data = google.visualization.arrayToDataTable([
<g:each in="${versions}" var="entry">
     ['${entry.module}',${entry.min}, ${entry.max}, '${entry.minAgentsHtml}', '${entry.maxAgentsHtml}'],
</g:each>
   ], true);
  data.setColumnLabel(0, 'Module');
  data.setColumnLabel(1, 'Min');
  data.setColumnLabel(2, 'Max');
  data.setColumnLabel(3, 'Min Agents');
  data.setColumnLabel(4, 'Max Agents');

  // Draw the chart.
  var chart = new google.visualization.CandlestickChart(document.getElementById('chart'));
  var chartView = new google.visualization.DataView(data);
  chartView.setColumns([0, 1, 1, 2, 2]);
  chart.draw(chartView, {legend:'none', hAxis: {showTextEvery: 1, slantedText: true, slantedTextAngle: 90},
    title: 'Versions variation according to the STATIC model',
    	vAxis: {},
    	chartArea: {width: "80%"}});

  var table = new google.visualization.Table(document.getElementById('table'));
  var tableView = new google.visualization.DataView(data);
  tableView.setColumns([0, 1, 3, 2, 4]);
  table.draw(tableView, {showRowNumber: false, allowHtml: true});
}
google.setOnLoadCallback(drawVisualization);
 </script>
</body>
</html>