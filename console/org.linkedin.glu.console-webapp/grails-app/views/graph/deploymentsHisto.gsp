%{--
  - Copyright (c) 2011 Ran Tavory
  -
  - Licensed under the Apache License, Version 2.0 (the "License"); you may not
  - use this file except in compliance with the License. You may obtain a copy of
  - the License at
  -
  - http://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
  - WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
  - License for the specific language governing permissions and limitations under
  - the License.
  --}%
<head>
  <meta name="layout" content="mainNoScript" />
</head>
<body>
	<h2>Graph: ${name}</h2>
	<g:if test="${histo.isEmpty()}">
	 <h3 style="text-align: center">Not enough data :-(</h3>
	</g:if>
  <g:else>
  <div id="chart_div" style="text-align: center;vertical-align: center"></div>
  <script type="text/javascript" src="https://www.google.com/jsapi"></script>
  <script type="text/javascript">
    google.load("visualization", "1", {packages:["corechart"]});
    google.setOnLoadCallback(drawChart);
    function drawChart() {
      var data = google.visualization.arrayToDataTable([
<g:each in="${histo}" var="entry">
     ['<g:formatDate format="MM-dd" date="${entry.key}"/>', ${entry.value.failed}, ${entry.value.success}],
</g:each>
       ], true);      
	     data.setColumnLabel(0, 'Date');
	     data.setColumnLabel(1, 'Failed');
	     data.setColumnLabel(2, 'Success');
	     var chart = new google.visualization.ColumnChart(document.getElementById('chart_div'));
	     chart.draw(data, {width: 800, height: 600, title: 'Deployments by date',
	                       hAxis: {title: 'Date', titleTextStyle: {color: 'white'}},
	                       backgroundColor: '#aaa', legend: 'none', isStacked: true,
	                       colors:['red','green']
	                      });
    }
  </script>
  </g:else>
</body>
</html>