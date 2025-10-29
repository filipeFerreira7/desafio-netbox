FROM maven:3.9.0-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src

RUN mvn -B -e -C -T 1C org.apache.maven.plugins:maven-dependency-plugin:3.6.0:go-offline
RUN mvn -B -e -T 1C package -DskipTests

FROM eclipse-temurin:17-jre-alpine
VOLUME /tmp
COPY --from=builder /app/target/descobre-ip-0.0.1-SNAPSHOT.jar app.jar

ENV JAVA_OPTS="-Xms256m -Xmx512m" \
    NETBOX_URL="http://netbox:8080" \
    NETBOX_VERIFY_SSL=true

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app.jar"]
