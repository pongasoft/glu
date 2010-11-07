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

import org.linkedin.util.lang.LangUtils;
import org.linkedin.util.xml.XMLIndent;

import java.util.Map;
import java.util.LinkedHashMap;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * @author ypujante@linkedin.com
 */
public class XmlStepCompletionStatusVisitor<T> implements IStepCompletionStatusVisitor<T>
{
  private final XMLIndent _xml;
  private final String _tagName;
  private final Map<String, Object> _attributes;
  private final Throwable _throwable;

  private static final DateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");

  /**
   * Constructor
   */
  public XmlStepCompletionStatusVisitor()
  {
    _xml = new XMLIndent();
    _tagName = null;
    _attributes = null;
    _throwable = null;
  }

  /**
   * Constructor
   */
  public XmlStepCompletionStatusVisitor(XMLIndent xml, String tagName, String nameValue)
  {
    _xml = xml;
    _tagName = tagName;
    _throwable = null;
    _attributes = new LinkedHashMap<String, Object>();
    _attributes.put("name", nameValue);
  }

  /**
   * Constructor
   */
  public XmlStepCompletionStatusVisitor(XMLIndent xml,
                                        String tagName,
                                        IStepCompletionStatus<T> status)
  {
    _xml = xml;
    _tagName = tagName;
    _attributes = getAttributes(status);
    _throwable = status.getThrowable();
  }

  @Override
  public void startVisit()
  {
    if(_tagName != null)
    {
      _xml.addOpeningTag(_tagName, _attributes);
      if(_throwable != null)
      {
        _xml.addTag("exception",
                    LangUtils.getStackTrace(_throwable),
                    "message",
                    _throwable.getMessage());
      }
    }
  }

  /**
   * Visits status of a leaf step.
   */
  @Override
  public void visitLeafStepStatus(IStepCompletionStatus<T> status)
  {
    addTag("leaf", getAttributes(status), status.getThrowable());
  }

  /**
   * Visit status of a sequential step.
   *
   * @return <code>null</code> if you want to stop the recursion, otherwise another visitor
   */
  @Override
  public IStepCompletionStatusVisitor<T> visitSequentialStepStatus(IStepCompletionStatus<T> status)
  {
    return new XmlStepCompletionStatusVisitor<T>(_xml, "sequential", status);
  }

  /**
   * Visit status a parallel step.
   *
   * @return <code>null</code> if you want to stop the recursion, otherwise another visitor
   */
  @Override
  public IStepCompletionStatusVisitor<T> visitParallelStepStatus(IStepCompletionStatus<T> status)
  {
    return new XmlStepCompletionStatusVisitor<T>(_xml, "parallel", status);
  }

  @Override
  public void endVisit()
  {
    if(_tagName != null)
    {
      _xml.addClosingTag(_tagName);
    }
  }

  private void addTag(String tagName, Map<String, Object> attributes, Throwable th)
  {
    if(th != null)
    {
      _xml.addOpeningTag(tagName, attributes);
      _xml.addTag("exception", LangUtils.getStackTrace(th), "message", th.getMessage());
      _xml.addClosingTag(tagName);
    }
    else
    {
      _xml.addEmptyTag(tagName, attributes);
    }
  }

  private Map<String, Object> getAttributes(IStepCompletionStatus<T> status)
  {
    Map<String, Object> attributes = new LinkedHashMap<String, Object>();

    attributes.putAll(status.getStep().getMetadata());
    attributes.put("startTime", formatTime(status.getStartTime()));
    attributes.put("endTime", formatTime(status.getEndTime()));
    attributes.put("status", status.getStatus().name());

    return attributes;
  }

  public XMLIndent getXml()
  {
    return _xml;
  }

  public Map getAttributes()
  {
    return _attributes;
  }

  public String getTagName()
  {
    return _tagName;
  }

  public static String formatTime(long time)
  {
    synchronized(DATE_FORMATTER)
    {
      return DATE_FORMATTER.format(time);
    }
  }
}