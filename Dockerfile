FROM            maven:3.5.4-jdk-8
MAINTAINER      Sam Kavanagh "s.kavanagh@auckland.ac.nz"

ARG             http_proxy
ARG             https_proxy

# Configure proxy for maven on UoA VMs
RUN		        if [ "$http_proxy" != "" ]; then \
		            proxy=$(basename $http_proxy); host=${proxy%:*}; port=${proxy#*:}; \
		            echo "/opt/maven/bin/mvn -DproxySet=true -DproxyHost=$host -DproxyPort=$port \$*" >> /usr/local/bin/mvn; \
		        else \
		            echo "/opt/maven/bin/mvn \$*" >> /usr/local/bin/mvn; \
		        fi

ENV             MAVEN_HOME /opt/maven
RUN             rm -f /apache-maven.tar.gz

# Build cer api jar with maven
WORKDIR         /cer-api/

# Resolve dependencies with maven, stops maven from re-downloading dependencies
COPY            /pom.xml /cer-api/pom.xml
RUN             mvn dependency:go-offline
RUN             mvn verify clean --fail-never

# Copy src files and build project
COPY            /src /cer-api/src
RUN 		    mvn -o package
RUN             mv target/app.jar /app.jar

ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-Dspring.config.location=file:/application.properties","-jar","/app.jar"]
