#!/bin/sh
export $(cat .env | xargs)
./mvnw clean spring-boot:run -Dmaven.test.skip=true