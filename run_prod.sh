#!/bin/sh
#JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home \
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 && \
./mvnw spring-boot:run \
-Dmaven.test.skip=true \
-Dspring-boot.run.jvmArguments="-Dspring.profiles.active=dev" &
