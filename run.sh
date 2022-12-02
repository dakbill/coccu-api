#!/bin/sh
export DB_HOST=127.0.0.1 && export DB_PORT=6005 && export DB_NAME=coccu && \
export DB_USERNAME=smartsapp && export DB_PASSWORD=geGlouvatlanifCiWav5 && \
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home \
./mvnw clean spring-boot:run \
-Dmaven.test.skip=true \
-Dspring-boot.run.jvmArguments="-Dspring.profiles.active=dev -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"