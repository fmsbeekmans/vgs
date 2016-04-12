#!/bin/bash

killall java &
#Param1 ID of starting RM
#Param2 ID of ending RM
java -Djava.rmi.server.hostname=172.31.26.186 -Djava.net.preferIPv4Stack=true -Djava.security.manager -Djava.security.policy=./my.policy -jar ResourceManager.jar 0 9 &
