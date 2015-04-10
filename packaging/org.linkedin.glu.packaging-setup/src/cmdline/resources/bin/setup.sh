#!/bin/bash

#
# Copyright (c) 2010-2010 LinkedIn, Inc
# Portions Copyright (c) 2013 Yan Pujante
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

# locations
CURRDIR=`pwd`
BASEDIR=`cd $(dirname $0)/.. ; pwd`
GLU_VERSION=@glu.version@

cd $BASEDIR

LIB_DIR=lib

shopt -s nullglob

# first time... 'resolve' the links
for file in $LIB_DIR/*.jar.lnk
do
  ln -s `head -n 1 $file` $LIB_DIR
  rm $file
done

for file in `ls -1 $LIB_DIR/*.jar `; do
  if [ -z "$JVM_CLASSPATH" ]; then
    JVM_CLASSPATH="-classpath $JAVA_HOME/lib/tools.jar:$file"
  else
    JVM_CLASSPATH=$JVM_CLASSPATH:$file
  fi
done

JVM_LOG4J="-Dlog4j.configuration=file:$BASEDIR/conf/log4j.xml -Djava.util.logging.config.file=$CONF_DIR/logging.properties"
JVM_DEBUG=
#JVM_DEBUG="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"

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

$JAVA_CMD $JVM_LOG4J $JVM_DEBUG $JVM_CLASSPATH -Duser.pwd=$CURRDIR -Dglu.version=$GLU_VERSION org.pongasoft.glu.packaging.setup.SetupMain "$@"
