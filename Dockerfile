# Etapa 1: Build
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

# Copiar archivos de configuración de Maven
COPY pom.xml .

# Descargar dependencias (se cachea si pom.xml no cambia)
RUN mvn dependency:go-offline -B

# Copiar código fuente
COPY src ./src

# Compilar la aplicación y crear el JAR
RUN mvn clean package -DskipTests -B

# Etapa 2: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Crear usuario no-root para ejecutar la aplicación
RUN addgroup -S spring && adduser -S spring -G spring

# Copiar el JAR desde la etapa de build
COPY --from=build /app/target/*.jar app.jar

# Cambiar permisos
RUN chown spring:spring app.jar

# Cambiar a usuario no-root
USER spring:spring

# Exponer puerto (se asigna dinámicamente por el hosting)
EXPOSE 8080

# Variables de entorno por defecto
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Healthcheck (usa PORT si está definido, sino 8080)
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:${PORT:-8080}/api/actuator/health || exit 1

# Ejecutar la aplicación
# Northflank/Railway/Render usan la variable PORT para asignar el puerto dinámicamente
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT:-8080} -jar app.jar"]
