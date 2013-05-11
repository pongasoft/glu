#!/usr/bin/env bash

set -xv

if [ -e /var/lib/glu ]
then
	echo "glu already installed"
	exit 0
fi

GLU_VERSION=$1
if [ -z "$GLU_VERSION" ]
then
	echo "ERROR: GLU_VERSION not specified"
	exit 1
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
export JAVA_HOME=/usr/lib/jvm/java-7-oracle
export JRE_HOME=/usr/lib/jvm/java-7-oracle/jre
echo 'export JAVA_HOME=/usr/lib/jvm/java-7-oracle' >> /etc/profile
echo 'export JRE_HOME=/usr/lib/jvm/java-7-oracle/jre' >> /etc/profile
echo 'export JAVA_HOME=/usr/lib/jvm/java-7-oracle' >> /etc/bash.bashrc
echo 'export JRE_HOME=/usr/lib/jvm/java-7-oracle/jre' >> /etc/bash.bashrc
ln -s $JRE_HOME/bin/java /bin/java

# install and configure apache
apt-get -y install apache2
a2enmod proxy_http
a2enmod headers
cp /vagrant/default /etc/apache2/sites-available/default
cp /vagrant/ports.conf /etc/apache2/ports.conf
service apache2 restart

# install glu tutorial
GLU_TUTORIAL_ROOT=/var/lib/glu
mkdir -p $GLU_TUTORIAL_ROOT
cd $GLU_TUTORIAL_ROOT
wget -q http://dl.bintray.com/content/pongasoft/glu/org.linkedin.glu.packaging-all-${GLU_VERSION}.tgz?direct -O glu.tgz
tar zxf glu.tgz -C $GLU_TUTORIAL_ROOT --strip-components=1
#JAVA_HOME=/usr/lib/jvm/java-7-oracle JRE_HOME=/usr/lib/jvm/java-7-oracle/jre ./bin/tutorial.sh setup
#JAVA_HOME=/usr/lib/jvm/java-7-oracle JRE_HOME=/usr/lib/jvm/java-7-oracle/jre ./bin/tutorial.sh start
