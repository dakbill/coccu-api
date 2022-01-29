#!/bin/sh
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home \
./mvnw spring-boot:run \
-Dmaven.test.skip=true \
-Dspring-boot.run.jvmArguments="-Dspring.profiles.active=dev"
# -Dspring-boot.run.jvmArguments="-Dspring.profiles.active=dev -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"




