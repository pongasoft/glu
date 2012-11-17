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

package org.linkedin.glu.agent.impl

import org.linkedin.glu.agent.api.Agent
import org.linkedin.glu.agent.impl.script.AgentContext
import org.linkedin.glu.agent.api.Shell
import org.linkedin.glu.agent.impl.script.MOP
import org.linkedin.glu.agent.api.MountPoint
import org.linkedin.glu.agent.impl.script.ScriptManagerImpl
import org.linkedin.glu.agent.impl.script.ScriptManager
import org.linkedin.glu.agent.impl.script.StateKeeperScriptManager
import org.linkedin.glu.agent.impl.capabilities.MOPImpl
import org.hyperic.sigar.Sigar
import org.hyperic.sigar.SigarException
import org.linkedin.glu.agent.api.AgentException
import java.util.concurrent.TimeoutException
import org.linkedin.util.lifecycle.Shutdownable
import org.linkedin.util.clock.Clock
import org.linkedin.util.clock.SystemClock
import org.linkedin.util.io.resource.Resource
import org.linkedin.util.clock.Timespan
import org.linkedin.glu.agent.api.TimeOutException
import org.linkedin.glu.utils.tags.Taggeable
import org.linkedin.glu.utils.tags.TaggeableTreeSetImpl
import org.linkedin.glu.agent.impl.script.ScriptNode
import java.util.concurrent.ExecutionException
import org.linkedin.glu.agent.impl.command.CommandManager
import org.linkedin.glu.agent.impl.command.CommandManagerImpl
import org.linkedin.glu.agent.api.NoSuchCommandException
import org.linkedin.glu.commands.impl.MemoryCommandExecutionIOStorage
import org.linkedin.glu.commands.impl.CommandExecution
import org.linkedin.glu.agent.impl.command.CommandGluScriptFactoryFactory

/**
 * The main implementation of the agent
 */
def class AgentImpl implements Agent, AgentContext, Shutdownable
{
  public static final String MODULE = AgentImpl.class.getName();
  public static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MODULE);

  Clock clock = SystemClock.instance()

  private Sigar _sigar
  private Shell _shellForScripts
  private Shell _shellForCommands
  private Shell _rootShell
  private Resource _agentLogDir
  private ScriptManager _scriptManager
  private CommandManager _commandManager
  private MOP _mop
  private Closure _sync
  private Taggeable _taggeable

  private volatile _shutdown = false

  /********************************************************************
   * Boot
   ********************************************************************/
  /**
   * Boots the agent.
   *
   * <ul>
   * <li><code>restartSoftware</code>: the flag indicates whether the software that were running
   * before the agent was shutdown should be started again (default to <code>false</code>)</li>
   * </ul>
   */
  void boot(args)
  {
    _shellForScripts = args.shellForScripts
    _rootShell = args.rootShell
    _shellForCommands = args.shellForCommands ?: _rootShell
    _agentLogDir = args.agentLogDir
    _sigar = args.sigar
    _mop = args.mop ?: new MOPImpl()

    def storage = args.storage
    if(storage != null)
    {
      _scriptManager = new ScriptManagerImpl(agentContext: this)
      _commandManager = new CommandManagerImpl(agentContext: this,
                                               ioStorage: new MemoryCommandExecutionIOStorage(clock: clock))
      def f = new CommandGluScriptFactoryFactory(ioStorage: _commandManager.ioStorage)
      _scriptManager.scriptFactoryFactory.chain(f)
      _scriptManager = new StateKeeperScriptManager(scriptManager: _scriptManager,
                                                    storage: storage)
      _commandManager.scriptManager = _scriptManager
    }
    else
    {
      _scriptManager = args.scriptManager
      _commandManager = args.commandManager
    }

    if(!_scriptManager.isMounted(MountPoint.ROOT))
      _scriptManager.installRootScript([:])

    _sync = args.sync
    _taggeable = args.taggeable ?: new TaggeableTreeSetImpl()
  }

  Clock getClock()
  {
    return clock
  }
  
  /**
   * Default shutdown: stops the containers and shut downs the agent
   *
   * @see #shutdown(boolean)
   */
  void shutdown()
  {
    if(!_shutdown)
    {
      _shutdown = true
      _scriptManager.shutdown()
    }
  }

  /**
   * Shuts down the agent
   *
   * @param stopSoftware <code>true</code> if the software should be stopped or
   *                     <code>false</code> if it should be left running.
   */
  void shutdown(boolean stopSoftware)
  {
    shutdown()
  }

  /**
   * Waits for the agent to be completely down (but no longer than the timeout).
   * @return <code>true</code> if it shutdown or <code>false</code> if the timeout elapsed before
   * termination
   */
  void waitForShutdown(timeout)
  {
    if(!_shutdown)
      throw new IllegalStateException('call shutdown first!')

    timeout = Timespan.parse(timeout?.toString()) ?: Timespan.ZERO_MILLISECONDS

    _scriptManager.waitForShutdown(timeout.durationInMilliseconds)
  }

  public void waitForShutdown()
  {
    waitForShutdown(0)
  }

  public getMountPoints()
  {
    handleException {
      return _scriptManager.mountPoints
    }
  }

  /**
   * {@inheritdoc}
   */
  public def getHostInfo()
  {
    handleException {
      log.info "getHostInfo()"
      def res = [:]
      if(_sigar)
      {
        // cpus
        res.cpus = _sigar.cpuInfoList?.collect { it.toMap() }

        // memory
        res.mem = _sigar.mem.toMap()
      }

      return res
    }
  }

  /**
   * {@inheritdoc}
   */
  public def ps()
  {
    handleException {
      log.info "ps()"

      def ps = [:]

      def procList = _sigar?.procList ?: []

      procList.each { pid ->
        def proc = [:]
        ps[pid] = proc

        [
            'env': 'getProcEnv',
            'args': 'getProcArgs',
            'cpu': 'getProcCpu',
            // 'cred': 'getProcCred',
            'credName': 'getProcCredName',
            'exe': 'getProcExe',
            'fd': 'getProcFd',
            'mem': 'getProcMem',
            'modules': 'getProcModules',
            'state': 'getProcState',
            'time': 'getProcTime'
        ].each { k, v ->
          try
          {
            def res = _sigar."${v}"(pid)

            if(res.respondsTo('toMap'))
              res = res.toMap()

            proc[k] = res
          }
          catch (SigarException e)
          {
            if(log.isDebugEnabled())
              log.debug("ignored exception", e)
          }
        }
      }

      return ps
    }
  }

  /**
   * {@inheritdoc}
   */
  public void kill(long pid, int signal)
  {
    handleException {
      try
      {
        log.info "Sending signal ${signal} to process ${pid}"
        _sigar?.kill(pid, signal)
      }
      catch (SigarException e)
      {
        log.warn("Exception while sending signal ${signal} to process ${pid} (ignored)", e)
        throw new AgentException(e)
      }
    }
  }

  /**
   * {@inheritdoc}
   */
  public void sync()
  {
    handleException {
      log.info "sync()"

      if(_sync)
        _sync()
    }
  }

  /**
   * {@inheritdoc}
   */
  void installScript(args)
  {
    handleException {
      if(log.isDebugEnabled())
        log.debug("installScript(${args})")
      _scriptManager.installScript(args)
    }
  }

  /**
   * {@inheritdoc}
   */
  void uninstallScript(args)
  {
    handleException {
      if(log.isDebugEnabled())
        log.debug("uninstallScript(${args})")

      def force = args.force != null ? args.force : false
      if(force instanceof String)
        force = Boolean.parseBoolean(force)
      if(force)
        interruptAction(args)
      _scriptManager.uninstallScript(args.mountPoint, force)
    }
  }

  /**
   * {@inheritdoc}
   */
  String executeAction(args)
  {
    handleException {
      if(log.isDebugEnabled()) {
        log.debug("executeAction(" + args + ")")
      }

      _scriptManager.executeAction(args).id
    }
  }

  /**
   * {@inheritdoc}
   */
  def waitForAction(args)
  {
    handleException {
      if(log.isDebugEnabled())
        log.debug("waitForAction(${args})")

      _scriptManager.waitForAction(args)
    }
  }

  /**
   * {@inheritdoc}
   */
  def executeActionAndWait(args)
  {
    handleException {
      if(log.isDebugEnabled())
        log.debug("executeActionAndWait(${args})")
      def future = _scriptManager.executeAction(args)
      def timeout = Timespan.parse(args.timeout?.toString()) ?: Timespan.ZERO_MILLISECONDS
      try
      {
        def res = future.get(timeout)
        if(log.isDebugEnabled())
        {
          if(res instanceof InputStream)
            log.debug("executeActionAndWait(${args}): InputStream")
          else
            log.debug("executeActionAndWait(${args}): ${res}")
        }
        return res
      }
      catch(ExecutionException e)
      {
        throw e.cause
      }
      catch (TimeoutException e)
      {
        if(log.isDebugEnabled())
          log.debug("executeActionAndWait(${args}): TimeoutException[${e.message}]")
        throw new TimeoutException()
      }
    }
  }

  /*
  * {@inheritdoc}
  */
  boolean interruptAction(args)
  {
    handleException {
      if(log.isDebugEnabled())
        log.debug("interruptAction(${args})")

      _scriptManager.interruptAction(args)
    }
  }

  /**
   * {@inheritdoc}
   */
  void clearError(args)
  {
    handleException {
      if(log.isDebugEnabled())
        log.debug("clearError(" + args + ")")

      _scriptManager.clearError(MountPoint.create(args.mountPoint))
    }
  }

  /**
   * {@inheritdoc}
   */
  def executeCall(args)
  {
    handleException {
      if(log.isDebugEnabled())
        log.debug("executeCall(" + args + ")")

      return _scriptManager.executeCall(args)
    }
  }

  /**
   * {@inheritdoc}
   */
  public getState(args)
  {
    handleException {
      if(log.isDebugEnabled())
        log.debug("getState(" + args + ")")

      return _scriptManager.getState(args.mountPoint)
    }
  }

  /**
   * {@inheritdoc}
   */
  public getFullState(args)
  {
    handleException {
      if(log.isDebugEnabled())
        log.debug("getFullState(" + args + ")")

      return _scriptManager.getFullState(args.mountPoint);
    }
  }

  /**
   * {@inheritdoc}
   */
  boolean waitForState(args)
  {
    handleException {
      if(log.isDebugEnabled())
        log.debug("waitForState(${args})")
      return _scriptManager.waitForState(args)
    }
  }

  /**
   * {@inheritdoc}
   */
  boolean executeActionAndWaitForState(Object args)
  {
    executeAction(args)
    return waitForState(args);
  }

  /**
   * {@inheritdoc}
   */
  InputStream tailAgentLog(args)
  {
    handleException {
      args.location = _agentLogDir.createRelative(args.log?.toString() ?: "${_rootShell.env['org.linkedin.app.name']}.out")
      getFileContent(args)
    }
  }

  /**
   * {@inheritdoc}
   */
  def getFileContent(args) throws IOException, AgentException
  {
    handleException {
      log.info "getFileContent: ${args}"

      def location = _rootShell.toResource(args.location)

      if(location.isDirectory())
      {
        def resources = _rootShell.ls(location)
        def res = [:]
        resources.each {
          def file = it.file
          def details = [:]
          res[file.name] = details
          details.canonicalPath = file.canonicalPath
          details.length = file.length()
          details.lastModified = file.lastModified()
          details.isDirectory = file.isDirectory()
        }
        return res
      }
      else
      {
        return _rootShell.tail(args)
      }
    }
  }

  @Override
  def executeShellCommand(args)
  {
    handleException {
      if(log.isDebugEnabled())
      {
        def argsNoStdin = [*:args]
        def stdin = argsNoStdin.remove('stdin')
        log.debug("executeShellCommand(${argsNoStdin}${stdin ? ' - stdin ' : ''})")
      }
      [id: _commandManager.executeShellCommand(args).id]
    }
  }

  @Override
  def waitForCommand(args)
  {
    handleException {
      if(log.isDebugEnabled())
        log.debug("waitForCommand(${args})")
      _commandManager.waitForCommand(args)
    }
  }

  @Override
  def streamCommandResults(args)
  {
    handleException {
      if(log.isDebugEnabled())
        log.debug("streamCommandResults(${args})")

      def res = _commandManager.findCommandExecutionAndStreams(args)
      if(res == null)
        throw new NoSuchCommandException(args.id)

      CommandExecution commandExecution = res.remove('commandExecution')

      if(commandExecution.startTime > 0)
        res.startTime = commandExecution.startTime

      if(commandExecution.isCompleted())
        res.completionTime = commandExecution.completionTime

      return res
    }
  }

  @Override
  boolean interruptCommand(args)
  {
    handleException {
      if(log.isDebugEnabled())
        log.debug("interruptCommand(${args})")

      _commandManager.interruptCommand(args)
    }
  }

  public Shell getShellForScripts()
  {
    return _shellForScripts;
  }

  @Override
  Shell getShellForCommands()
  {
    return _shellForCommands
  }

  public MOP getMop()
  {
    return _mop;
  }

  @Override
  int getTagsCount()
  {
    handleException {
      return _taggeable.tagsCount
    }
  }

  @Override
  boolean hasTags()
  {
    handleException {
      return _taggeable.hasTags()
    }
  }

  @Override
  Set<String> getTags()
  {
    handleException {
      return _taggeable.tags
    }
  }

  @Override
  boolean hasTag(String tag)
  {
    handleException {
      return _taggeable.hasTag(tag)
    }
  }

  @Override
  boolean hasAllTags(Collection<String> tags)
  {
    handleException {
      return _taggeable.hasAllTags(tags)
    }
  }

  @Override
  boolean hasAnyTag(Collection<String> tags)
  {
    handleException {
      return _taggeable.hasAnyTag(tags)
    }
  }

  @Override
  boolean addTag(String tag)
  {
    handleException {
      log.info "adding tag: ${tag}"
      return _taggeable.addTag(tag)
    }
  }

  @Override
  Set<String> addTags(Collection<String> tags)
  {
    handleException {
      log.info "adding tags: ${tags}"
      return _taggeable.addTags(tags)
    }
  }

  @Override
  boolean removeTag(String tag)
  {
    handleException {
      log.info "removing tag: ${tag}"
      return _taggeable.removeTag(tag)
    }
  }

  @Override
  Set<String> removeTags(Collection<String> tags)
  {
    handleException {
      log.info "removing tags: ${tags}"
      return _taggeable.removeTags(tags)
    }
  }

  @Override
  void setTags(Collection<String> tags)
  {
    handleException {
      log.info "setting tags: ${tags}"
      return _taggeable.setTags(tags)
    }
  }

  private <T> T handleException(Closure closure)
  {
    try
    {
      return closure()
    }
    catch(RuntimeException e)
    {
      throw e
    }
    catch(TimeoutException e)
    {
      // adapting timeout exception...
      def toex = new TimeOutException(e.message)
      toex.initCause(e)
      throw toex
    }
    catch (AgentException e)
    {
      throw e
    }
    catch(Throwable th)
    {
      throw new AgentException('unexpected exception', th)
    }
  }

  /**
   * @return a script previously installed (or <code>null</code> if not found)
   */
  ScriptNode findScript(mountPoint)
  {
    return _scriptManager.findScript(mountPoint)
  }
}