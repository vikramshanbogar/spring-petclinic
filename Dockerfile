FROM amazoncorretto:21-alpine-jdk
LABEL authors="Vikram"
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} petclinic.jar
ENTRYPOINT ["java","-jar","/petclinic.jar"]
