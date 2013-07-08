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

JETTY_DISTRIBUTION=$BASEDIR/@jetty.distribution@

if [ ! -d $JETTY_DISTRIBUTION ]; then
  echo "Setting up jetty..."
  tar -zxf $BASEDIR/glu/repository/tgzs/@jetty.archive@
  rm -rf $JETTY_DISTRIBUTION/contexts/*
  rm -rf $JETTY_DISTRIBUTION/webapps/*
  cp $BASEDIR/conf/*-jetty-context.xml $JETTY_DISTRIBUTION/contexts
  touch $JETTY_DISTRIBUTION/logs/console.log
  chmod +x $JETTY_DISTRIBUTION/bin/*.sh
fi

if [ -z "$PLUGINS_DIR"]; then
  PLUGINS_DIR=$BASEDIR/glu/repository/plugins
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

if [ -z "$JVM_SIZE" ]; then
  JVM_SIZE="-Xmx512m -XX:MaxPermSize=384m"
fi

if [ -z "$JAVA_OPTIONS" ]; then
  JAVA_OPTIONS=""
fi

JAVA_OPTIONS="$JAVA_OPTIONS $JVM_SIZE -Dorg.linkedin.app.name=org.linkedin.glu.console-webapp -Dorg.linkedin.glu.console.config.location=$BASEDIR/conf/glu-console-webapp.groovy -Dorg.linkedin.glu.console.keys.dir=$BASEDIR/keys -Dorg.linkedin.glu.console.plugins.classpath=$PLUGINS_CLASSPATH -Dorg.linkedin.glu.console.root=$BASEDIR"

JAVA_OPTIONS="$JAVA_OPTIONS" $JETTY_DISTRIBUTION/bin/jetty.sh "$@"
