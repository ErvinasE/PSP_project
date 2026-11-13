FROM eclipse-temurin:21-jdk AS build
WORKDIR /build

# Dependency layer (cached until pom.xml or config/ changes)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY config/ config/
RUN chmod +x mvnw && ./mvnw -q dependency:go-offline

# Tests and static analysis run in `mvn verify`, not in the image build
COPY src/ src/
RUN ./mvnw -q -DskipTests -Dcheckstyle.skip -Dspotbugs.skip package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app --no-create-home app
COPY --from=build /build/target/compliance-checker-*.jar /app/app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
