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

# from http://stackoverflow.com/questions/59895/can-a-bash-script-tell-what-directory-its-stored-in
SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do
  DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE"
done
BASEDIR="$( cd -P "$( dirname "$SOURCE" )/.." && pwd )"
cd $BASEDIR
CONF_DIR=$BASEDIR/conf

JETTY_DISTRIBUTION=$BASEDIR/@jetty.distribution@

# log dir
LOG_DIR=$JETTY_DISTRIBUTION/logs

if [ ! -d $LOG_DIR ]; then
  mkdir $LOG_DIR
  touch $LOG_DIR/console.log
fi

# Files referenced in the script
GC_LOG=$LOG_DIR/gc.log

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
if [ -f $GLU_USER_CONFIG_DIR/master_conf.sh ]; then
  echo "Loading config [$GLU_USER_CONFIG_DIR/master_conf.sh]..."
  source $GLU_USER_CONFIG_DIR/master_conf.sh
fi

# Load Configuration Files - $GLU_USER_CONFIG_DIR/post_master_conf.sh last (if exists)
if [ -f $GLU_USER_CONFIG_DIR/post_master_conf.sh ]; then
  echo "Loading config [$GLU_USER_CONFIG_DIR/post_master_conf.sh]..."
  source $GLU_USER_CONFIG_DIR/post_master_conf.sh
fi

# Java Tuning Options (heap & generations sizes; GC tuning)
JVM_TUNING_OPTIONS="$JVM_SIZE $JVM_SIZE_NEW $JVM_SIZE_PERM $JVM_GC_TYPE $JVM_GC_OPTS $JVM_GC_LOG"

JAVA_OPTIONS="$JAVA_OPTIONS $JVM_TUNING_OPTIONS $JVM_APP_INFO -Dorg.linkedin.glu.console.config.location=$BASEDIR/conf/glu-console-webapp.groovy -Dorg.linkedin.glu.console.keys.dir=$BASEDIR/keys -Dorg.linkedin.glu.console.plugins.classpath=$PLUGINS_CLASSPATH -Dorg.linkedin.glu.console.root=$BASEDIR"

JAVA="$JAVA_CMD" JAVA_OPTIONS="$JAVA_OPTIONS" JETTY_PORT="$JETTY_PORT" $JETTY_CMD "$@"
