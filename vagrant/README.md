Installation
============

Follow the steps below for your host os to install all required software and get the virtual machine up and running.

Steps (for OSX host)
--------------------

1. Install newest [Virtual Box](https://www.virtualbox.org/)
1. Install newest [Vagrant](http://downloads.vagrantup.com/)
1. `sudo sh -c 'echo "127.0.0.1 	glu-tutorial" >> /private/etc/hosts'`
1. `sudo dscacheutil -flushcache`
1. `git clone git@github.com:Ensighten/glu-vagrant.git`
1. `cd glu-vagrant`
1. `vagrant up`
1. `vagrant ssh`
1. `sudo su -`
1. `cd /var/lib/glu`
1. Now you are ready to follow along with the [glu tutorial](http://pongasoft.github.io/glu/docs/latest/html/tutorial.html)

Forwarded Ports
---------------
The following ports on the VM are forwarded to the host:

| Application | Guest Port | Host Port |
|---|---|---|
| console | 8080 | 8080 |
| webapp1 | 9000 | 9000 |
| webapp2 | 9001 | 9001 |
| webapp3 | 9002 | 9002 |

* [console](http://localhost:8080/console)
