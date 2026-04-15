# 🔧 Solución Error: java.lang.ExceptionInInitializerError - Lombok

## 🔴 Error Completo:
```
java: java.lang.ExceptionInInitializerError
com.sun.tools.javac.code.TypeTag :: UNKNOWN
```

Este error ocurre por incompatibilidad entre **Lombok** y **Java 21**.

---

## ✅ SOLUCIÓN RÁPIDA (3 pasos):

### Paso 1: Recargar Maven en IntelliJ

He actualizado el `pom.xml` con la versión correcta de Lombok.

En IntelliJ:
1. Click derecho en `pom.xml`
2. `Maven` → `Reload Project`

O usa este atajo:
- **Windows**: `Ctrl + Shift + O`

### Paso 2: Limpiar y Recompilar

```bash
# Desde terminal en la carpeta backend:
cd C:\Users\Patricio.Sanchez\Documents\Proyectos\psanchezs\forniture_store\backend

mvn clean install
```

### Paso 3: Verificar Plugin de Lombok en IntelliJ

1. `File` → `Settings` (Ctrl + Alt + S)
2. `Plugins`
3. Busca **"Lombok"**
4. Si no está instalado, click `Install`
5. Reinicia IntelliJ

Luego:
1. `File` → `Settings` → `Build, Execution, Deployment` → `Compiler` → `Annotation Processors`
2. ✅ Marca **"Enable annotation processing"**
3. Click `OK`

---

## 🔍 Si el error persiste:

### Opción A: Invalidar Caché de IntelliJ

1. `File` → `Invalidate Caches...`
2. Marca todas las opciones
3. Click `Invalidate and Restart`

### Opción B: Verificar Java SDK en IntelliJ

1. `File` → `Project Structure` (Ctrl + Alt + Shift + S)
2. `Project Settings` → `Project`
3. **Project SDK**: Debe ser Java 21
4. **Language Level**: 21 - Pattern matching for switch
5. Click `OK`

También verifica en:
1. `File` → `Settings` → `Build, Execution, Deployment` → `Build Tools` → `Maven`
2. **Maven JDK**: Debe ser Java 21

### Opción C: Actualizar Lombok Plugin

1. `File` → `Settings` → `Plugins`
2. Busca "Lombok"
3. Si está instalado, verifica que sea la versión más reciente
4. Si hay actualización disponible, actualízala
5. Reinicia IntelliJ

---

## 🎯 Ejecutar desde Terminal (Alternativa)

Si IntelliJ sigue dando problemas, ejecuta desde terminal:

```bash
cd C:\Users\Patricio.Sanchez\Documents\Proyectos\psanchezs\forniture_store\backend

# Limpiar
mvn clean

# Compilar
mvn compile

# Ejecutar
mvn spring-boot:run
```

Esto debería funcionar sin problemas desde la terminal.

---

## ✅ Verificación Final

Una vez solucionado, deberías ver:

```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.2.1)

2024-XX-XX INFO  c.m.FurnitureStoreApplication - Starting FurnitureStoreApplication
2024-XX-XX INFO  c.m.FurnitureStoreApplication - Started FurnitureStoreApplication in X seconds
```

---

## 📋 Resumen de Cambios

He actualizado `pom.xml` para:
- Especificar versión de Lombok compatible: **1.18.30**
- Esta versión es totalmente compatible con Java 21

Ahora sigue los 3 pasos de arriba y debería funcionar perfectamente. 🚀
