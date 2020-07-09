#!/bin/bash

mvn -f ./ws-test/pom.xml clean package

export ATTACH_HOST=192.168.80.1
export  ATTACH_PORT=8080
export  REMOTE_HOST=192.168.80.32
export REMOTE_PORT=5122

java -jar ./ws-test/target/ws-test-1.0-SNAPSHOT.jar

