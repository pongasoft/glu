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
  echo "   Usage:  setup-zookeeper.sh -h -f <fabric> [-d zookeeper-config]  "
  echo ""
  echo "     -h : usage help"
  echo "     -f : fabric name (required)"
  echo "     -d : folder containing keys + config (default: $GLU_ZK_CONFIG)"
  echo "     -z : Zookeeper server:port comma-delimited list (default: $GLU_ZK_CONNECT_STRING)"
  echo "     -v : verbose"
  echo ""
  echo "     -c : overrides location of config.properties"
  echo "     -k : overrides location of agent.keystore"
  echo "     -r : zookeeper root (default: $GLU_ZK_ROOT)"
  echo "     -t : overrides location of console.truststore"
  echo "     -Z : overrides zookeeper cli location"
  echo ""
  exit 0
}

###############################################################################
#
# Function Name : zk_upload()
# Description   : Uploads the provided file in zookeeper at the provided path
# Arguments     : $1: the file to upload, $2: where to store it in zookeeper
# Return Result : none
#
###############################################################################
zk_upload()
{
  CMD="$GLU_ZK_CLI -s $GLU_ZK_CONNECT_STRING upload -f $1 $2"
  if [ ! -z "$VERBOSE" ]; then
    echo $CMD
  fi
  $CMD
}

############################### SCRIPT BODY ###################################

# save current directory
CURRDIR=`pwd`

# base directory
BASEDIR=`cd $(dirname $0)/.. ; pwd`

cd $BASEDIR

GLU_ZK_CONFIG=$BASEDIR/zookeeper-config
GLU_ZK_ROOT=/org/glu
GLU_ZK_CONNECT_STRING=localhost:2181
GLU_ZK_CLI=$BASEDIR/org.linkedin.zookeeper-cli-@zookeeper.version@/bin/zk.sh

# get script options
while getopts "hvc:d:f:k:r:t:z:Z:" opt ; do
  case $opt in
    c  )
         GLU_AGENT_CONFIG=$OPTARG
         ;;
    d  )
         GLU_ZK_CONFIG=$OPTARG
         ;;
    f  )
         GLU_FABRIC=$OPTARG
         ;;
    h  ) usage
         exit 0
         ;;
    k  )
         GLU_AGENT_KEYSTORE=$OPTARG
         ;;
    r  )
         GLU_ZK_ROOT=$OPTARG
         ;;
    t  )
         GLU_CONSOLE_TRUSTSTORE=$OPTARG
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

if [ -z "$GLU_CONSOLE_TRUSTSTORE" ]; then
  GLU_CONSOLE_TRUSTSTORE=$GLU_ZK_CONFIG/console.truststore
fi
if [ -z "$GLU_AGENT_KEYSTORE" ]; then
  GLU_AGENT_KEYSTORE=$GLU_ZK_CONFIG/agent.keystore
fi
if [ -z "$GLU_AGENT_CONFIG" ]; then
  GLU_AGENT_CONFIG=$GLU_ZK_CONFIG/config.properties
fi

if [ -z "GLU_FABRIC" ]; then
  echo "You must provide a fabric (-f)"
  exit 1
fi

if [ ! -z "$VERBOSE" ]; then
  echo GLU_AGENT_CONFIG=$GLU_AGENT_CONFIG
  echo GLU_AGENT_FABRIC=$GLU_AGENT_FABRIC
  echo GLU_AGENT_KEYSTORE=$GLU_AGENT_KEYSTORE
  echo GLU_ZK_ROOT=$GLU_ZK_ROOT
  echo GLU_CONSOLE_TRUSTSTORE=$GLU_CONSOLE_TRUSTSTORE
  echo GLU_ZK_CONNECT_STRING=$GLU_ZK_CONNECT_STRING
  echo GLU_ZK_CLI=$GLU_ZK_CLI
fi

GLU_ZK_AGENT_CONFIG_PATH="$GLU_ZK_ROOT/agents/fabrics/$GLU_FABRIC/config"

zk_upload $GLU_AGENT_CONFIG "$GLU_ZK_AGENT_CONFIG_PATH/config.properties"
zk_upload $GLU_AGENT_KEYSTORE "$GLU_ZK_AGENT_CONFIG_PATH/agent.keystore"
zk_upload $GLU_CONSOLE_TRUSTSTORE "$GLU_ZK_AGENT_CONFIG_PATH/console.truststore"
