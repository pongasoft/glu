/*
 * Copyright (c) 2011 Yan Pujante
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

/*
 * Toggle show/hide by adding/removing the class 'hidden'
 * @param container the container to show/hide
*/
function _toggleShowHide(container) {
  container.toggleClass('hidden')
}

/*
 * Toggle show/hide by adding/removing the class 'hidden'
 * @param containerId the container to show/hide
*/
function toggleShowHide(containerId) {
  _toggleShowHide($(containerId));
}

/*
 * Toggle show/hide by adding/removing the class 'hidden'
 * @param containerId where to start looking for children
 * @param className only select the children that have the provided className
 */
function toggleShowHideChildren(containerId, className) {
  alert('toggleShowHideChildren: Not Supported')
  var container = document.getElementById(containerId);
  if(YAHOO.util.Dom.hasClass(container, className))
  {
    _toggleShowHide(container);
  }

  var children = container.getElementsByTagName('*');
  for(var i = 0; i < children.length; i++)
  {
    var child = children.item(i);
    if(YAHOO.util.Dom.hasClass(child, className))
    {
      _toggleShowHide(child);
    }
  }
}

/**
 * Set or remove a class on the given element.
 *
 * @param element the element to act on
 * @param toggle true for setting, false for removing
 * @param clazz the class to set or remove
 */
function _toggleClass(element, toggle, clazz)
{
  alert('_toggleClass: Not Supported')
  if(toggle == null)
  {
    toggle = !element.hasClass(clazz);
  }

  if(toggle)
  {
    element.addClass(clazz);
  }
  else
  {
    element.removeClass(clazz);
  }
}

/**
 * Set or remove a class on the given element.
 *
 * @param elementId the element id to act on
 * @param toggle true for setting, false for removing
 * @param clazz the class to set or remove
 */
function toggleClass(elementId, toggle, clazz)
{
  alert('toggleClass: Not Supported')
  _toggleClass($(elementId), toggle, clazz);
}

/**
 * Hides the element provided its id (add the hidden class to it)
 */
function hideElement(elementId)
{
  alert('hideElement: Not Supported')
  toggleClass(elementId, true, 'hidden');
}

/**
 * Shows the element provided its id (removes the hidden class to it)
 */
function showElement(elementId)
{
  alert('showElement: Not Supported')
  toggleClass(elementId, false, 'hidden');
}

/**
 * Set or remove the 'class' for all children of containerId which have the class 'selectionClass'
 */
function toggleClassChildren(containerId, selectionClass, toggle, clazz)
{
  alert('toggleClassChildren: Not Supported')
  var container = document.getElementById(containerId);
  if(container.hasClass(selectionClass))
  {
    _toggleClass(container, toggle, clazz);
  }

  var children = container.getElementsByTagName('*');
  for(var i = 0; i < children.length; i++)
  {
    var child = children.item(i);
    if(child.hasClass(selectionClass))
    {
      _toggleClass(child, toggle, clazz);
    }
  }
}

/**
 * Returns all the children of the provided container with the provided class.
 *
 * @param containerId
 * @param selectionClass
 * @param includeContainer true if you want the container to be included if it has the right class
 */
function getAllChildrenWithClass(containerId, selectionClass, includeContainer)
{
  alert('getAllChildrenWithClass: Not Supported')
  var res = [];
  var container = document.getElementById(containerId);
  if(container.hasClass(selectionClass))
  {
    if(includeContainer)
      res.push(container);
  }

  var children = container.getElementsByTagName('*');
  for(var i = 0; i < children.length; i++)
  {
    var child = children.item(i);
    if(child.hasClass(selectionClass))
    {
      res.push(child);
    }
  }

  return res;
}


