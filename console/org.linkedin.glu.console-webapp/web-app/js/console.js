/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

/**
 * Look for all the children of the provided container for all input elements and select
 * the percentage provided.
 */
function quickSelect(containerId, className, percentage) {
  var container = document.getElementById(containerId);
  var inputs = container.getElementsByTagName('input');
  var filteredInputs = new Array();
  for(var i = 0; i < inputs.length; i++)
  {
    var input = inputs.item(i)
    if(input.className == className)
    {
      input.checked = false
      filteredInputs.push(input);
    }
  }

  filteredInputs.length = Math.round(filteredInputs.length * percentage / 100)

  for(i in filteredInputs)
  {
    filteredInputs[i].checked = true;
  }
}

/**
 * Select only one (the first one).
 *
 * @param containerId the root to look for children
 * @param className the class name associated to the input tag
 */
function selectOne(containerId, className)
{
  var container = document.getElementById(containerId);
  var inputs = container.getElementsByTagName('input');

  // unselect all except the first one
  var firstOneChecked = false
  for(var i = 0; i < inputs.length; i++)
  {
    var input = inputs.item(i)
    if(input.className == className)
    {
      if(firstOneChecked)
      {
        input.checked = false
      }
      else
      {
        input.checked = true
        firstOneChecked = true
      }
    }
  }
}

/**
 * Similar to grails encodeAsHTML but in javascript.
 * Credits to http://jsperf.com/htmlencoderegex/35
 */
encodeAsHTML = (function () {
  'use strict';
  var DOMText = document.createTextNode("test");
  var DOMNative = document.createElement("span");
  DOMNative.appendChild(DOMText);

  //main work for each case
  return function (html) {
    DOMText.nodeValue = html;
    return DOMNative.innerHTML
  };
}());