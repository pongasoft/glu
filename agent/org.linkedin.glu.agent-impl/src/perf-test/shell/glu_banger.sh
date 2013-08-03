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

COUNTER=0

#PRETEND="-n"
PRETEND=""

if [ -z "$CONSOLE_CLI_CMD" ]; then
  CONSOLE_CLI_CMD="./tutorial/bin/console-cli.sh"
fi

while [[ 1 ]]; do
  let COUNTER=COUNTER+1 
  echo "=========> " `date` "[$COUNTER] <========="
  $CONSOLE_CLI_CMD -f glu-dev-1 -u admin -x admin $PRETEND -a -p redeploy
  sleep 1
  $CONSOLE_CLI_CMD -f glu-dev-1 -u admin -x admin $PRETEND -p -s "metadata.cluster='c1'" redeploy
  sleep 1
  $CONSOLE_CLI_CMD -f glu-dev-1 -u admin -x admin $PRETEND -p -s "metadata.cluster='c2'" redeploy	#statements
done
