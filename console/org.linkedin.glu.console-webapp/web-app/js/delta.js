/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
 * Portions Copyright (c) 2011 Yan Pujante
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
 * Show/Hide the given column
 */
function showHideColumn(column)
{
  toggleClassChildren('__delta_content', column, !document.getElementById(column).checked, 'hidden');
}

/**
 * compute the rendering parameters
 */
function computeRenderParams(groupBy, columns)
{
  if(groupBy == null)
    groupBy = document.getElementById('groupBy').value;

  var p = 'groupBy=' + groupBy;
  p = p + '&summary=' + document.getElementById('summaryFilter').checked;
  p = p + '&errors=' + document.getElementById('errorsFilter').checked;
  
  for(var i = 0; i < columns.length; i++)
  {
    var column = columns[i];
    if(column != groupBy)
    {
      var elt = document.getElementById(column)
      if(elt != null)
      {
        p = p + '&' + column + '=' + elt.checked;
      }
    }
  }

  return p;
}

/**
 * Shows the spinner while ajax is executing
 */
function showSpinner()
{
  document.getElementById('__delta_content').innerHTML =
    '<img src="/console/images/spinner.gif" alt="Spinner" id="loadingSpinner"/>';
}