#!/bin/bash

if [ "$1" == "--local" ]; then
    # If the local flag is passed, we run a version of the cer api project set up for development.
    # Run dependency to make sure new dependencies since last Docker build are installed.
    mvn dependency:go-offline
    mvn spring-boot:run -Drun.jvmArguments=-Dspring.config.location=/application.properties
    exit
fi

java -Djava.security.egd=file:/dev/./urandom -Dspring.config.location=file:/application.properties -jar /app.jar
