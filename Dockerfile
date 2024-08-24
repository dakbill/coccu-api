FROM openjdk:21-jdk-slim AS builder
WORKDIR extracted
ADD target/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

FROM openjdk:21-jdk-slim
WORKDIR application
COPY --from=builder extracted/dependencies/ ./
COPY --from=builder extracted/spring-boot-loader/ ./
COPY --from=builder extracted/snapshot-dependencies/ ./
COPY --from=builder extracted/application/ ./


ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]
