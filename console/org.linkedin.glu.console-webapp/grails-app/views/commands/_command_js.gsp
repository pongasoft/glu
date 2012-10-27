%{--
  - Copyright (c) 2012 Yan Pujante
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
<g:javascript>
function renderCommandStream(commandId, streamType, success)
{
%{--YP Implementation note: this is obviously hacky, but there does not seem to be a way to use g.remoteFunction + javascript variables in all places!

  ${g.remoteFunction(controller: 'commands', id: <javascript commandId>, action: 'stream', params: [streamType: <javascript streamType>], update:[success: '<javascript variable>'])}

--}%
  jQuery.ajax({type:'GET',data:{'streamType': streamType}, url:'/console/commands/' + commandId + '/stream',success:function(data,textStatus){jQuery('#' + success).html(data);},error:function(XMLHttpRequest,textStatus,errorThrown){}});
}
function renderCommand(commandId, success) {
  if(!isHidden('#' + success))
  {
    jQuery.ajax({type:'GET',url:'/console/commands/renderCommand/' + commandId,success:function(data,textStatus){jQuery('#' + success).parent().html(data);},error:function(XMLHttpRequest,textStatus,errorThrown){}});
  }
}
</g:javascript>