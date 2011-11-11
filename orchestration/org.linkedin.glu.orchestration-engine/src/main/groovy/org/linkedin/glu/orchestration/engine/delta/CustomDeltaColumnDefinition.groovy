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

package org.linkedin.glu.orchestration.engine.delta

import org.linkedin.glu.utils.core.Externable

/**
 * @author yan@pongasoft.com */
public class CustomDeltaColumnDefinition implements Externable
{
  public static def SUPPORTED_GROUP_BY =
    [ "uniqueVals", "uniqueCount", "uniqueCountOrUniqueVal", "vals", "count", "min", "max" ] as Set

  public static def SUPPORTED_ORDER_BY =
    [ "asc", "desc", null ] as Set

  /**
   * The name of the column (use in the display)
   */
  String name

  /**
   * Which entry to 'select' for this column in the flattened delta (ex: <code>metadata.container.name</code>)
   */
  String source

  /**
   * Which 'function' to apply when there is more than 1 entry and it is a summary view. Default
   * to <code>uniqueCountOrUniqueVal</code>.
   *
   * @see #SUPPORTED_GROUP_BY for the list of supported values
   */
  String groupBy = "uniqueCountOrUniqueVal"

  /**
   * Which order to use when not a summary view for this column. The <code>null</code> value specifies
   * that this column should be ignored in the global sorting of the row.
   *
   * @see #SUPPORTED_ORDER_BY for the list of supported values */
  String orderBy = 'asc'

  /**
   * Whether the values should generate links */
  boolean linkable = true

  /**
   * Whether this column should be rendered or not
   */
  boolean visible = true

  void setGroupBy(String gb)
  {
    if(!SUPPORTED_GROUP_BY.contains(gb))
      throw new IllegalArgumentException("${gb} is not a valid group by value [${SUPPORTED_GROUP_BY.join(',')}]")

    groupBy = gb
  }

  void setOrderBy(String ob)
  {
    if(ob != null && !SUPPORTED_ORDER_BY.contains(ob))
      throw new IllegalArgumentException("${ob} is not a valid order by value [${SUPPORTED_ORDER_BY.join(',')}]")

    orderBy = ob
  }

  @Override
  protected Object clone()
  {
    fromExternalRepresentation(toExternalRepresentation())
  }

  @Override
  def toExternalRepresentation()
  {
    return [
      name: name,
      source: source,
      groupBy: groupBy,
      orderBy: orderBy,
      linkable: linkable,
      visible: visible
    ]
  }

  def groupBy(Collection values)
  {
    if(values == null)
      return null

    Closure groupByClosure = this."groupBy_${groupBy}"

    return groupByClosure(values)
  }

  private Closure groupBy_uniqueVals = { Collection values ->
    values.flatten().unique()
  }

  private Closure groupBy_uniqueCount = { Collection values ->
    [count: values.flatten().unique().size()]
  }

  private Closure groupBy_uniqueCountOrUniqueVal = { Collection values ->
    Collection uniqueValues = values.flatten().unique()

    if(uniqueValues.size() == 1)
      return uniqueValues[0]
    else
      [count: uniqueValues.size()]
  }

  private Closure groupBy_vals = { Collection values ->
    values.flatten()
  }

  private Closure groupBy_count = { Collection values ->
    [count: values.size()]
  }

  private Closure groupBy_min = { Collection values ->
    values.min()
  }

  private Closure groupBy_max = { Collection values ->
    values.max()
  }

  static CustomDeltaColumnDefinition fromExternalRepresentation(def er)
  {
    new CustomDeltaColumnDefinition(er)
  }
}