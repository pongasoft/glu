#!/bin/bash

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
 cd $BASEDIR/org.linkedin.zookeeper-server-@zookeeper.version@; ./bin/zkServer.sh start
 echo "### Setting up keys and agent configuration..."
 cd $BASEDIR/org.linkedin.glu.packaging-setup-@glu.version@; ./bin/setup-zookeeper.sh -f $GLU_FABRIC
  echo "### Setting $GLU_AGENT_NAME in $GLU_FABRIC..."
 cd $BASEDIR/org.linkedin.glu.packaging-setup-@glu.version@; ./bin/setup-agent.sh -f $GLU_FABRIC -n $GLU_AGENT_NAME -d $BASEDIR/org.linkedin.glu.agent-server-@glu.version@
 echo "### Stopping ZooKeeper..."
 cd $BASEDIR/org.linkedin.zookeeper-server-@zookeeper.version@; ./bin/zkServer.sh stop
 echo "### Done."
}

start()
{
 echo "### Starting ZooKeeper..."
 cd $BASEDIR/org.linkedin.zookeeper-server-@zookeeper.version@; JVMFLAGS="-Dorg.linkedin.app.name=org.linkedin.zookeeper-server" ./bin/zkServer.sh start
 echo "### Starting Agent..."
 cd $BASEDIR/org.linkedin.glu.agent-server-@glu.version@; ./bin/agentctl.sh start
 echo "### Starting Console..."
 cd $BASEDIR/org.linkedin.glu.console-server-@glu.version@; ./bin/consolectl.sh start
 echo "### Done."
}

stop()
{
 echo "### Stopping Console..."
 cd $BASEDIR/org.linkedin.glu.console-server-@glu.version@; ./bin/consolectl.sh stop
 echo "### Stopping Agent..."
 cd $BASEDIR/org.linkedin.glu.agent-server-@glu.version@; ./bin/agentctl.sh stop
 echo "### Stopping ZooKeeper..."
 cd $BASEDIR/org.linkedin.zookeeper-server-@zookeeper.version@; ./bin/zkServer.sh stop
 echo "### Done."
}

status()
{
 echo "### ZooKeeper Status"
 cd $BASEDIR/org.linkedin.zookeeper-server-@zookeeper.version@; ./bin/zkServer.sh status
 echo "### Agent Status"
 cd $BASEDIR/org.linkedin.glu.agent-server-@glu.version@; ./bin/agentctl.sh status
 echo "### Console Status"
 cd $BASEDIR/org.linkedin.glu.console-server-@glu.version@; ./bin/consolectl.sh check
}

tail()
{
 exec tail -f $BASEDIR/org.linkedin.glu.console-server-@glu.version@/@jetty.distribution@/logs/console.log \
              $BASEDIR/org.linkedin.glu.agent-server-@glu.version@/data/logs/org.linkedin.glu.agent-server.out \
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
