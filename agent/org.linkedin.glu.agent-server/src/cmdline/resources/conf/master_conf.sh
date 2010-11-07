#
# Copyright 2010-2010 LinkedIn, Inc
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License. You may obtain a copy of
# the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations under
# the License.
#

# handle GLU_ZOOKEEPER
if [ -z "$GLU_ZOOKEEPER" ]; then
  D_GLU_ZOOKEEPER=""
else
  D_GLU_ZOOKEEPER="-Dglu.agent.zkConnectString=$GLU_ZOOKEEPER"
fi

# handle GLU_AGENT_NAME
if [ -z "$GLU_AGENT_NAME" ]; then
  D_GLU_AGENT_NAME=""
else
  D_GLU_AGENT_NAME="-Dglu.agent.name=$GLU_AGENT_NAME"
fi

# handle GLU_AGENT_FABRIC
if [ -z "$GLU_AGENT_FABRIC" ]; then
  D_GLU_AGENT_FABRIC=""
else
  D_GLU_AGENT_FABRIC="-Dglu.agent.fabric=$GLU_AGENT_FABRIC"
fi

if [ -z "$GLU_AGENT_APPS" ]; then
  GLU_AGENT_APPS=$GLU_AGENT_HOME/../apps
fi

# make sure that the apps folder exist
mkdir -p $GLU_AGENT_APPS
GLU_AGENT_APPS=`cd $GLU_AGENT_APPS; pwd`

if [ -z "$GLU_AGENT_ZOOKEEPER_ROOT" ]; then
  GLU_AGENT_ZOOKEEPER_ROOT=/org/glu
fi

# Application name (for the container/cmdline app) - build-time expansion
APP_NAME="glu-agent"

# Application version (for the container/cmdline app) - build-time expansion
APP_VERSION="@agent.version@"

# Java Classpath (leave empty - set by the ctl script)
JVM_CLASSPATH=

# Min, max, total JVM size (-Xms -Xmx)
JVM_SIZE=-Xmx256m

# New Generation Sizes (-XX:NewSize -XX:MaxNewSize)
JVM_SIZE_NEW=

# Perm Generation Sizes (-XX:PermSize -XX:MaxPermSize)
JVM_SIZE_PERM=

# Type of Garbage Collector to use
JVM_GC_TYPE=

# Tuning options for the above garbage collector
JVM_GC_OPTS=

# JVM GC activity logging settings ($LOG_DIR set in the ctl script)
JVM_GC_LOG="-XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintTenuringDistribution -Xloggc:$GC_LOG"

# Log4J configuration
JVM_LOG4J="-Dlog4j.configuration=file:$CONF_DIR/log4j.xml"

# Java I/O tmp dir
JVM_TMP_DIR="-Djava.io.tmpdir=$TMP_DIR"

# Any additional JVM arguments
JVM_XTRA_ARGS="-Dsun.security.pkcs11.enable-solaris=false $D_GLU_ZOOKEEPER $D_GLU_AGENT_NAME $D_GLU_AGENT_FABRIC -Dglu.agent.homeDir=$GLU_AGENT_HOME -Dglu.agent.apps=$GLU_AGENT_APPS -Dglu.agent.zookeeper.root=$GLU_AGENT_ZOOKEEPER_ROOT"
#JVM_XTRA_ARGS="-Djavax.net.debug=all"

# Debug arguments to pass to the JVM (when starting with '-d' flag)
JVM_DEBUG="-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,address=8887,server=y,suspend=n"

# Application Info - name, version, instance, etc.
JVM_APP_INFO="-Dorg.linkedin.app.name=$APP_NAME -Dorg.linkedin.app.version=$APP_VERSION"

# Main Java Class to run
MAIN_CLASS=org.linkedin.glu.agent.server.AgentMain

# Main Java Class arguments
MAIN_CLASS_ARGS="file:$CONF_DIR/agentConfig.properties"

# set JAVA_HOME (JDK6) & JAVA_CMD
if [ -z "$JAVA_HOME" ]; then
  echo "Please set JAVA_HOME manually."
  exit 1
fi

if [ -z "$JAVA_CMD" ]; then
  JAVA_CMD=$JAVA_HOME/bin/java
fi


