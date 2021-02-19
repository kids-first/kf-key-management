FROM openjdk:11.0.4-jdk-slim as build-keys

WORKDIR /tmp/kf-key-management
COPY . .

RUN ./mvnw package -DskipTests

FROM openjdk:11.0.4-jre-slim

RUN mkdir -p /opt/kidsfirst/keys
COPY --from=build-keys /tmp/kf-key-management/target/keys.jar /opt/kidsfirst/keys/keys.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","/opt/kidsfirst/keys/keys.jar"]