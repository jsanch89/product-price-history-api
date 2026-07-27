# ---- Build stage ----
FROM gradle:8.5.0-jdk21 AS build
WORKDIR /home/gradle/project
COPY --chown=gradle:gradle . .
RUN gradle clean build -x test --no-daemon

# ---- Layer extraction stage (Spring Boot layertools) ----
# El plugin de Spring Boot genera un jar en capas (dependencies, snapshot-dependencies,
# spring-boot-loader, application). Extraerlas por separado permite que Docker cachee
# la capa de dependencias (la que menos cambia) independientemente del codigo de la app.
FROM build AS extract
WORKDIR /home/gradle/project
RUN java -Djarmode=tools -jar build/libs/*.jar extract --layers --launcher --destination extracted

# ---- Runtime stage: JRE slim, sin JDK/herramientas de build ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=extract /home/gradle/project/extracted/dependencies/ ./
COPY --from=extract /home/gradle/project/extracted/spring-boot-loader/ ./
COPY --from=extract /home/gradle/project/extracted/snapshot-dependencies/ ./
COPY --from=extract /home/gradle/project/extracted/application/ ./

# Flags JVM para un contenedor de 1 CPU / 1 GB (T11):
# - MaxRAMPercentage=75: heap acotado al limite del contenedor, dejando margen
#   para metaspace, stacks y buffers nativos.
# - SerialGC: con 1 CPU evita que hilos de GC concurrentes compitan con los
#   hilos de la aplicacion; heap pequeño => pausas cortas.
# - ExitOnOutOfMemoryError: ante OOM el contenedor muere y puede reiniciarse,
#   en vez de quedar degradado.
# Se usa JAVA_TOOL_OPTIONS para que apliquen aunque se sobreescriba el comando.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+UseSerialGC -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
