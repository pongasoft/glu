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
  for(i = 0; i < inputs.length; i++)
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
  for(i = 0; i < inputs.length; i++)
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
 * Select default value in a drop down combo box
 *
 * @param formName Name of the form that contains the combo boxes
 * @param defaultValue default value to set combo box selection to
 */
function setSelectByValue(formName, defaultValue)
{
  var form = document.forms(formName);
  var inputs = form.getElementsByTagName('select');
  var rv = false;

  for(i = 0; i < inputs.length; i++)
  {
    var input = inputs.item(i)
    if(input.type == 'select-one')
    {
      var combo = input
      var clen = combo.options.length
      for (var j = 0; j < clen && combo.options[j].value != defaultValue; j++)
        ;
      if (rv = (j != clen)) {
          combo.selectedIndex = j;
      }
    }
  }

  return rv;
}