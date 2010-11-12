package org.linkedin.glu.agent.tracker

/**
 * @author ypujante@linkedin.com */
class PrefixAgentInfoPropertyAccessor implements AgentInfoPropertyAccessor
{
  static AgentInfoPropertyAccessor DEFAULT = new PrefixAgentInfoPropertyAccessor(prefix: 'glu')

  String prefix

  def getPropertyValue(AgentInfo agentInfo, String propertyName)
  {
    return agentInfo.agentProperties["${prefix}.${propertyName}".toString()];
  }

}
