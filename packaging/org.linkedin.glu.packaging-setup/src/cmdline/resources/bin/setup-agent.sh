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

###############################################################################
#
# Function Name : usage()
# Arguments     : N/A
# Return Result : N/A, exit 0
#
###############################################################################
usage()
{
  echo ""
  echo "   Usage:  setup-agent.sh -h -f <fabric> [-d zookeeper-config]  "
  echo ""
  echo "     -h : usage help"
  echo "     -f : fabric name (required)"
  echo "     -d : glu agent directory"
  echo "     -z : Zookeeper server:port comma-delimited list (default: $GLU_ZK_CONNECT_STRING)"
  echo "     -v : verbose"
  echo ""
  echo "     -n : overrides agent name (default: $GLU_AGENT_NAME)"
  echo "     -p : overrides agent port (default: $GLU_AGENT_PORT)"
  echo "     -r : zookeeper root (default: $GLU_ZK_ROOT)"
  echo "     -t : overrides location of console.truststore"
  echo "     -Z : overrides zookeeper cli location"
  echo ""
  exit 0
}

############################### SCRIPT BODY ###################################

# save current directory
CURRDIR=`pwd`

# base directory
BASEDIR=`cd $(dirname $0)/.. ; pwd`

cd $BASEDIR

GLU_AGENT_NAME=`hostname`
GLU_AGENT_PORT=12906
GLU_ZK_ROOT=/org/glu
GLU_ZK_CONNECT_STRING=localhost:2181
GLU_ZK_CLI=$BASEDIR/org.linkedin.zookeeper-cli-@zookeeper.version@/bin/zk.sh

# get script options
while getopts "hvd:f:n:p:r:z:Z:" opt ; do
  case $opt in
    d  )
         GLU_AGENT_DIR=$OPTARG
         ;;
    f  )
         GLU_AGENT_FABRIC=$OPTARG
         ;;
    h  ) usage
         exit 0
         ;;
    n  )
         GLU_AGENT_NAME=$OPTARG
         ;;
    p  )
         GLU_AGENT_PORT=$OPTARG
         ;;
    r  )
         GLU_ZK_ROOT=$OPTARG
         ;;
    z  )
         GLU_ZK_CONNECT_STRING=$OPTARG
         ;;
    Z  )
         GLU_ZK_CLI=$OPTARG
         ;;
    v  )
         VERBOSE=true
         ;;
    \? ) usage
         exit 1
         ;;
  esac
done

if [ -z "GLU_FABRIC" ]; then
  echo "You must provide a fabric (-f)"
  exit 1
fi

if [ ! -z "$VERBOSE" ]; then
  echo GLU_AGENT_DIR=$GLU_AGENT_DIR
  echo GLU_AGENT_FABRIC=$GLU_AGENT_FABRIC
  echo GLU_AGENT_NAME=$GLU_AGENT_NAME
  echo GLU_AGENT_PORT=$GLU_AGENT_PORT
  echo GLU_ZK_ROOT=$GLU_ZK_ROOT
  echo GLU_ZK_CONNECT_STRING=$GLU_ZK_CONNECT_STRING
  echo GLU_ZK_CLI=$GLU_ZK_CLI
fi

CMD="$GLU_ZK_CLI put -f $GLU_AGENT_FABRIC $GLU_ZK_ROOT/agents/names/$GLU_AGENT_NAME/fabric"
if [ ! -z "$VERBOSE" ]; then
  echo $CMD
fi
$CMD

if [ ! -z "$GLU_AGENT_DIR" ]; then
  CONF_DIR=$GLU_AGENT_DIR/`cat $GLU_AGENT_DIR/version.txt`/conf
  echo "# written by setup on `date`
GLU_AGENT_FABRIC=$GLU_AGENT_FABRIC
GLU_AGENT_NAME=$GLU_AGENT_NAME
GLU_AGENT_PORT=$GLU_AGENT_PORT
GLU_ZOOKEEPER=$GLU_ZK_CONNECT_STRING" > $CONF_DIR/pre_master_conf.sh
fi
