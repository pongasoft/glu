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
 * @param containerId the container to show/hide
*/
function toggleShowHide(selector) {
  toggleClass(selector, null, 'hidden')
}

/*
 * Toggles a class
 *
 * @param selector the selector to select on which elements this should apply
 * @param toggle true means addClass, false means remove class, null means toggle
 * @param clazz which class to toggle
*/
function toggleClass(selector, toggle, clazz) {
  if(toggle == null)
  {
    $(selector).toggleClass(clazz)
  }
  else
  {
    if(toggle)
    {
      $(selector).addClass(clazz)
    }
    else
    {
      $(selector).removeClass(clazz)
    }
  }
}

/**
 * Hides the elements that the selector selects (add the hidden class to it)
 */
function hide(selector)
{
  toggleClass(selector, true, 'hidden');
}

/**
 * Shows the elements that the selector selects (removes the hidden class to it)
 */
function show(selector)
{
  toggleClass(selector, false, 'hidden');
}
