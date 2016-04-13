#!/bin/bash

killall java
#Param1 ID of starting RM
#Param2 ID of ending RM
#Param3 numbef of nodes
#Normal test
java -Djava.rmi.server.hostname=172.31.29.133 -Djava.net.preferIPv4Stack=true -Djava.security.manager -Djava.security.policy=./my.policy -jar ResourceManager.jar 10 19 1000 &
#For the offloading test just, 2 nodes per RM
#java -Djava.rmi.server.hostname=172.31.29.133 -Djava.net.preferIPv4Stack=true -Djava.security.manager -Djava.security.policy=./my.policy -jar ResourceManager.jar 10 19 2 &
