package org.pongasoft.glu.provisioner.core.metamodel;

/**
 * Represents the agent upgrade only (all configuration happens for the full agent)
 *
 * @author yan@pongasoft.com
 */
public interface AgentUpgradeMetaModel extends MetaModel
{
  /**
   * @return the agent associated to it
   */
  AgentMetaModel getAgent();
}