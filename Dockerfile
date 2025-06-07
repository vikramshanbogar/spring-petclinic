FROM amazoncorretto:21-alpine
LABEL authors="Vikram"
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} petclinic.jar
ENTRYPOINT ["java","-Xmx1000M" ,"-jar","/petclinic.jar"]
