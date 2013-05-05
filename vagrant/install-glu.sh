#!/usr/bin/env bash

if [ -e /var/lib/glu ]
then
	echo "glu already installed"
	exit 0
fi

# run all apt commands in noninteractive mode
export DEBIAN_FRONTEND=noninteractive

# install oracle java 7
# https://github.com/flexiondotorg/oab-java6
cd ~/
wget -q https://github.com/flexiondotorg/oab-java6/raw/0.2.7/oab-java.sh -O oab-java.sh
chmod +x oab-java.sh
./oab-java.sh -7
apt-get -q -y -f install oracle-java7-jre oracle-java7-bin oracle-java7-jdk
echo 'JAVA_HOME=/usr/lib/jvm/java-7-oracle' >> /etc/environment
echo 'JRE_HOME=/usr/lib/jvm/java-7-oracle/jre' >> /etc/environment
. /etc/environment
ln -s $JRE_HOME/bin/java /bin/java

# install glu tutorial
GLU_TUTORIAL_ROOT=/var/lib/glu
mkdir -p $GLU_TUTORIAL_ROOT
cd $GLU_TUTORIAL_ROOT
wget -q http://dl.bintray.com/content/pongasoft/glu/org.linkedin.glu.packaging-all-5.0.0.tgz?direct -O glu.tgz
tar zxf glu.tgz -C $GLU_TUTORIAL_ROOT --strip-components=1
