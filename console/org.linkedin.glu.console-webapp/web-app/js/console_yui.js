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

/*
 * Toggle show/hide by adding/removing the class 'hidden'
 * @param container the container to show/hide
*/
function _toggleShowHide(container) {
  if(YAHOO.util.Dom.hasClass(container, 'hidden'))
  {
    YAHOO.util.Dom.removeClass(container, 'hidden');
  }
  else
  {
    YAHOO.util.Dom.addClass(container, 'hidden');
  }
}

/*
 * Toggle show/hide by adding/removing the class 'hidden'
 * @param containerId where to start looking for children
 * @param className only select the children that have the provided className
 */
function toggleShowHideChildren(containerId, className) {
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

/*
 * Toggle show/hide by adding/removing the class 'hidden'
 * @param containerId the container to show/hide
*/
function toggleShowHide(containerId) {
  _toggleShowHide(document.getElementById(containerId));
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
  if(toggle == null)
  {
    toggle = !YAHOO.util.Dom.hasClass(element, clazz);
  }

  if(toggle)
  {
    YAHOO.util.Dom.addClass(element, clazz);
  }
  else
  {
    YAHOO.util.Dom.removeClass(element, clazz);
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
  _toggleClass(document.getElementById(elementId), toggle, clazz);
}

/**
 * Hides the element provided its id (add the hidden class to it)
 */
function hideElement(elementId)
{
  toggleClass(elementId, true, 'hidden');
}

/**
 * Shows the element provided its id (removes the hidden class to it)
 */
function showElement(elementId)
{
  toggleClass(elementId, false, 'hidden');
}

/**
 * Set or remove the 'class' for all children of containerId which have the class 'selectionClass'
 */
function toggleClassChildren(containerId, selectionClass, toggle, clazz)
{
  var container = document.getElementById(containerId);
  if(YAHOO.util.Dom.hasClass(container, selectionClass))
  {
    _toggleClass(container, toggle, clazz);
  }

  var children = container.getElementsByTagName('*');
  for(var i = 0; i < children.length; i++)
  {
    var child = children.item(i);
    if(YAHOO.util.Dom.hasClass(child, selectionClass))
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
  var res = [];
  var container = document.getElementById(containerId);
  if(YAHOO.util.Dom.hasClass(container, selectionClass))
  {
    if(includeContainer)
      res.push(container);
  }

  var children = container.getElementsByTagName('*');
  for(var i = 0; i < children.length; i++)
  {
    var child = children.item(i);
    if(YAHOO.util.Dom.hasClass(child, selectionClass))
    {
      res.push(child);
    }
  }

  return res;
}


