#!/bin/bash

killall java
#Param1: jobDuration
#Param2: user ID
#Param3: flag indicating type of simulation (regular, offload, sequential).
#Param4 (optional): number of jobs per RM.
java -Djava.rmi.server.hostname=172.31.28.4 -Djava.net.preferIPv4Stack=true -Djava.security.manager -Djava.security.policy=./my.policy -jar User.jar 2000 0 regular 500 &
java -Djava.rmi.server.hostname=172.31.28.4 -Djava.net.preferIPv4Stack=true -Djava.security.manager -Djava.security.policy=./my.policy -jar User.jar 2000 1 regular 500 &

#For testing the offload/load balance
#java -Djava.rmi.server.hostname=172.31.28.4 -Djava.net.preferIPv4Stack=true -Djava.security.manager -Djava.security.policy=./my.policy -jar User.jar 2000 2 10 &
#java -Djava.rmi.server.hostname=172.31.28.4 -Djava.net.preferIPv4Stack=true -Djava.security.manager -Djava.security.policy=./my.policy -jar User.jar 2000 3 10 &
