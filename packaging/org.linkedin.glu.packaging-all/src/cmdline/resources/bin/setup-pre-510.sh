#!/bin/bash

#
# Copyright (c) 2013 Yan Pujante
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

# The purpose of this script is to regenerate the distribution the same way it was prior
# to 5.1.0 for easier migration

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

init()
{
  if [ -z "$GLU_PRE_510_DIR" ]; then
    GLU_PRE_510_DIR=$GLU_HOME/pre-510
  fi


  GLU_PRE_510_DISTS=$GLU_PRE_510_DIR/dists
  GLU_PRE_510_BIN=$GLU_PRE_510_DIR/bin

  GLU_PRE_510_AGENT_ROOT=$GLU_PRE_510_DISTS/agents/org.linkedin.glu.agent-server-$GLU_VERSION
  GLU_PRE_510_AGENT_UPGRADE_ROOT=$GLU_PRE_510_DISTS/agents/org.linkedin.glu.agent-server-upgrade-$GLU_VERSION
  GLU_PRE_510_AGENT_CLI_ROOT=$GLU_PRE_510_DISTS/agent-cli/org.linkedin.glu.agent-cli-$GLU_VERSION
  GLU_PRE_510_CONSOLE_ROOT=$GLU_PRE_510_DISTS/consoles/org.linkedin.glu.console-server-$GLU_VERSION
  GLU_PRE_510_CONSOLE_CLI_ROOT=$GLU_PRE_510_DISTS/console-cli/org.linkedin.glu.console-cli-$GLU_VERSION
  GLU_PRE_510_ZOOKEEPER_ROOT=$GLU_PRE_510_DISTS/zookeeper-clusters/zookeeper-cluster/org.linkedin.zookeeper-server-$ZOOKEEPER_VERSION

  if [ -z "$GLU_AGENT_UPGRADE_COMPRESS_COMMAND" ]; then
    GLU_AGENT_UPGRADE_COMPRESS_COMMAND="tar -zcf $GLU_PRE_510_DIR/org.linkedin.glu.agent-server-upgrade-$GLU_VERSION.tgz *"
  fi

  if [ -z "$GLU_CONSOLE_WAR_COMMAND" ]; then
    GLU_CONSOLE_WAR_COMMAND="jar cfm $GLU_PRE_510_DIR/org.linkedin.glu.console-$GLU_VERSION.war META-INF/MANIFEST.MF * "
  fi

}

usage()
{
  echo ""
  echo "   Usage:  setup-pre-510.sh [-d <output_dir>]"
  echo ""
}

setup()
{
  if [ ! -d $GLU_PRE_510_DIR ]; then
    echo "### Setting up pre 5.1.0 distributions..."
    $GLU_HOME/bin/setup.sh -D --keys-root $GLU_HOME/models/tutorial/keys -o $GLU_PRE_510_DISTS $GLU_HOME/models/pre-5.1.0/glu-meta-model.json.groovy

    # getting rid of install scripts
    rm -rf $GLU_PRE_510_BIN

    echo "### Creating agent upgrade..."
    cd $GLU_PRE_510_AGENT_UPGRADE_ROOT
    $GLU_AGENT_UPGRADE_COMPRESS_COMMAND
    cd $BASEDIR

    echo "### Creating console war..."
    cd $GLU_PRE_510_CONSOLE_ROOT/glu/repository/exploded-wars/org.linkedin.glu.console-webapp-$GLU_VERSION
    $GLU_CONSOLE_WAR_COMMAND
    cd $BASEDIR

    echo "### Moving files around..."
    mv $GLU_PRE_510_AGENT_ROOT $GLU_PRE_510_DIR/agent-server
    mv $GLU_PRE_510_AGENT_CLI_ROOT $GLU_PRE_510_DIR/agent-cli
    mv $GLU_PRE_510_CONSOLE_ROOT $GLU_PRE_510_DIR/console-server
    mv $GLU_PRE_510_CONSOLE_CLI_ROOT $GLU_PRE_510_DIR/console-cli
    mv $GLU_PRE_510_ZOOKEEPER_ROOT $GLU_PRE_510_DIR


    echo "### Creating shortcuts..."
    mkdir $GLU_PRE_510_BIN
    ln -s ../agent-server/bin/agentctl.sh $GLU_PRE_510_BIN/agent-server.sh
    ln -s ../agent-cli/bin/agent-cli.sh $GLU_PRE_510_BIN/agent-cli.sh
    ln -s ../console-server/bin/consolectl.sh $GLU_PRE_510_BIN/console-server.sh
    ln -s ../console-cli/bin/console-cli.py $GLU_PRE_510_BIN/console-cli.sh
    ln -s ../org.linkedin.zookeeper-server-$ZOOKEEPER_VERSION/bin/zookeeperctl.sh $GLU_PRE_510_BIN/zookeeper-server.sh

    echo "### Cleanup..."
    rm -rf $GLU_PRE_510_DISTS

    echo "### Setup complete."
  else
    echo "### $GLU_PRE_510_DIR already exists... skipping"
  fi
}

# get script options
while getopts "d:" opt ; do
  case $opt in
    d  )
         GLU_PRE_510_DIR=$OPTARG
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

JAVA_VER=$("$JAVA_CMD" -version 2>&1 | sed 's/java version "\(.*\)\.\(.*\)\..*"/\1\2/; 1q')
if [ "$JAVA_VER" -lt 17 ]; then
	echo "### ERROR START ###########"
	echo "### Java @ $JAVA_CMD too old (required java 1.7)"
	$JAVA_CMD -version
	echo "### ERROR END   ###########"
  exit 1;
fi

# correct the index so the command argument is always $1
shift $(($OPTIND - 1))

setup