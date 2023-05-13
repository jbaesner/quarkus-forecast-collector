FROM maven:3-openjdk-11 AS builder
COPY ./pom.xml /usr/src/app/
RUN mvn -f /usr/src/app/pom.xml -B de.qaware.maven:go-offline-maven-plugin:resolve-dependencies
COPY src /usr/src/app/src
WORKDIR /usr/src/app
RUN mvn -DskipTests=true -Dquarkus.package.type=mutable-jar -B de.qaware.maven:go-offline-maven-plugin:resolve-dependencies clean package

FROM registry.access.redhat.com/ubi8/ubi-minimal:8.3 
ARG JAVA_PACKAGE=java-11-openjdk-headless
ARG RUN_JAVA_VERSION=1.3.8
ENV LANG='en_US.UTF-8' LANGUAGE='en_US:en'
# Install java and the run-java script
# Also set up permissions for user `1001`
RUN microdnf install curl ca-certificates ${JAVA_PACKAGE} \
    && microdnf update \
    && microdnf clean all \
    && mkdir /deployments \
    && chown 1001 /deployments \
    && chmod "g+rwX" /deployments \
    && chown 1001:root /deployments \
    && curl https://repo1.maven.org/maven2/io/fabric8/run-java-sh/${RUN_JAVA_VERSION}/run-java-sh-${RUN_JAVA_VERSION}-sh.sh -o /deployments/run-java.sh \
    && chown 1001 /deployments/run-java.sh \
    && chmod 540 /deployments/run-java.sh \
    && echo "securerandom.source=file:/dev/urandom" >> /etc/alternatives/jre/lib/security/java.security
# Configure the JAVA_OPTIONS, you can add -XshowSettings:vm to also display the heap size.
ENV JAVA_OPTIONS="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"
# We make four distinct layers so if there are application changes the library layers can be re-used
COPY --chown=1001 --from=builder /usr/src/app/target/quarkus-app/lib/ /deployments/lib/
COPY --chown=1001 --from=builder /usr/src/app/target/quarkus-app/*.jar /deployments/
COPY --chown=1001 --from=builder /usr/src/app/target/quarkus-app/app/ /deployments/app/
COPY --chown=1001 --from=builder /usr/src/app/target/quarkus-app/quarkus/ /deployments/quarkus/
EXPOSE 8080
USER 1001
ENTRYPOINT [ "/deployments/run-java.sh" ]
