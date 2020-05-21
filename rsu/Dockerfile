#
# Build stage
#
FROM maven:3.6.0-jdk-11-slim AS build
COPY ["ws-test/src", "/home/app/src"]
COPY ["ws-test/pom.xml", "/home/app"]
RUN mvn -f /home/app/pom.xml clean package

#
# Package stage
#
FROM openjdk:8
COPY --from=build /home/app/target/ws-test-1.0-SNAPSHOT.jar /usr/local/lib/demo.jar
ENTRYPOINT ["java","-jar","/usr/local/lib/demo.jar"]
