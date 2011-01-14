#!/bin/bash

#
# Copyright (c) 2010-2010 LinkedIn, Inc
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

VERSION_FILE=$BASEDIR/version.txt
AGENTCTL=$BASEDIR/bin/agentctl.sh
PID_FILE=$BASEDIR/data/logs/glu-agent.pid

# versions
CURRENT_VERSION=`cat $VERSION_FILE`
NEW_VERSION=$1

if [ "$CURRENT_VERSION" = "$NEW_VERSION" ] ; then
  echo "Same versions... no upgrade"
  exit 0
fi

###############################################################################
#
# Function Name : get_pid()
# Arguments     : N/A
# Return Result : -1 - PID file not found (not running),
#                  0 - not running
#                 >0 - PID
#
###############################################################################
function get_pid()
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

echo "Stopping agent version $CURRENT_VERSION"

# stop the current version
$AGENTCTL stop

# adding some delay to make sure that it has enough time to stop
echo "Waiting 1s for agent to stop"
sleep 1

# wait for the old version to be stopped
timeout=5
while [ $(get_pid) -gt 0 ] && [ $timeout -gt 0 ] ; do
  sleep 1
  timeout=$(($timeout - 1))
  echo "Agent still not down $timeout..."
done

# if still not stopped, then we bail out
if [ $(get_pid) -gt 0 ] ; then
  echo "Current agent is still running... aborting!"
  exit 1
fi

echo "Installing version $NEW_VERSION"

# update the version file with the new version
echo $NEW_VERSION > $VERSION_FILE

echo "Starting agent version $NEW_VERSION"

# start the new version
$AGENTCTL start

# adding some delay to make sure that it has enough time to start
echo "Waiting 30s for agent to start"
sleep 30

# wait for the new version to be started
timeout=5
while [ $(get_pid) -le 0 ] && [ $timeout -gt 0 ] ; do
  sleep 1
  timeout=$(($timeout - 1))
  echo "Agent still not up $timeout..."
done

# check if we really started
if [ $(get_pid) -le 0 ] ; then
  echo "Could not start the new agent... rolling back to previous one"
  echo $CURRENT_VERSION > $VERSION_FILE
  $AGENTCTL start
else
  echo "Started agent version $NEW_VERSION successfully"
fi
