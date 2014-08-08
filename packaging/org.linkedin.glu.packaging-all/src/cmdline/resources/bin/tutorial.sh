#!/bin/bash

#
# Copyright (c) 2010-2010 LinkedIn, Inc
# Portions Copyright (c) 2011-2013 Yan Pujante
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

# from http://stackoverflow.com/questions/59895/can-a-bash-script-tell-what-directory-its-stored-in
SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do
  DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE"
done
BASEDIR="$( cd -P "$( dirname "$SOURCE" )/.." && pwd )"
cd $BASEDIR

GLU_HOME=$BASEDIR
GLU_VERSION=@glu.version@
ZOOKEEPER_VERSION=@zookeeper.version@
JETTY_DISTRIBUTION=@jetty.distribution@

GLU_TUTORIAL_AGENT_NAME=agent-1
GLU_TUTORIAL_CONSOLE_NAME=tutorialConsole
GLU_TUTORIAL_ZOOKEEPER_CLUSTER_NAME=tutorialZooKeeperCluster
GLU_TUTORIAL_ZOOKEEPER_HOST=127.0.0.1

init()
{
  if [ -z "$GLU_TUTORIAL_DIR" ]; then
    GLU_TUTORIAL_DIR=$GLU_HOME/tutorial
  fi

  GLU_TUTORIAL_DISTS=$GLU_TUTORIAL_DIR/distributions/tutorial
  GLU_TUTORIAL_BIN=$GLU_TUTORIAL_DIR/bin

  GLU_TUTORIAL_AGENT_ROOT=$GLU_TUTORIAL_DISTS/agents/org.linkedin.glu.agent-server-$GLU_TUTORIAL_AGENT_NAME-$GLU_TUTORIAL_ZOOKEEPER_CLUSTER_NAME-$GLU_VERSION
  GLU_TUTORIAL_AGENT_CLI_ROOT=$GLU_TUTORIAL_DISTS/agent-cli/org.linkedin.glu.agent-cli-$GLU_VERSION
  GLU_TUTORIAL_CONSOLE_ROOT=$GLU_TUTORIAL_DISTS/consoles/org.linkedin.glu.console-server-$GLU_TUTORIAL_CONSOLE_NAME-$GLU_VERSION
  GLU_TUTORIAL_CONSOLE_CLI_ROOT=$GLU_TUTORIAL_DISTS/console-cli/org.linkedin.glu.console-cli-$GLU_VERSION
  GLU_TUTORIAL_ZOOKEEPER_ROOT=$GLU_TUTORIAL_DISTS/zookeeper-clusters/zookeeper-cluster-$GLU_TUTORIAL_ZOOKEEPER_CLUSTER_NAME/org.linkedin.zookeeper-server-$ZOOKEEPER_VERSION

  GLU_TUTORIAL_AGENT_LINK=$GLU_TUTORIAL_DIR/agent-server
  GLU_TUTORIAL_AGENT_CLI_LINK=$GLU_TUTORIAL_DIR/agent-cli
  GLU_TUTORIAL_CONSOLE_LINK=$GLU_TUTORIAL_DIR/console-server
  GLU_TUTORIAL_CONSOLE_CLI_LINK=$GLU_TUTORIAL_DIR/console-cli
  GLU_TUTORIAL_ZOOKEEPER_LINK=$GLU_TUTORIAL_DIR/zookeeper-server

}

usage()
{
  echo ""
  echo "   Usage:  tutorial.sh [-d <tutorial_dir>] start|stop|status|tail"
  echo ""
}

setup()
{
  if [ ! -d $GLU_TUTORIAL_DIR ]; then
    echo "### Setting up tutorial..."
    $GLU_HOME/bin/setup.sh -D --keys-root $GLU_HOME/models/tutorial/keys -o $GLU_TUTORIAL_DISTS $GLU_HOME/models/tutorial/glu-meta-model.json.groovy

    # getting rid of install scripts
    rm -rf $GLU_TUTORIAL_BIN

    echo "### Creating shortcuts..."
    mkdir $GLU_TUTORIAL_BIN
    ln -s $GLU_TUTORIAL_AGENT_ROOT $GLU_TUTORIAL_AGENT_LINK
    ln -s $GLU_TUTORIAL_AGENT_LINK/bin/agentctl.sh $GLU_TUTORIAL_BIN/agent-server.sh

    ln -s $GLU_TUTORIAL_AGENT_CLI_ROOT $GLU_TUTORIAL_AGENT_CLI_LINK
    ln -s $GLU_TUTORIAL_AGENT_CLI_LINK/bin/agent-cli.sh $GLU_TUTORIAL_BIN/agent-cli.sh

    ln -s $GLU_TUTORIAL_CONSOLE_ROOT $GLU_TUTORIAL_CONSOLE_LINK
    ln -s $GLU_TUTORIAL_CONSOLE_LINK/bin/consolectl.sh $GLU_TUTORIAL_BIN/console-server.sh

    ln -s $GLU_TUTORIAL_CONSOLE_CLI_ROOT $GLU_TUTORIAL_CONSOLE_CLI_LINK
    ln -s $GLU_TUTORIAL_CONSOLE_CLI_LINK/bin/console-cli.py $GLU_TUTORIAL_BIN/console-cli.sh

    ln -s $GLU_TUTORIAL_ZOOKEEPER_ROOT $GLU_TUTORIAL_ZOOKEEPER_LINK
    ln -s $GLU_TUTORIAL_ZOOKEEPER_LINK/bin/zookeeperctl.sh $GLU_TUTORIAL_BIN/zookeeper-server.sh

    echo "### Configuring ZooKeeper..."
    $GLU_TUTORIAL_BIN/zookeeper-server.sh start
    $GLU_HOME/bin/setup.sh -Z -o $GLU_TUTORIAL_DISTS $GLU_HOME/models/tutorial/glu-meta-model.json.groovy
    $GLU_TUTORIAL_BIN/zookeeper-server.sh stop

    echo "### Initializing console..."
    touch $GLU_TUTORIAL_CONSOLE_LINK/$JETTY_DISTRIBUTION/logs/console.log
    echo "### Setup complete."
  else
    echo "### $GLU_TUTORIAL_DIR already exists... skipping"
  fi
}

start()
{
  if [ ! -d $GLU_TUTORIAL_DIR ]; then
    setup
    # make sure that ZooKeeper is actually stopped
    sleep 2
  fi
 echo "### Starting ZooKeeper..."
 JVMFLAGS="-Dorg.linkedin.app.name=org.linkedin.zookeeper-server" $GLU_TUTORIAL_BIN/zookeeper-server.sh start
 echo "### Starting Agent..."
 $GLU_TUTORIAL_BIN/agent-server.sh start
 echo "### Starting Console..."
 $GLU_TUTORIAL_BIN/console-server.sh start
 echo "### Done."
}

stop()
{
 echo "### Stopping Console..."
 $GLU_TUTORIAL_BIN/console-server.sh stop
 echo "### Stopping Agent..."
 $GLU_TUTORIAL_BIN/agent-server.sh stop
 echo "### Stopping ZooKeeper..."
 $GLU_TUTORIAL_BIN/zookeeper-server.sh stop
 echo "### Done."
}

status()
{
 echo "### ZooKeeper Status"
 $GLU_TUTORIAL_BIN/zookeeper-server.sh status
 echo "### Agent Status"
 $GLU_TUTORIAL_BIN/agent-server.sh status
 echo "### Console Status"
 $GLU_TUTORIAL_BIN/console-server.sh check
}

tail()
{
 exec tail -f $GLU_TUTORIAL_CONSOLE_LINK/$JETTY_DISTRIBUTION/logs/console.log \
              $GLU_TUTORIAL_AGENT_LINK/data/logs/org.linkedin.glu.agent-server.out \
              $GLU_TUTORIAL_ZOOKEEPER_LINK/logs/zookeeper.log
}

# get script options
while getopts "d:" opt ; do
  case $opt in
    d  )
         GLU_TUTORIAL_DIR=$OPTARG
         ;;
    \? ) usage
         exit 1
         ;;
  esac
done

# initializes variables
init

# Check to make sure we have the correct version of Java.
if [ -z "$JAVA_CMD" ]; then
  if [ -f $JAVA_HOME/bin/java ]; then
    JAVA_CMD=$JAVA_HOME/bin/java
  else
    JAVA_CMD=java
  fi
fi

JAVA_VER=$("$JAVA_CMD" -version 2>&1 | grep 'java version' | sed 's/java version "\(.*\)\.\(.*\)\..*"/\1\2/; 1q')
if [ "$JAVA_VER" -lt 17 ]; then
	echo "### ERROR START ###########"
	echo "### Java @ $JAVA_CMD too old (required java 1.7)"
	$JAVA_CMD -version
	echo "### ERROR END   ###########"
  exit 1;
fi

# correct the index so the command argument is always $1
shift $(($OPTIND - 1))

case $1 in
  'setup' ) setup
            ;;
  'start' ) start
            ;;
  'stop'  ) stop
            ;;
  'status') status
            ;;
  'tail') tail
            ;;
         *) usage
            exit 1
            ;;
esac
