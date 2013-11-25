%{--
  - Copyright (c) 2013 Yan Pujante
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

<%@ page import="org.linkedin.util.clock.Timespan; org.linkedin.util.io.PathUtils; org.linkedin.glu.grails.utils.ConsoleConfig" contentType="text/html;charset=UTF-8" %>
<g:set var="file" value="${new File(URI.create("file:${params.file}"))}"/>
<g:set var="directory" value="${file.parentFile}"/>
<html>
<head>
  <title>File ${file.path.encodeAsHTML()}</title>
  <meta name="layout" content="main"/>
  <link rel="stylesheet" href="${resource(dir:'css',file:'agents-list.css')}"/>
  <style type="text/css">
#file-content {
  white-space: pre;
  word-wrap: normal;
  word-break: break-all;
  max-height: 500px;
  overflow-y: auto;
}
  </style>
  <script type="text/javascript" src="${resource(dir:'js',file:'console.js')}"></script>
  <g:javascript>
nextOffset = null;
nextTimer = null;

function pause()
{
  var playPauseIcon = $('#play-pause-icon');
  // pause pause refresh
  playPauseIcon.removeClass('icon-play');
  playPauseIcon.addClass('icon-pause');
  hide('#spinner');
  show('#spinner-not-spinning');
  if(nextTimer != null)
  {
    clearTimeout(nextTimer);
    nextTimer = null;
  }
}
function toggleRefresh() {
  var playPauseIcon = $('#play-pause-icon');
  var isRefreshing = playPauseIcon.hasClass('icon-play');
  if(isRefreshing)
  {
    // pause pause refresh
    pause();
  }
  else
  {
    // start auto refresh
    playPauseIcon.removeClass('icon-pause');
    playPauseIcon.addClass('icon-play');
    show('#spinner');
    hide('#spinner-not-spinning');
    if(nextTimer == null)
    {
      fetchContent(nextOffset);
    }
  }
}
function toggleWrap() {
  var contentElt = $('#file-content');
  if(contentElt.css('white-space') == 'pre')
  {
    contentElt.css({'white-space': 'pre-wrap'});
  }
  else
  {
    contentElt.css({'white-space': 'pre'});
  }
}
function toggleFullPath() {
   var toggleFullPathIcon = $('#toggle-fullpath-icon');
   var isShowingFullPath = toggleFullPathIcon.hasClass('icon-zoom-out');
   if(isShowingFullPath)
   {
     toggleFullPathIcon.removeClass('icon-zoom-out');
     toggleFullPathIcon.addClass('icon-zoom-in');
     hide('#fullpath');
     show('#filename-only');
   }
   else
   {
     toggleFullPathIcon.removeClass('icon-zoom-in');
     toggleFullPathIcon.addClass('icon-zoom-out');
     hide('#filename-only');
     show('#fullpath');
   }
}
function renderContent(data, textStatus, jqXHR) {
  var totalSize = jqXHR.getResponseHeader("X-glu-length");
  if(totalSize != null)
  {
    $('#file-size').html(totalSize + ' (' + jqXHR.getResponseHeader("X-glu-length-as-MemorySize") + ')');
    $('#file-lastModified').html(jqXHR.getResponseHeader("X-glu-lastModified-as-String"));
    if(data !== "")
    {
      $('#file-content').append(encodeAsHTML(data));
      $('#file-content').scrollTop($("#file-content")[0].scrollHeight);
    }
    asyncFetchContent(totalSize);
  }
  else
  {
    pause();
    nextOffset = 0;
    $('#file-size').html('-');
    $('#file-lastModified').html('-');

    if(jqXHR.getResponseHeader('X-glu-grails-view') == 'login')
    {
      $('#file-content').append('<div class="stderr">You are no longer logged in... Please login again.</div>');
      return;
    }

    if(jqXHR.getResponseHeader('X-glu-unauthorized'))
    {
      $('#file-content').append('<div class="stderr">Unauthorized.</div>');
      return;
    }

    if(data != "")
    {
      $('#file-content').html(encodeAsHTML(data));
      $('#file-content').scrollTop($("#file-content")[0].scrollHeight);
    }
    else
    {
      $('#file-content').append('<div class="stderr">File does not exist.</div>');
    }
  }
}
function asyncFetchContent(offset)
{
  if(offset != null)
  {
    if($('#play-pause-icon').hasClass('icon-play'))
    {
      nextOffset = offset
      nextTimer = setTimeout('fetchContent(' + offset  + ')', ${Timespan.parse(ConsoleConfig.getInstance().defaults.tail?.refreshRate?.toString() ?: '5s').durationInMilliseconds});
    }
  }
}
function fetchContent(offset) {
    jQuery.ajax({
      type:'GET',
      data:{'location': '${file.toURI().rawPath}', 'offset': offset},
      url: '${cl.createLink(controller: 'agents', action: 'fileContent', id: params.id)}',
      success: function(data, textStatus, jqXHR){renderContent(data, textStatus, jqXHR);}});
}
function changeFontSize(selector, increment) {
 var fontSize = parseInt($(selector).css('font-size'));
 fontSize = fontSize + increment + "px";
 $(selector).css({'font-size': fontSize});
 }
  </g:javascript>
</head>
<body onload="fetchContent('-${ConsoleConfig.getInstance().defaults.tail?.size ?: '10k'}');">
<ul class="nav nav-tabs">
  <li><cl:link controller="agents" action="list">List</cl:link></li>
  <cl:whenFeatureEnabled feature="commands"><li><cl:link controller="commands" action="list">All Commands</cl:link></li></cl:whenFeatureEnabled>
  <li><cl:link action="view" id="${params.id}">agent [${params.id}]</cl:link></li>
  <li><cl:link action="plans" id="${params.id}">Plans</cl:link></li>
  <cl:whenFeatureEnabled feature="commands"><li><cl:link action="commands" id="${params.id}">Commands</cl:link></li></cl:whenFeatureEnabled>
  <li><cl:link action="ps" id="${params.id}">All Processes</cl:link></li>
  <li><cl:link title="${directory.path.encodeAsHTML()}" action="fileContent" id="${params.id}" params="[location: directory.toURI().rawPath]">Directory [${directory.name.encodeAsHTML()}]</cl:link></li>
  <li class="active"><a href="#" title="${file.path.encodeAsHTML()}">File [${file.name.encodeAsHTML()}]</a></li>
</ul>

<table class="table table-bordered xtight-table">
  <thead>
  <tr>
    <th>Actions</th>
    <th>File</th>
    <th>Last Modified</th>
    <th>Size</th>
  </tr>
  </thead>
  <tbody>
  <tr>
    <td>
      <cl:link title="Go to directory [${directory.name}]" action="fileContent" id="${params.id}" params="[location: directory.toURI().rawPath]"><i class="icon-arrow-up"> </i></cl:link>
      |
      <a href="#" title="Auto Refresh" onclick="toggleRefresh();"><i class="icon-play" id="play-pause-icon"> </i><img id="spinner" src="${resource(dir:'images',file:'spinner.gif')}" alt="Auto Refresh"/><img id="spinner-not-spinning" class="hidden" src="${resource(dir:'images',file:'spinner_not_spinning.png')}" alt="Auto Refresh Paused"/></a>
      |
      <a href="#" title="Wrap/Unwrap text" onclick="toggleWrap();"><i class="icon-align-justify"> </i></a>
      |
      <a href="#" title="Increase font size" onclick="changeFontSize('#file-content', 1);"><i class="icon-plus"> </i></a>
      |
      <a href="#" title="Decrease font size" onclick="changeFontSize('#file-content', -1);"><i class="icon-minus"> </i></a>
      |
      <cl:link title="View Raw" action="fileContent" id="${params.id}" params="[location: file.toURI().rawPath, maxSize: -1]"><i class="icon-eye-open"> </i></cl:link>
      |
      <cl:link title="Download" action="fileContent" id="${params.id}" params="[location: file.toURI().rawPath, maxSize: -1, binaryOutput: true]"><i class="icon-download-alt"> </i></cl:link>
    </td>
    <td><span id="filename-only">${file.name.encodeAsHTML()}</span><span id="fullpath" class="hidden"><cl:linkFilePath file="${file}" agent="${params.id}"/></span><div class="file-actions"><a href="#" id="toggle-fullpath-icon" title="Show/Hide full path" onclick="toggleFullPath();"><i class="icon-zoom-in"> </i></a></div></td>
    <td id="file-lastModified">-</td>
    <td id="file-size">-</td>
  </tr>
  </tbody>
</table>

<div class="text-file-content">
  <div id="file-content" class="shell"></div>
</div>
</body>
</html>