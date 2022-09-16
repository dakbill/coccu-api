#!/bin/sh
kill -9 $(lsof -t -i:18080)
export $(cat .env | xargs)
./mvnw spring-boot:run -Dmaven.test.skip=true -Dspring-boot.run.jvmArguments="-Dspring.profiles.active=dev" &
