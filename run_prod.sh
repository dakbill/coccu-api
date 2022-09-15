#!/bin/sh
if [ ! -f .env ]
then
  export $(cat .env | xargs)
fi

./mvnw spring-boot:run -Dmaven.test.skip=true -Dspring-boot.run.jvmArguments="-Dspring.profiles.active=dev" &
