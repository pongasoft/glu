Installation
============

Follow the steps below for your host os to install all required software and get the virtual machine up and running.

Steps (for OSX host)
--------------------

1. Install newest [Virtual Box](https://www.virtualbox.org/)
1. Install newest [Vagrant](http://downloads.vagrantup.com/)
1. `git clone git@github.com:pongasoft/glu.git`
1. `cd glu/vagrant`
1. `vagrant up --glu-version=5.0.0` (or specify another verion)
1. `vagrant ssh`
1. `sudo su -l`
1. `cd /var/lib/glu`
1. Now you are ready to follow along with the [glu tutorial](http://pongasoft.github.io/glu/docs/latest/html/tutorial.html)

NOTE: If you see errors from the glu tutorial script about JAVA_HOME not being set, run the command as follows: 
`JAVA_HOME=/usr/lib/jvm/java-7-oracle JRE_HOME=/usr/lib/jvm/java-7-oracle/jre ./bin/tutorial.sh`

Forwarded Ports
---------------
The following ports on the VM are forwarded to the host:

| Application | Guest Port | Host Port |
|---|---|---|
| apache proxy to glu console | 8000 | 8000 |
| glu console | 8080 | 8080 |
| webapp1 | 9000 | 9000 |
| webapp2 | 9001 | 9001 |
| webapp3 | 9002 | 9002 |

* [console](http://localhost:8000/console)

NOTE: The apache frontend to glu rewrites the location header to deal with the issue mentioned [here](http://glu.977617.n3.nabble.com/Glu-console-does-more-absolute-url-redirects-with-4-7-0-td4025588.html).
