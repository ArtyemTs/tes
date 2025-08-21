FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -q -e -DskipTests dependency:go-offline
COPY src ./src
# важное изменение: добавить repackage
RUN mvn -q -DskipTests package spring-boot:repackage

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/target/tes-api-0.1.0.jar app.jar
ENV JAVA_OPTS="-XX:+UseZGC -Xms256m -Xmx512m"
EXPOSE 8080
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]
