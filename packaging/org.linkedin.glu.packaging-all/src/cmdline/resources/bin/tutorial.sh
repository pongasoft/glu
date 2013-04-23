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

BASEDIR=`cd $(dirname $0)/.. ; pwd`
cd $BASEDIR

GLU_FABRIC=glu-dev-1
GLU_AGENT_NAME=agent-1

usage()
{
  echo ""
  echo "   Usage:  tutorial.sh start|stop|status|tail"
  echo ""
}

setup()
{
 echo "### Starting ZooKeeper..."
 $BASEDIR/bin/zookeeper-server.sh start
 echo "### Setting up keys and agent configuration..."
 $BASEDIR/bin/setup-zookeeper.sh -f $GLU_FABRIC
 echo "### Setting $GLU_AGENT_NAME in $GLU_FABRIC..."
 $BASEDIR/bin/setup-agent.sh -f $GLU_FABRIC -n $GLU_AGENT_NAME -d $BASEDIR/agent-server
 echo "### Stopping ZooKeeper..."
 $BASEDIR/bin/zookeeper-server.sh stop
 echo "### Initializing Console..."
 $BASEDIR/bin/console-server.sh check
 echo "### Done."
}

start()
{
 echo "### Starting ZooKeeper..."
 JVMFLAGS="-Dorg.linkedin.app.name=org.linkedin.zookeeper-server" $BASEDIR/bin/zookeeper-server.sh start
 echo "### Starting Agent..."
 $BASEDIR/bin/agent-server.sh start
 echo "### Starting Console..."
 $BASEDIR/bin/console-server.sh start
 echo "### Done."
}

stop()
{
 echo "### Stopping Console..."
 $BASEDIR/bin/console-server.sh stop
 echo "### Stopping Agent..."
 $BASEDIR/bin/agent-server.sh stop
 echo "### Stopping ZooKeeper..."
 $BASEDIR/bin/zookeeper-server.sh stop
 echo "### Done."
}

status()
{
 echo "### ZooKeeper Status"
 $BASEDIR/bin/zookeeper-server.sh status
 echo "### Agent Status"
 $BASEDIR/bin/agent-server.sh status
 echo "### Console Status"
 $BASEDIR/bin/console-server.sh check
}

tail()
{
 exec tail -f $BASEDIR/console-server/@jetty.distribution@/logs/console.log \
              $BASEDIR/agent-server/data/logs/org.linkedin.glu.agent-server.out \
              $BASEDIR/org.linkedin.zookeeper-server-@zookeeper.version@/logs/zookeeper.log
}

# get script options
while getopts "n:f:" opt ; do
  case $opt in
    n  )
         GLU_AGENT_NAME=$OPTARG
         ;;
    f  )
         GLU_FABRIC=$OPTARG
         ;;
    \? ) usage
         exit 1
         ;;
  esac
done

# Check to make sure we have the correct version of Java.
JAVA_VER=$("$JAVA_HOME/bin/java" -version 2>&1 | sed 's/java version "\(.*\)\.\(.*\)\..*"/\1\2/; 1q')
if [ "$JAVA_VER" -ge 16 ]; then
	echo "### Suitable JVM found under $JAVA_HOME"
	$JAVA_HOME/bin/java -version
else
	echo "### Java @ $JAVA_HOME too old." 
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
