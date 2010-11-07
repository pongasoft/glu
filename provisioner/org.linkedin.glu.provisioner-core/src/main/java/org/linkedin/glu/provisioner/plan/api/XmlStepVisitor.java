/*
 * Copyright 2010-2010 LinkedIn, Inc
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

package org.linkedin.glu.provisioner.plan.api;

import org.linkedin.util.xml.XMLIndent;

import java.util.Map;

/**
 * @author ypujante@linkedin.com
 */
public class XmlStepVisitor<T> implements IStepVisitor<T>
{
  private final XMLIndent _xml;
  private final String _tagName;
  private final Map<String, Object> _metadata;

  /**
   * Constructor
   */
  public XmlStepVisitor()
  {
    this(new XMLIndent(), null, null);
  }

  /**
   * Constructor
   */
  public XmlStepVisitor(XMLIndent xml, String tagName, Map<String, Object> metadata)
  {
    _xml = xml;
    _tagName = tagName;
    _metadata = metadata;
  }

  @Override
  public void startVisit()
  {
    if(_tagName != null)
    {
      _xml.addOpeningTag(_tagName, _metadata);
    }
  }

  @Override
  public void visitLeafStep(LeafStep<T> step)
  {
    _xml.addEmptyTag("leaf", step.getMetadata());
  }

  @Override
  public IStepVisitor<T> visitSequentialStep(final SequentialStep<T> step)
  {
    return new XmlStepVisitor<T>(_xml, "sequential", step.getMetadata());
  }

  @Override
  public IStepVisitor<T> visitParallelStep(ParallelStep<T> step)
  {
    return new XmlStepVisitor<T>(_xml, "parallel", step.getMetadata());
  }

  @Override
  public void endVisit()
  {
    if(_tagName != null)
    {
      _xml.addClosingTag(_tagName);
    }
  }

  public XMLIndent getXml()
  {
    return _xml;
  }

  public Map<String, Object> getMetadata()
  {
    return _metadata;
  }

  public String getTagName()
  {
    return _tagName;
  }
}
