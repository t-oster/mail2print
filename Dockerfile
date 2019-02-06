FROM maven:3-jdk-8-alpine AS compile
ADD . /build
WORKDIR /build
RUN mvn package

FROM woahbase/alpine-libreoffice:x86_64
WORKDIR /app
COPY --from=compile /build/target/Mail2Print-1.0-SNAPSHOT-jar-with-dependencies.jar .
ENTRYPOINT ["java", "-jar", "/app/Mail2Print-1.0-SNAPSHOT-jar-with-dependencies.jar"]