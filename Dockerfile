FROM amazoncorretto:21-alpine-jdk
LABEL authors="Vikram"
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} petclinic.jar

# Configure keystore location for container environment
ENV APP_SSL_KEYSTORE_PATH=/tmp/keystore.p12
ENV SERVER_SSL_KEY_STORE=file:/tmp/keystore.p12

ENTRYPOINT ["java","-jar","/petclinic.jar"]
