FROM            maven:3.5.4-jdk-8 AS prepare
MAINTAINER      Sam Kavanagh "s.kavanagh@auckland.ac.nz"

ARG             http_proxy
ARG             https_proxy

ENV             MAVEN_HOME /opt/maven

# Build cer api jar with maven
WORKDIR         /cer-api/

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

# Local development stage.
FROM		prepare	AS local
VOLUME		["/cer-api/src","/application.properties","/cer-api/target","/cer-api/pom.xml"]
ENTRYPOINT ["/docker-entrypoint.sh","--local"]


# Build stage.
FROM		prepare	AS build

# Copy src files and build project
COPY            /src /cer-api/src
COPY            application.properties /
RUN             mvn -o package
RUN             mv target/app.jar /app.jar

ENTRYPOINT ["/docker-entrypoint.sh"]
