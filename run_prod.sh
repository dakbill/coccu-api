#!/bin/sh
export $(cat .env | xargs) && ./mvnw spring-boot:run -Dmaven.test.skip=true -Dspring-boot.run.jvmArguments="-Dspring.profiles.active=dev" &
