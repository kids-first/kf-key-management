FROM eclipse-temurin:17-jdk-alpine as build-keys

WORKDIR /tmp/kf-key-management
COPY . .

RUN ./mvnw package -DskipTests

FROM eclipse-temurin:17-jdk-alpine

# Mitigates CVE-2026-114938 (Java4Shell); drop once Temurin ships a patched 17.
ENV JAVA_TOOL_OPTIONS=-XX:-UseBASE64Intrinsics

RUN mkdir -p /opt/kidsfirst/keys
COPY --from=build-keys /tmp/kf-key-management/target/keys.jar /opt/kidsfirst/keys/keys.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","/opt/kidsfirst/keys/keys.jar"]