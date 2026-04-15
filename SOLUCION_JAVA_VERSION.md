# 🔧 Solución Definitiva - Error Lombok + Java 21

## 🔴 Error:
```
Fatal error compiling: java.lang.ExceptionInInitializerError: 
com.sun.tools.javac.code.TypeTag :: UNKNOWN
```

Este es un error de **incompatibilidad entre Lombok y Java**.

---

## ✅ SOLUCIÓN DEFINITIVA:

He actualizado:
- **Lombok** → versión 1.18.32 (última versión estable para Java 21)
- **maven-compiler-plugin** → versión 3.13.0

---

## 🚀 EJECUTA ESTOS COMANDOS (EN ORDEN):

```bash
# 1. Navega al backend
cd C:\Users\Patricio.Sanchez\Documents\Proyectos\psanchezs\forniture_store\backend

# 2. ELIMINAR carpeta target (IMPORTANTE)
rmdir /s /q target

# 3. Limpiar dependencias y cache de Maven
mvn dependency:purge-local-repository -DactTransitively=false -DreResolve=false

# 4. Limpiar proyecto
mvn clean

# 5. Compilar SIN tests
mvn compile -DskipTests

# 6. Si compila bien, instalar
mvn install -DskipTests

# 7. Ejecutar la aplicación
mvn spring-boot:run
```

---

## 🔍 ALTERNATIVA: Verificar versión de Java

Antes de ejecutar, verifica que estés usando Java 21:

```bash
java -version
```

Deberías ver:
```
java version "21.x.x"
OpenJDK Runtime Environment
```

Si ves Java 17, 11, u otra versión, necesitas:

### Windows - Configurar JAVA_HOME:

1. Busca dónde está instalado Java 21:
```bash
dir "C:\Program Files\Java"
```

2. Configura JAVA_HOME (reemplaza la ruta con tu instalación de Java 21):
```bash
set JAVA_HOME=C:\Program Files\Java\jdk-21
set PATH=%JAVA_HOME%\bin;%PATH%
```

3. Verifica:
```bash
java -version
mvn -version
```

---

## 🛠️ Si IntelliJ sigue dando problemas:

### Opción 1: Usar Maven Wrapper

En lugar de `mvn`, usa `mvnw` (Maven Wrapper):

```bash
# Windows
.\mvnw.cmd clean install -DskipTests
.\mvnw.cmd spring-boot:run
```

### Opción 2: Configurar IntelliJ para usar Java 21

1. **File → Project Structure (Ctrl+Alt+Shift+S)**
   - Project SDK: Java 21
   - Language Level: 21

2. **File → Settings → Build Tools → Maven**
   - Maven home directory: usa Maven embebido
   - JRE for Maven: Java 21

3. **File → Settings → Build Tools → Maven → Runner**
   - JRE: Java 21

4. **Reload Maven Project**:
   - Click derecho en pom.xml
   - Maven → Reload Project

---

## 📦 Si no tienes Java 21:

### Descargar Java 21:

**Opción 1 - Oracle JDK:**
https://www.oracle.com/java/technologies/downloads/#java21

**Opción 2 - OpenJDK (Eclipse Temurin - Recomendado):**
https://adoptium.net/temurin/releases/?version=21

**Instalar con Chocolatey (Windows):**
```bash
choco install temurin21
```

---

## 🎯 EJECUTA ESTO PRIMERO:

```bash
# Verificar Java
java -version

# Si NO es Java 21, descárgalo e instálalo primero

# Una vez tengas Java 21:
cd C:\Users\Patricio.Sanchez\Documents\Proyectos\psanchezs\forniture_store\backend
rmdir /s /q target
mvn clean compile -DskipTests
```

---

## 💡 SOLUCIÓN MÁS RÁPIDA - Usar Java 17:

Si no quieres instalar Java 21, puedo cambiar el proyecto a Java 17:

```xml
<properties>
    <java.version>17</java.version>
    <maven.compiler.source>17</maven.compiler.source>
    <maven.compiler.target>17</maven.compiler.target>
</properties>
```

¿Quieres que cambie el proyecto a Java 17? Es más compatible y más usado en producción.

---

## ✅ Verifica tu versión de Java:

```bash
java -version
```

**Dime qué versión de Java tienes y te ayudo a proceder.**
