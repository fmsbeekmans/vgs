#!/bin/bash

killall java
java -Djava.rmi.server.hostname=172.31.23.27 -Djava.net.preferIPv4Stack=true -Djava.security.manager -Djava.security.policy=./my.policy -jar GridScheduler.jar &