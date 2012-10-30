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

package org.linkedin.glu.agent.server

import org.hyperic.sigar.Sigar
import org.hyperic.sigar.SigarException
import org.linkedin.glu.agent.api.Agent
import org.linkedin.glu.agent.api.Shell
import org.linkedin.glu.agent.impl.AgentImpl
import org.linkedin.glu.agent.impl.capabilities.ShellImpl
import org.linkedin.glu.agent.impl.storage.DualWriteStorage
import org.linkedin.glu.agent.impl.storage.FileSystemStorage
import org.linkedin.glu.agent.impl.storage.Storage
import org.linkedin.glu.agent.impl.zookeeper.ZooKeeperStorage
import org.linkedin.glu.agent.rest.resources.AgentResource
import org.linkedin.glu.agent.rest.resources.FileResource
import org.linkedin.glu.agent.rest.resources.HostResource
import org.linkedin.glu.agent.rest.resources.LogResource
import org.linkedin.glu.agent.rest.resources.MountPointResource
import org.linkedin.glu.agent.rest.resources.ProcessResource
import org.linkedin.groovy.util.ant.AntUtils
import org.linkedin.groovy.util.io.GroovyIOUtils
import org.linkedin.groovy.util.io.fs.FileSystemImpl
import org.linkedin.groovy.util.ivy.IvyURLHandler
import org.linkedin.groovy.util.net.GroovyNetUtils
import org.linkedin.groovy.util.net.SingletonURLStreamHandlerFactory
import org.linkedin.util.codec.Base64Codec
import org.linkedin.util.codec.Codec
import org.linkedin.util.codec.CodecUtils
import org.linkedin.util.codec.OneWayCodec
import org.linkedin.util.codec.OneWayMessageDigestCodec
import org.linkedin.util.lifecycle.Shutdown
import org.linkedin.util.lifecycle.ShutdownProxy
import org.linkedin.util.reflect.ObjectProxyBuilder
import org.linkedin.zookeeper.client.IZKClient
import org.linkedin.zookeeper.client.LifecycleListener
import org.linkedin.zookeeper.client.ZooKeeperURLHandler
import org.restlet.util.Series
import org.restlet.routing.Router
import org.linkedin.groovy.util.config.Config
import org.restlet.Component
import org.restlet.data.Protocol
import org.restlet.routing.Template
import org.linkedin.groovy.util.log.JulToSLF4jBridge
import org.linkedin.glu.agent.rest.resources.TagsResource
import org.linkedin.glu.agent.impl.storage.AgentProperties
import org.linkedin.glu.agent.impl.storage.TagsStorage
import org.linkedin.glu.agent.impl.storage.WriteOnlyStorage
import org.linkedin.util.lifecycle.Configurable
import org.linkedin.glu.agent.rest.resources.AgentConfigResource
import org.linkedin.glu.agent.rest.resources.CommandsResource
import org.linkedin.glu.agent.rest.resources.CommandExitValueResource
import org.linkedin.glu.agent.rest.resources.CommandStreamsResource
import org.linkedin.glu.agent.impl.command.CommandManager
import org.linkedin.glu.utils.core.DisabledFeatureProxy
import org.linkedin.glu.agent.impl.command.CommandManagerImpl
import org.linkedin.glu.agent.impl.command.MemoryCommandExecutionIOStorage

/**
 * This is the main class to start the agent.
 *
 * @author ypujante@linkedin.com
 */
class AgentMain implements LifecycleListener, Configurable
{
  public static final String MODULE = AgentMain.class.getName();
  public static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MODULE);

  private static final Codec TWO_WAY_CODEC
  private static final OneWayCodec ONE_WAY_CODEC
  private static final OneWayCodec ONE_WAY_CODEC_2

  static {
    String p0 = "gluos2way"
    TWO_WAY_CODEC = new Base64Codec(p0)
    String p1 = "gluos1way1"
    ONE_WAY_CODEC =
      OneWayMessageDigestCodec.createSHA1Instance(p1, TWO_WAY_CODEC)
    String p2 = "gluos1way2"
    ONE_WAY_CODEC_2 =
      OneWayMessageDigestCodec.createSHA1Instance(p1, new Base64Codec(p2))
  }

  protected File _persistentPropertiesFile
  protected IZKClient _zkClient
  protected def _urlFactory
  protected String _fabric
  protected Sigar _sigar
  protected long _pid
  protected Closure hostnameFactory

  protected def _config
  protected AgentProperties _agentProperties

  protected File _agentTempDir
  protected String _agentName
  protected String _zooKeeperRoot
  protected Shutdown _shutdown
  protected Agent _proxiedAgent
  protected AgentImpl _agent
  protected def _restServer
  protected DualWriteStorage _dwStorage = null
  protected Storage _storage = null

  protected final Object _lock = new Object()
  protected volatile boolean _receivedShutdown = false

  AgentMain()
  {
    JulToSLF4jBridge.installBridge()
  }

  void init(args)
  {
    // register url factory
    _urlFactory = new SingletonURLStreamHandlerFactory()
    URL.setURLStreamHandlerFactory(_urlFactory)

    // read all the properties provided as urls on the command line
    Properties configFromCli = new Properties()

    args.each {
      readConfig(it, configFromCli)
    }

    // make sure all directories have their canonical representation
    toCanonicalPath(configFromCli)

    Properties configFromPreviousStart = new Properties()

    _persistentPropertiesFile = loadPersistentProperties(configFromCli, configFromPreviousStart)

    // we merge the 2: cli overrides 'remembered' properties
    Properties config = mergeConfig(configFromCli, configFromPreviousStart)

    // determine the host name of the agent
    def hf = computeHostnameFactory(config)
    hostnameFactory = hf.factory

    _agentName = Config.getOptionalString(config,
                                          "${prefix}.agent.name",
                                          InetAddress.getLocalHost().canonicalHostName)

    configFromCli["${prefix}.agent.name".toString()] = _agentName
    configFromCli["${prefix}.agent.hostnameFactory".toString()] = hf.propertyValue
    configFromCli["${prefix}.agent.hostname".toString()] = hostnameFactory()
    configFromCli["${prefix}.agent.version".toString()] = config['org.linkedin.app.version']

    log.info "Agent ZooKeeper name: ${_agentName}"

    _zooKeeperRoot = Config.getRequiredString(config, "${prefix}.agent.zookeeper.root")

    // create zookeeper client and registers a url handler with it
    _zkClient = createZooKeeperClient(config)

    if(_zkClient)
    {
      // add support for zookeeper:/a/b links
      _urlFactory.registerHandler('zookeeper') {
        return new ZooKeeperURLHandler(_zkClient)
      }
    }

    _fabric = computeFabric(configFromCli, config)

    // makes the fabric available
    configFromCli["${prefix}.agent.fabric".toString()] = _fabric

    log.info "Agent fabric: ${_fabric}"

    // read the config provided in configURL (most likely in zookeeper, this is why we
    // need to register the handler beforehand)
    // properties coming from ZooKeeper have higher priority than 'remembered' properties but lower
    // priority than cli!
    readConfig(Config.getOptionalString(config, "${prefix}.agent.configURL", null), configFromCli)

    // make sure all directories have their canonical representation
    toCanonicalPath(configFromCli)

    config = mergeConfig(configFromCli, configFromPreviousStart)

    // dealing with optional properties
    setOptionalProperty(config, "${prefix}.agent.port", "12906")
    setOptionalProperty(config, "${prefix}.agent.sslEnabled", "true")
    setOptionalProperty(config, "${prefix}.agent.rest.server.defaultThreads", '3')
    setOptionalProperty(config, "${prefix}.agent.features.commands.enabled", "true")
    if(_zkClient)
      setOptionalProperty(config,
                          "${prefix}.${IZKClientFactory.ZK_CONNECT_STRING}",
                          _zkClient.connectString)



    _agentProperties = new AgentProperties(config)

    _sigar = createSigar()

    _pid = getAgentPid()
    if(_pid)
    {
      _agentProperties["${prefix}.agent.pid".toString()] = _pid
    }

    savePersistentProperties(config, _agentProperties)

    _config = config

    log.info("Starting the agent with config: ${new TreeMap(_agentProperties.exposedProperties)}")
  }

  /**
   * Use the <code>glu.agent.hostname</code> config value to determine how to compute
   * hostname.
   */
  protected def computeHostnameFactory(Properties config)
  {
    Closure res

    def hf = Config.getOptionalString(config,
                                      "${prefix}.agent.hostnameFactory",
                                      ':ip')

    switch(hf)
    {
      case ':ip':
        res = { InetAddress.getLocalHost().hostAddress }
        break

      case ':canonical':
        res = { InetAddress.getLocalHost().canonicalHostName }
        break

      default:
        res = { hf }
        break
    }

    return [propertyValue: hf, factory: res]
  }

  /**
   * Loads the persistent properties which will serve as default values for whatever is not
   * specified.
   *
   * @param config to determine if/where to load the persistent properties from (this object will
   *        *not* be modified by this method)
   * @param persistentProperties will be populated with the persistent properties
   * @return the file they were loaded from (to write to it) or <code>null</code> if we should
   * not store any persistent properties
   */
  File loadPersistentProperties(Properties config, Properties persistentProperties)
  {
    String persistentPropertiesFilename =
      Config.getOptionalString(config, "${prefix}.agent.persistent.properties", null)

    if(persistentPropertiesFilename == null)
      return null

    AgentProperties agentProperties = new AgentProperties()

    File ppf = new File(persistentPropertiesFilename)

    agentProperties.load(ppf)

    persistentProperties.putAll(agentProperties.persistentProperties)

    return ppf
  }

  @Override
  void configure(Map config)
  {
    Map newPersistentProperties = new HashMap(_agentProperties.persistentProperties)
    newPersistentProperties.putAll(config)
    AgentProperties newAgentProperties = new AgentProperties(newPersistentProperties)

    File persistentProperties = savePersistentProperties(_agentProperties.persistentProperties,
                                                         newAgentProperties)

    // TODO MED YP: this should trigger automatic restart of the agent
    if(persistentProperties &&
       newAgentProperties.persistentProperties != _agentProperties.persistentProperties)
      log.warn "Persistent properties have been updated => agent needs to be restarted to take into account!"
  }

  synchronized File savePersistentProperties(config, AgentProperties agentProperties)
  {
    String persistentPropertiesFilename =
      Config.getOptionalString(config, "${prefix}.agent.persistent.properties", null)

    if(persistentPropertiesFilename == null)
      return null

    File ppf = new File(persistentPropertiesFilename)

    agentProperties.save(ppf)

    return ppf
  }

  /**
   * Ensures that a given property is set with at least its default value
   */
  private void setOptionalProperty(properties, String propertyName, String defaultPropertyValue)
  {
    properties[propertyName] = Config.getOptionalString(properties,
                                                        propertyName,
                                                        defaultPropertyValue)
  }

  String getPrefix()
  {
    return 'glu'
  }

  /**
   * Make sure that the paths are canonical (no .. or links)
   * @param config
   * @param name
   */
  protected void toCanonicalPath(config, name)
  {
    def path = Config.getOptionalString(config, name, null)
    if(path)
    {
      path = new File(path).canonicalPath
      config[name] = path
    }
  }

  /**
   * Make sure that the paths are canonical (no .. or links)
   * @param config
   */
  protected void toCanonicalPath(Properties config)
  {
    config.keySet().findAll { it.endsWith('Dir') }.each { toCanonicalPath(config, it) }
  }

  /**
   * Computes the fabric
   */
  protected String computeFabric(def configFromCli, def config)
  {
    File fabricFile = GroovyIOUtils.toFile(Config.getOptionalString(config,
                                                                    "${prefix}.agent.fabricFile",
                                                                    null))

    def fabricPropertyName = "${prefix}.agent.fabric".toString()

    def mgr = new FabricManager(_zkClient,
                                computeZKAgentFabricPath(),
                                Config.getOptionalString(configFromCli, fabricPropertyName, null),
                                Config.getOptionalString(config, fabricPropertyName, null),
                                fabricFile)

    String fabric = mgr.getFabric()

    if(!fabric)
      throw new IllegalStateException("cannot determine the fabric for the agent")

    return fabric
  }

  protected String computeZKAgentFabricPath()
  {
    return "${_zooKeeperRoot}/agents/names/${_agentName}"
  }

  protected def getRemoteConfigCodec()
  {
    return ONE_WAY_CODEC_2
  }

  protected def getTwoWayCodec()
  {
    TWO_WAY_CODEC
  }

  protected def getOneWayCodec()
  {
    ONE_WAY_CODEC
  }

  protected IZKClient createZooKeeperClient(def config)
  {
    def factory = new IZKClientFactory(config: config, codec: remoteConfigCodec, prefix: prefix)
    IZKClient zkClient = factory.create()

    if(zkClient)
    {
      zkClient.start()
      zkClient.waitForStart(null)
    }
    
    return zkClient
  }

  def startAndWait()
  {
    start()
    awaitTermination()
  }

  def start()
  {
    start(true)
  }

  def start(boolean withTerminationHandler)
  {
    _shutdown = new Shutdown()
    _agent = new AgentImpl()
    _agentTempDir = GroovyIOUtils.toFile(Config.getRequiredString(_config, "${prefix}.agent.tempDir"))
    _storage = createStorage()

    _zkClient?.registerListener(this)

    TagsStorage tagsStorage = new TagsStorage(_storage, "${prefix}.agent.tags".toString())

    def rootShell = createRootShell()

    def agentArgs =
    [
      rootShell: rootShell,
      shellForScripts: createShell(rootShell, "${prefix}.agent.scriptRootDir"),
      commandManager: createCommandsManager(),
      agentLogDir: rootShell.toResource(Config.getRequiredString(_config, "${prefix}.agent.logDir")),
      storage: _storage,
      sigar: _sigar,
      sync: _zkSync,
      taggeable: tagsStorage
    ]

    _agent.boot(agentArgs)

    _proxiedAgent = ObjectProxyBuilder.createProxy(new ShutdownProxy(_agent, _shutdown),
                                                   Agent.class)

    startRestServer()

    if(withTerminationHandler)
      registerTerminationHandler()

    log.info 'Agent started.'
  }

  def registerTerminationHandler()
  {
    addShutdownHook(stop)
  }

  def stop = {
    log.info 'Shutting down...'

    synchronized(_lock) {
      _receivedShutdown = true
      _lock.notify()
    }

    // first we make sure that no calls can come in and that all pending calls have
    // gone through
    _shutdown.shutdown()
    _shutdown.waitForShutdown()

    if(_restServer)
    {
      log.info 'Stopping REST service...'
      _restServer.stop()
      log.info 'REST service stopped.'
    }

    if(_agent)
    {
      log.info 'Shutting down the agent...'
      _agent.shutdown()
      _agent.waitForShutdown()
      log.info 'Agent shut down...'
    }

    if(_zkClient)
    {
      log.info 'Stopping ZooKeeper client...'
      _zkClient.destroy()
      _zkClient = null
      log.info 'ZooKeeper client stopped.'
    }


    log.info 'Shutdown sequence complete.'
  }

  def startRestServer()
  {
    def port = Config.getOptionalInt(_config, "${prefix}.agent.port", 12906)

    _restServer = new Component();
    def context = _restServer.getContext().createChildContext()
    def router = new Router(context)
    def attributes = context.getAttributes()

    attributes.put('agent', _proxiedAgent)
    attributes.put('shellForCommands', _agent.shellForCommands)
    attributes.put('configurable', this)
    attributes.put('codec', remoteConfigCodec)

    [
            agent: [clazz: AgentResource, matchingMode: Template.MODE_STARTS_WITH],
            host: [clazz: HostResource],
            config: [clazz: AgentConfigResource],
            process: [clazz: ProcessResource, matchingMode: Template.MODE_STARTS_WITH],
            mountPoint: [clazz: MountPointResource, matchingMode: Template.MODE_STARTS_WITH],
            log: [clazz: LogResource, matchingMode: Template.MODE_STARTS_WITH],
            file: [clazz: FileResource, matchingMode: Template.MODE_STARTS_WITH],
            tags: [clazz: TagsResource, matchingMode: Template.MODE_STARTS_WITH],
            commands: [clazz: CommandsResource],
            commandExitValue: [clazz: CommandExitValueResource, path: "/command/{id}/exitValue"],
            commandStreams: [clazz: CommandStreamsResource, path: "/command/{id}/streams"],
    ].each { name, map ->
      def path = map.path ?: "/${name}".toString()
      Class clazz = map.clazz
      def route = router.attach(path, clazz)
      if(map.matchingMode)
        route.matchingMode = map.matchingMode
      attributes.put(clazz.name, path)
    }
    
    _restServer.getDefaultHost().attach(router);

    def secure = ''

    if(Config.getOptionalBoolean(_config, "${prefix}.agent.sslEnabled", true))
    {
      def serverContext = context.createChildContext()

      Series params = serverContext.getParameters();
      // keystore
      def keystore = fetchFile(Config.getRequiredString(_config, "${prefix}.agent.keystorePath"),
                               Config.getRequiredString(_config, "${prefix}.agent.keystoreChecksum"))
      params.add('keystorePath', keystore.path)
      params.add('keystorePassword', getPassword(_config, "${prefix}.agent.keystorePassword"))
      params.add('keyPassword', getPassword(_config, "${prefix}.agent.keyPassword"))

      // truststore
      def truststore = fetchFile(Config.getRequiredString(_config, "${prefix}.agent.truststorePath"),
                                 Config.getRequiredString(_config, "${prefix}.agent.truststoreChecksum"))
      params.add('truststorePath', truststore.path)
      params.add('truststorePassword', getPassword(_config, "${prefix}.agent.truststorePassword"))

      params.add('sslContextFactory', 'org.restlet.engine.security.DefaultSslContextFactory')

      params.add('needClientAuthentication', 'true')

      params.add('defaultThreads',
                 Config.getOptionalString(_config, "${prefix}.agent.rest.server.defaultThreads", '3'))
      
      def server = _restServer.getServers().add(Protocol.HTTPS, port);
      server.setContext(serverContext)

      secure = '(secure)'
    }
    else
    {
      _restServer.getServers().add(Protocol.HTTP, port);
    }

    _restServer.start()
    log.info "Started REST service on ${secure} port: ${port}"
  }

  def awaitTermination()
  {
    synchronized (_lock) {
      while(!_receivedShutdown)
      {
        _lock.wait()
      }
    }
  }

  protected Shell createShell(ShellImpl rootShell, String root)
  {
    def fs =
      rootShell.fileSystem.newFileSystem(GroovyIOUtils.toFile(Config.getRequiredString(_config,
                                                                                       root)))
    return rootShell.newShell(fs)
  }

  protected ShellImpl createRootShell()
  {
    // registering ivy url handler
    def ivySettings =
      GroovyNetUtils.toURI(Config.getOptionalString(_config, "${prefix}.agent.ivySettings", null))
    if(ivySettings)
    {
      _urlFactory.registerHandler('ivy') {
        return new IvyURLHandler(ivySettings)
      }
    }

    def fileSystem = new FileSystemImpl(new File('/'), _agentTempDir)
    return new ShellImpl(fileSystem: fileSystem,
                         agentProperties: _agentProperties)
  }

  protected CommandManager createCommandsManager()
  {
    if(Config.getOptionalBoolean(_config, "${prefix}.agent.features.commands.enabled", true))
    {
      new CommandManagerImpl(agentContext: _agent,
                             storage: new MemoryCommandExecutionIOStorage(agentContext: _agent))
    }
    else
    {
      log.info "Feature [commands] => [disabled]"
      // disabling all commands methods...
      ObjectProxyBuilder.createProxy(new DisabledFeatureProxy("commands"), CommandManager)
    }
  }

  protected Storage createStorage()
  {
    def fileSystem =
      new FileSystemImpl(GroovyIOUtils.toFile(Config.getRequiredString(_config,
                                                                       "${prefix}.agent.scriptStateDir")),
                         _agentTempDir)
    
    Storage storage = new FileSystemStorage(fileSystem,
                                            _agentProperties,
                                            _persistentPropertiesFile)

    // clean up on boot
    def invalidStates = storage.deleteInvalidStates()
    if(invalidStates)
      log.warn("cleaned up invalid states [${invalidStates.size()}]")

    WriteOnlyStorage zkStorage = createZooKeeperStorage()

    if(zkStorage)
    {
      _dwStorage = new DualWriteStorage(storage, zkStorage)
      storage = _dwStorage
    }

    return storage
  }


  /**
   * Create Sigar.
   */
  protected Sigar createSigar()
  {
    try
    {
      Sigar.load()
      return new Sigar()
    }
    catch (Throwable th)
    {
      log.warn("Cannot load the Sigar library... ignoring", th)
      return null
    }
  }

  protected long getAgentPid()
  {
    if(_sigar)
    {
      try
      {
        return _sigar.pid
      }
      catch(SigarException e)
      {
        log.warn("Could not determine the pid for this agent [ignored]", e)
      }
    }

    return 0
  }

  public void onConnected()
  {
    _zkSync()
  }

  private _zkSync = {
    if(_zkClient)
    {
      synchronized(_zkClient)
      {
        // we recompute the hostname
        try
        {
          _storage.updateAgentProperty("${prefix}.agent.hostname".toString(), hostnameFactory())
        }
        catch(Throwable th)
        {
          log.warn("could not recompute the hostname... ignored", th)
        }

        log.info("Syncing filesystem <=> ZooKeeper")
        _dwStorage.sync()
      }
    }
  }

  protected String computeAgentEphemeralPath()
  {
    return "${_zooKeeperRoot}/agents/fabrics/${_fabric}/instances/${_agentName}"
  }

  public void onDisconnected()
  {
    if(_zkClient)
      log.warn("Detected ZooKeeper failure.")
  }

  protected ZooKeeperStorage createZooKeeperStorage()
  {
    if(_zkClient)
    {
      ZooKeeperStorage storage = new ZooKeeperStorage(_zkClient.chroot(computeZooKeeperStoragePath()),
                                                      _zkClient.chroot(computeAgentEphemeralPath()))
      storage.prefix = prefix
      
      return storage
    }

    return null
  }

  protected String computeZooKeeperStoragePath()
  {
    return "${_zooKeeperRoot}/agents/fabrics/${_fabric}/state/${_agentName}"
  }

  String getZookeeperRoot()
  {
    return _zooKeeperRoot
  }

  String getFabric()
  {
    return _fabric
  }

  String getAgentName()
  {
    return _agentName
  }

  static void main(args)
  {
    AgentMain agentMain = new AgentMain()
    agentMain.init(args)
    agentMain.startAndWait()
  }

  protected def readConfig(url, Properties properties)
  {
    staticReadConfig(url, properties)
  }

  // creating a new method in order not to change the non static one
  static def staticReadConfig(url, Properties properties)
  {
    if(url)
    {
      // using ant to read the properties which will automatically do ${} replacement
      def project = AntUtils.withBuilder { ant ->
        properties.each { k,v ->
          ant.property(name:k, value: v)
        }
        ant.loadproperties() {
          ant.url(url: url)
        }
      }.project

      project.properties.keySet().each { name ->
        properties[name] = project.replaceProperties(project.getProperty(name))
      }
    }
  }

  /**
   * The goal of this method is to merge the configuration in the following fashion: all
   * properties that are in the <code>higherPriority</code> bucket will override any that are
   * in the other bucket
   *
   * @return always a new object with the merged properties
   */
  protected Properties mergeConfig(Properties higherPriorityConfig, Properties lowerPriorityConfig)
  {
    Properties mergedConfig = new Properties()

    // using ant to read the properties which will automatically do ${} replacement
    def project = AntUtils.withBuilder { ant ->
      [higherPriorityConfig, lowerPriorityConfig].each { props ->
        props?.each { k,v ->
          ant.property(name:k, value: v)
        }
      }
      return ant.project
    }

    project.properties.keySet().each { name ->
      mergedConfig[name] = project.replaceProperties(project.getProperty(name))
    }

    return mergedConfig
  }

  protected String getPassword(def config, String name)
  {
    def password = Config.getRequiredString(config, name)

    // to encrypt the password use the password.sh cli (provided with agent-cli)
    if(Config.getOptionalBoolean(config, "${name}Encrypted", true))
    {
      password = CodecUtils.decodeString(twoWayCodec, password)
    }

    return password
  }

  protected File fetchFile(fileLocation, checksum)
  {
    File file = GroovyIOUtils.toFile(fileLocation)
    def computedChecksum = oneWayCodec.encode(file.readBytes())
    if(computedChecksum != checksum)
      throw new IllegalArgumentException("wrong checksum for ${fileLocation}")
    return file
  }
}
