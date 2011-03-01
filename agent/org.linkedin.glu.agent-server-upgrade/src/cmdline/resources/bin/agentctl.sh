#!/bin/bash

#
# Copyright (c) 2010-2010 LinkedIn, Inc
# Portions Copyright (c) 2011 Yan Pujante
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
  echo "   Usage:  agentctl.sh [-hdkr] [-z server:port] [-f fabric] [-n agentName] [-c :ip|:canonical|<hostname>] [-t tag1;tag2;...] start|stop|status|pid"
  echo ""    
  echo "     -h : usage help"
  echo "     -d : debugging options on"
  echo "     -f : the fabric to assign this agent to"
  echo "     -k : force kill the process (kill -9)"
  echo "     -n : the agent name (default to canonical host name)"
  echo "     -t : the agent tags"
  echo "     -c : how to compute the hostname either ':ip' or ':canonical'. Any other value is treated as the hostname to use (default to ':ip')."
  echo "     -r : run in foreground (Ctl-C to stop)"
  echo "     -z : Zookeeper server:port comma-delimited list"
  echo ""
  exit 0
}

###############################################################################
#
# Function Name : start()
# Arguments     : N/A
# Return Result : N/A
#
###############################################################################
start()
{
  # check if process is alreading running
  if [ $(pid_status) -eq 0 ]
  then
    status
    exit 0
  fi
  
  # rotate existing gc.log
  if [ -f $GC_LOG ]; then
    mv $GC_LOG ${GC_LOG}.`date "+%Y-%m-%d.%H%M%S"`
  fi

  # Add all JARs from the lib directory to the classpath
  for file in $LIB_DIR/*.jar  
  do
    if [ -z "$JVM_CLASSPATH" ]; then
      JVM_CLASSPATH="-classpath $file"
    else
      JVM_CLASSPATH=$JVM_CLASSPATH:$file
    fi
  done

  if [ -z "$JVM_CLASSPATH" ]; then
    JVM_CLASSPATH="-classpath $JAVA_HOME/lib/tools.jar"
  else
    JVM_CLASSPATH=$JVM_CLASSPATH:$JAVA_HOME/lib/tools.jar
  fi

  # Java Tuning Options (heap & generations sizes; GC tuning)
  JVM_TUNING_OPTIONS="$JVM_SIZE $JVM_SIZE_NEW $JVM_SIZE_PERM $JVM_GC_TYPE $JVM_GC_OPTS $JVM_GC_LOG"
  
  # Java options - all VM options (excluding main class & arguments passed to it)
  JAVA_OPTIONS="$JVM_APP_INFO $JVM_TUNING_OPTIONS $JVM_LOG4J $JVM_TMP_DIR $JVM_XTRA_ARGS $JVM_CLASSPATH"
  
  # Add debug options, if enabled
  if [ -n "$OPT_DEBUG" ] && [ "$OPT_DEBUG" = "true" ]
  then
    JAVA_OPTIONS="$JAVA_OPTIONS $JVM_DEBUG"
  fi
  
  # Start in foreground (Ctl-C to stop) or background depending on OPT_RUN
  if [ -n "$OPT_RUN" ] && [ "$OPT_RUN" = "true" ]
  then
    # Echo start options
    echo "****************************************************"
    echo "Starting $APP_NAME in foreground (Ctl-C to quit):   "
    echo "$JAVA_CMD $JAVA_OPTIONS $MAIN_CLASS $MAIN_CLASS_ARGS"
    echo "****************************************************"
    echo ""
    
    # Run in foreground
    exec $JAVA_CMD $JAVA_OPTIONS $MAIN_CLASS $MAIN_CLASS_ARGS
  else
    # Echo start options
    echo `date "+%Y/%m/%d %H:%M:%S"`"     INFO [glu-agent] Start:" >> $LOG_DIR/$APP_NAME.out
    echo "$JAVA_CMD $JAVA_OPTIONS $MAIN_CLASS $MAIN_CLASS_ARGS" >> $LOG_DIR/$APP_NAME.out

    # Start in background
    $JAVA_CMD $JAVA_OPTIONS $MAIN_CLASS $MAIN_CLASS_ARGS >> $LOG_DIR/$APP_NAME.out 2>&1 &
    
    # Save PID
    echo $! > $PID_FILE
    
    echo "Started $APP_NAME - PID [`cat $PID_FILE`]"
  fi
  
} 

###############################################################################
#
# Function Name : stop()
# Arguments     : N/A
# Return Result : N/A
#
###############################################################################
stop()
{
  # check that process exists
  if [ $(pid_status) -ne 0 ]
  then
    status
    exit 0
  fi

  # Send a kill (with an optional -9 signal) to the process
  kill $OPT_FORCEKILL `cat $PID_FILE`

  echo "Stopping $APP_NAME - PID [`cat $PID_FILE`]"
}

###############################################################################
#
# Function Name : status()
# Arguments     : N/A
# Return Result : N/A
#
###############################################################################
status()
{
  case $(pid_status) in
    -1) echo "Status: PID file [$PID_FILE] not found !!!"
         ;;
     0) echo "Status: $APP_NAME running - PID [`cat $PID_FILE`]"
         ;;
     1) echo "Status: $APP_NAME not running - PID [`cat $PID_FILE`] not found."
         ;;
  esac
}

###############################################################################
#
# Function Name : pid_status()
# Arguments     : N/A
# Return Result : -1 - PID file not found (not running),
#                  0 - PID found (running), 
#                  1 - PID not found (not running)
#
###############################################################################
pid_status()
{
  case $(get_pid) in
   -1   ) echo -1
          ;;
    0   ) echo 1
          ;;
    *   ) echo 0
          ;;
  esac
  
  return 0
}

###############################################################################
#
# Function Name : get_pid()
# Arguments     : N/A
# Return Result : -1 - PID file not found (not running),
#                  0 - not running
#                 >0 - PID
#
###############################################################################
get_pid()
{
  if [ ! -f $PID_FILE ]
  then
    echo -1
  else
    PID=`cat $PID_FILE`
    PS_STAT=`ps -p $PID -o'user,pid=' | tail -1 | awk '{print $2}'`

    case "$PS_STAT" in
      $PID  ) echo $PID
              ;;
      *     ) echo 0
              ;;
    esac
  fi
  
  return 0
}

############################### SCRIPT BODY ###################################

# save current directory
CURRDIR=`pwd`  

# GLU Agent base directory
BASEDIR=`cd $(dirname $0)/.. ; pwd`

cd $BASEDIR

GLU_AGENT_HOME=`cd $BASEDIR/.. ; pwd`

DATA_DIR=$GLU_AGENT_HOME/data

if [ ! -d $DATA_DIR ]; then
  mkdir $DATA_DIR
fi

# Directories referenced in the script
CONF_DIR=$BASEDIR/conf
LIB_DIR=lib

LOG_DIR=$DATA_DIR/logs

if [ ! -d $LOG_DIR ]; then
  mkdir $LOG_DIR
fi

TMP_DIR=$DATA_DIR/tmp

if [ ! -d $TMP_DIR ]; then
  mkdir $TMP_DIR
fi

# Files referenced in the script
GC_LOG=$LOG_DIR/gc.log

# get script options
while getopts "dhkprz:n:t:f:c:" opt ; do
  case $opt in
    d  ) OPT_DEBUG=true
         ;;
    h  ) usage
         exit 0 
         ;;
    k  ) OPT_FORCEKILL="-9"
         ;;
    r  ) OPT_RUN=true
         ;;
    z  ) 
         GLU_ZOOKEEPER=$OPTARG
         ;;
    n  )
         GLU_AGENT_NAME=$OPTARG
         ;;
    t  )
         GLU_AGENT_TAGS=$OPTARG
         ;;
    c  )
         GLU_AGENT_HOSTNAME_FACTORY=$OPTARG
         ;;
    f  )
         GLU_AGENT_FABRIC=$OPTARG
         ;;
    \? ) usage
         exit 1
         ;;
  esac
done

# hook for custom configuration
if [ -z "$GLU_USER_CONFIG_DIR" ]; then
  GLU_USER_CONFIG_DIR=$CONF_DIR
fi

# Load Configuration Files - $GLU_USER_CONFIG_DIR/pre_master_conf.sh first (if exists)
if [ -f $GLU_USER_CONFIG_DIR/pre_master_conf.sh ]; then
  echo "Loading config [$GLU_USER_CONFIG_DIR/pre_master_conf.sh]..."
  source $GLU_USER_CONFIG_DIR/pre_master_conf.sh
fi

# Load Configuration Files - master_conf.sh (comes bundled)
if [ -f $CONF_DIR/master_conf.sh ]; then
  echo "Loading config [$CONF_DIR/master_conf.sh]..."
  source $CONF_DIR/master_conf.sh
fi

# Load Configuration Files - $GLU_USER_CONFIG_DIR/post_master_conf.sh last (if exists)
if [ -f $GLU_USER_CONFIG_DIR/post_master_conf.sh ]; then
  echo "Loading config [$GLU_USER_CONFIG_DIR/post_master_conf.sh]..."
  source $GLU_USER_CONFIG_DIR/post_master_conf.sh
fi

PID_FILE=$LOG_DIR/$APP_NAME.pid

# correct the index so the command argument is always $1
shift $(($OPTIND - 1)) 

# call appropriate function according to the passed command
case $1 in
  'start' ) start
            ;;
  'stop'  ) stop
            ;;
  'status') status
            ;;
  'pid') get_pid
            ;;
         *) usage
            exit 1
            ;;
esac