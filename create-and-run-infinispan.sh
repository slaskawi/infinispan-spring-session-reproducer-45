#!/bin/bash

rm -rf infinispan-server-9.2.1.Final || true
wget -N http://downloads.jboss.org/infinispan/9.2.1.Final/infinispan-server-9.2.1.Final-bin.zip
unzip infinispan-server-9.2.1.Final-bin.zip
cp standalone.xml ./infinispan-server-9.2.1.Final/standalone/configuration
./infinispan-server-9.2.1.Final/bin/add-user.sh -a -u user -p password
./infinispan-server-9.2.1.Final/bin/standalone.sh