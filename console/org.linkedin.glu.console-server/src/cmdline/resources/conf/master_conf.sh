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

# set JAVA_HOME (JDK6) & JAVA_CMD
if [ -z "$JAVA_HOME" ]; then
  echo "Please set JAVA_HOME manually."
  exit 1
fi

if [ -z "$JAVA_CMD" ]; then
  JAVA_CMD=$JAVA_HOME/bin/java
fi

if [ -z "$JAVA_CMD_TYPE" ]; then
  FULLVERSION=$( $JAVA_CMD -fullversion 2>&1 )
  if [ -z "$(echo $FULLVERSION | grep IBM)" ]; then
    JAVA_CMD_TYPE=oracle
  else
    JAVA_CMD_TYPE=ibm
  fi
fi

if [ -z "$PLUGINS_DIR"]; then
  PLUGINS_DIR=$BASEDIR/glu/repository/plugins
fi

# Application name (for the container/cmdline app) - build-time expansion
if [ -z "$APP_NAME" ]; then
  APP_NAME="org.linkedin.glu.console-webapp"
fi

# Application version (for the container/cmdline app) - build-time expansion
if [ -z "$APP_VERSION" ]; then
  APP_VERSION="@glu.version@"
fi

# this will make $PLUGINS_DIR/*.jar returns empty string
shopt -s nullglob

# Add all plugins from the plugins directory to the plugins classpath
if [ -d $PLUGINS_DIR ]; then
  for file in $PLUGINS_DIR/*.jar
  do
    if [ -z "$PLUGINS_CLASSPATH" ]; then
      PLUGINS_CLASSPATH="$file"
    else
      PLUGINS_CLASSPATH="$PLUGINS_CLASSPATH;$file"
    fi
  done
fi

# Min, max, total JVM size (-Xms -Xmx)
if [ -z "$JVM_SIZE" ]; then
  JVM_SIZE=-Xmx512m
fi

# New Generation Sizes (-XX:NewSize -XX:MaxNewSize)
if [ -z "$JVM_SIZE_NEW" ]; then
  JVM_SIZE_NEW=
fi

# Perm Generation Sizes (-XX:PermSize -XX:MaxPermSize)
if [ -z "$JVM_SIZE_PERM" ]; then
  JVM_SIZE_PERM=-XX:MaxPermSize=384m
fi

# Type of Garbage Collector to use
if [ -z "$JVM_GC_TYPE" ]; then
  JVM_GC_TYPE=
fi

# Tuning options for the above garbage collector
if [ -z "$JVM_GC_OPTS" ]; then
  JVM_GC_OPTS=
fi

# JVM GC activity logging settings ($LOG_DIR set in the ctl script)
if [ -z "$JVM_GC_LOG" ]; then
  case "$JAVA_CMD_TYPE" in
    'ibm' ) JVM_GC_LOG="-Xverbosegclog:$GC_LOG"
    	    ;;
    'oracle' ) JVM_GC_LOG="-XX:+PrintGCDateStamps -Xloggc:$GC_LOG"
    	       ;;
  esac

fi

# Application Info - name, version, instance, etc.
if [ -z "$JVM_APP_INFO" ]; then
  JVM_APP_INFO="-Dorg.linkedin.app.name=$APP_NAME -Dorg.linkedin.app.version=$APP_VERSION"
fi

if [ -z "$JETTY_CMD" ]; then
  JETTY_CMD=$JETTY_DISTRIBUTION/bin/jetty.sh
fi

if [ -z "$JAVA_OPTIONS" ]; then
  JAVA_OPTIONS=
fi

