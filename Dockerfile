FROM gradle:8.5.0-jdk21 AS build
COPY --chown=gradle:gradle . /home/gradle/project
WORKDIR /home/gradle/project
RUN gradle clean build

FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY --from=build /home/gradle/project/build/libs/*.jar app.jar
# Flags JVM para un contenedor de 1 CPU / 1 GB (T11):
# - MaxRAMPercentage=75: heap acotado al limite del contenedor, dejando margen
#   para metaspace, stacks y buffers nativos.
# - SerialGC: con 1 CPU evita que hilos de GC concurrentes compitan con los
#   hilos de la aplicacion; heap pequeño => pausas cortas.
# - ExitOnOutOfMemoryError: ante OOM el contenedor muere y puede reiniciarse,
#   en vez de quedar degradado.
# Se usa JAVA_TOOL_OPTIONS para que apliquen aunque se sobreescriba el comando.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+UseSerialGC -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["java", "-jar", "app.jar"]