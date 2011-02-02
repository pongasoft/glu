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

package org.linkedin.glu.utils.tags;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author yan@pongasoft.com
 */
public class TaggeableTreeSetImpl extends TaggeableSetImpl
{
  public TaggeableTreeSetImpl()
  {
  }

  public TaggeableTreeSetImpl(Collection<String> tags)
  {
    super(tags);
  }

  @Override
  protected Set<String> createEmptySet()
  {
    return new TreeSet<String>();
  }

  @Override
  protected Set<String> createSet(Collection<String> tags)
  {
    return new TreeSet<String>(tags);
  }
}
