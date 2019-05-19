FROM            maven:3.6.1-jdk-8 AS prepare
MAINTAINER      Sam Kavanagh "s.kavanagh@auckland.ac.nz"

ARG             http_proxy
ARG             https_proxy

WORKDIR         /cer-api/


# Local development stage.
FROM		prepare	AS local

# Create a user in the image that has the same UID as the host user and run the Docker image as the user, so that generated classfiles can be shared between host and image.
ARG		current_uid
RUN		useradd -m --uid $current_uid cerapi
USER		cerapi

# Mount source and generated classfile directories as volumes
VOLUME		["/cer-api/src","/application.properties","/cer-api/target","/cer-api/pom.xml","/docker-entrypoint.sh"]

ENTRYPOINT ["/docker-entrypoint.sh","--local"]


# Build stage.
FROM		prepare	AS build


# Resolve dependencies with maven, stops maven from re-downloading dependencies
COPY            /pom.xml /cer-api/pom.xml
COPY		/docker-entrypoint.sh /

# Configure proxy for maven on UoA vms
RUN		if [ -z $http_proxy ]; then \
			mvn dependency:go-offline; \
			mvn verify clean --fail-never; \
		else \
			proxy=$(basename $http_proxy); host=${proxy%:*}; port=${proxy#*:}; \
			export MAVEN_OPTS="-DproxySet=true -DproxyHost=$host -DproxyPort=$port"; \
			echo $MAVEN_OPTS; \
			mvn dependency:go-offline; \
			mvn verify clean --fail-never; \
		fi;

# Copy src files and build project
COPY            /src /cer-api/src
COPY            application.properties /
RUN             mvn -o package
RUN             mv target/app.jar /app.jar

ENTRYPOINT ["/docker-entrypoint.sh"]
