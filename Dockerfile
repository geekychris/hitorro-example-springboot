# Multi-stage Docker build for Hitorro Example Spring Boot Application
# Stage 1: Build the application with Maven
FROM maven:3.9-eclipse-temurin-21 AS builder

# Set working directory
WORKDIR /build

# Copy all module POMs first (for better layer caching)
COPY hitorro-util/pom.xml /build/hitorro-util/pom.xml
COPY hitorro-base/pom.xml /build/hitorro-base/pom.xml
COPY hitorro-basedms/pom.xml /build/hitorro-basedms/pom.xml
COPY hitorro-spring-boot/pom.xml /build/hitorro-spring-boot/pom.xml
COPY hitorro-spring-boot/hitorro-spring-boot-autoconfigure/pom.xml /build/hitorro-spring-boot/hitorro-spring-boot-autoconfigure/pom.xml
COPY hitorro-spring-boot/hitorro-spring-boot-starter/pom.xml /build/hitorro-spring-boot/hitorro-spring-boot-starter/pom.xml

# Copy the example app pom
COPY hitorro-example-springboot/pom.xml /build/hitorro-example-springboot/pom.xml

# Download dependencies (this layer will be cached unless POMs change)
RUN cd /build/hitorro-example-springboot && mvn dependency:go-offline -B || true

# Copy the complete source code
COPY hitorro-util /build/hitorro-util
COPY hitorro-base /build/hitorro-base
COPY hitorro-basedms /build/hitorro-basedms
COPY hitorro-spring-boot /build/hitorro-spring-boot
COPY hitorro-example-springboot /build/hitorro-example-springboot

# Build dependencies first (skip test compilation entirely)
WORKDIR /build
RUN cd hitorro-util && mvn clean install -Dmaven.test.skip=true -B && \
    cd ../hitorro-base && mvn clean install -Dmaven.test.skip=true -B && \
    cd ../hitorro-basedms && mvn clean install -Dmaven.test.skip=true -B && \
    cd ../hitorro-spring-boot && mvn clean install -Dmaven.test.skip=true -B

# Build the example application
WORKDIR /build/hitorro-example-springboot
RUN mvn clean package -Dmaven.test.skip=true -B

# Stage 2: Create the runtime image
FROM eclipse-temurin:21-jre-alpine

# Install required system packages
RUN apk add --no-cache \
    ttf-dejavu \
    fontconfig \
    libreoffice \
    curl \
    bash

# Create application user
RUN addgroup -S hitorro && adduser -S hitorro -G hitorro

# Set up application directories
ENV HT_BIN=/opt/hitorro \
    HT_HOME=/var/lib/hitorro \
    APP_HOME=/opt/hitorro-app

RUN mkdir -p ${HT_BIN} ${HT_HOME} ${APP_HOME} && \
    mkdir -p ${HT_HOME}/config/csv \
    ${HT_HOME}/data \
    ${HT_HOME}/logs \
    ${APP_HOME}/data/files && \
    chown -R hitorro:hitorro ${HT_BIN} ${HT_HOME} ${APP_HOME}

# Copy CSV configuration files
COPY --chown=hitorro:hitorro hitorro-example-springboot/docker/csv/*.csv ${HT_HOME}/config/csv/

# Copy the built application from builder stage
COPY --from=builder --chown=hitorro:hitorro /build/hitorro-example-springboot/target/*.jar ${APP_HOME}/app.jar

# Switch to application user
USER hitorro

# Set working directory
WORKDIR ${APP_HOME}

# Expose application port
EXPOSE 8080

# Expose CLI ports (Telnet and SSH)
EXPOSE 9000 9022

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM Options
ENV JAVA_OPTS="-Xmx2g -Xms512m \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=${HT_HOME}/logs \
    -Djava.security.egd=file:/dev/./urandom"

# Run the application
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar ${APP_HOME}/app.jar"]
