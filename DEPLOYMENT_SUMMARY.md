# 🚀 Resumen de Configuración Docker + Render

## ✅ Archivos Creados

### 📁 Configuración de Docker
- ✅ `Dockerfile` - Build multi-stage optimizado (Maven + JRE Alpine)
- ✅ `.dockerignore` - Optimización del contexto de build
- ✅ `docker-test.sh` - Script para probar localmente

### ⚙️ Configuración de Despliegue
- ✅ `application-prod.yml` - Perfil de producción con variables de entorno
- ✅ `render.yaml` - Blueprint para despliegue automático
- ✅ `DEPLOY_RENDER.md` - Guía completa de despliegue paso a paso

### 📝 Documentación
- ✅ `README.md` - Actualizado con sección de Docker y despliegue

### 🔧 Dependencias Agregadas
- ✅ `spring-boot-starter-actuator` - Para healthcheck en producción

---

## 🏗️ Arquitectura Docker

```
┌─────────────────────────────────────────────────────────┐
│ Etapa 1: BUILD (maven:3.9-eclipse-temurin-21-alpine)   │
│ ┌─────────────────────────────────────────────────────┐ │
│ │ 1. Copiar pom.xml                                   │ │
│ │ 2. Descargar dependencias (mvn dependency:offline)  │ │
│ │ 3. Copiar código fuente                             │ │
│ │ 4. Compilar y empaquetar (mvn clean package)        │ │
│ │ 5. Generar app.jar                                  │ │
│ └─────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────┐
│ Etapa 2: RUNTIME (eclipse-temurin:21-jre-alpine)       │
│ ┌─────────────────────────────────────────────────────┐ │
│ │ 1. Crear usuario no-root (spring:spring)            │ │
│ │ 2. Copiar app.jar desde etapa BUILD                 │ │
│ │ 3. Configurar healthcheck (/actuator/health)        │ │
│ │ 4. Exponer puerto 8080                              │ │
│ │ 5. Ejecutar con java $JAVA_OPTS -jar app.jar       │ │
│ └─────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
```

**Beneficios:**
- 🎯 Imagen final ligera (~150 MB vs ~500 MB con JDK)
- 🔒 Usuario no-root para seguridad
- ⚡ Cache de dependencias Maven
- 🏥 Healthcheck automático cada 30 segundos

---

## 🌐 Flujo de Despliegue en Render

```
┌──────────────┐
│ Git Push     │
│ (main branch)│
└──────┬───────┘
       │
       ↓
┌──────────────────────────────────────┐
│ Render detecta cambios               │
│ - Lee Dockerfile                     │
│ - Lee variables de entorno           │
└──────┬───────────────────────────────┘
       │
       ↓
┌──────────────────────────────────────┐
│ Build automático                     │
│ 1. Ejecuta Etapa 1 (Maven Build)    │
│ 2. Ejecuta Etapa 2 (JRE Runtime)    │
│ 3. Crea imagen Docker                │
└──────┬───────────────────────────────┘
       │
       ↓
┌──────────────────────────────────────┐
│ Deploy                               │
│ - Inicia contenedor                  │
│ - Asigna URL pública                 │
│ - Configura SSL/TLS automático       │
└──────┬───────────────────────────────┘
       │
       ↓
┌──────────────────────────────────────┐
│ Healthcheck                          │
│ GET /api/actuator/health             │
│ Verifica cada 30 segundos            │
└──────────────────────────────────────┘
       │
       ↓
┌──────────────────────────────────────┐
│ ✅ App en Producción                 │
│ https://tu-app.onrender.com/api      │
└──────────────────────────────────────┘
```

---

## 🔑 Variables de Entorno Requeridas

| Variable | Ejemplo | Descripción |
|----------|---------|-------------|
| `MONGODB_URL` | `mongodb+srv://user:pass@cluster.mongodb.net/furniture_store` | URI de MongoDB Atlas |
| `JWT_SECRET` | `(generado con openssl rand -base64 64)` | Clave secreta para JWT (mínimo 256 bits) |
| `JWT_EXPIRATION` | `86400000` | Expiración del token en milisegundos (24h) |
| `SPRING_PROFILES_ACTIVE` | `prod` | Activa perfil de producción |
| `PORT` | `8080` | Puerto (Render lo asigna, pero Spring usa 8080) |

---

## 📊 Comparación: Desarrollo vs Producción

| Aspecto | Desarrollo | Producción |
|---------|------------|------------|
| **Perfil** | default | prod |
| **Logs** | DEBUG | INFO/WARN |
| **MongoDB** | `MONGODB_URL` env var | `MONGODB_URL` env var |
| **JWT Secret** | Hardcoded (seguro solo para dev) | Variable de entorno |
| **Puerto** | 8080 | `${PORT:8080}` (configurable) |
| **CORS** | localhost:3000, localhost:5173 | + dominios de producción |
| **Actuator** | Todos los endpoints | Solo health e info |
| **Error Details** | Completos | Limitados |

---

## 🧪 Cómo Probar Antes de Desplegar

### 1. Probar Localmente con Docker

```bash
cd backend

# Build
docker build -t furniture-test .

# Run (usa tus credenciales de MongoDB Atlas)
docker run -d \
  --name furniture-test \
  -p 8080:8080 \
  -e MONGODB_URL="mongodb+srv://user:pass@cluster.mongodb.net/furniture_store" \
  -e JWT_SECRET="$(openssl rand -base64 64)" \
  -e SPRING_PROFILES_ACTIVE="prod" \
  furniture-test

# Ver logs
docker logs -f furniture-test

# Probar healthcheck
curl http://localhost:8080/api/actuator/health

# Probar API
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test","email":"test@test.com","password":"test123"}'

# Limpiar
docker stop furniture-test
docker rm furniture-test
docker rmi furniture-test
```

### 2. Verificar que MongoDB Atlas Permite Conexiones

1. Ir a MongoDB Atlas → Network Access
2. Agregar IP Address: `0.0.0.0/0` (permite desde cualquier IP)
   - ⚠️ Solo para desarrollo/pruebas
   - En producción seria, usa VPC peering o IPs específicas

### 3. Generar JWT Secret Seguro

```bash
# En Linux/Mac/Git Bash
openssl rand -base64 64

# En PowerShell
$bytes = New-Object byte[] 64
[Security.Cryptography.RNGCryptoServiceProvider]::Create().GetBytes($bytes)
[Convert]::ToBase64String($bytes)
```

---

## 🚦 Pasos para Despliegue en Render

### Opción 1: Despliegue Manual (UI)

1. **Push a GitHub**
   ```bash
   git add .
   git commit -m "Add Docker configuration for Render deployment"
   git push origin main
   ```

2. **Crear Web Service en Render**
   - Ir a https://dashboard.render.com/
   - New + → Web Service
   - Conectar repositorio
   - Name: `furniture-store-api`
   - Environment: Docker
   - Branch: main
   - Root Directory: `backend`

3. **Configurar Variables de Entorno**
   - Agregar las 5 variables listadas arriba

4. **Deploy**
   - Click "Create Web Service"
   - Esperar ~5-10 minutos

5. **Verificar**
   ```bash
   curl https://tu-app.onrender.com/api/actuator/health
   ```

### Opción 2: Despliegue con Blueprint (render.yaml)

1. **Configurar render.yaml**
   - Ya está creado en `backend/render.yaml`
   - Editar región y plan según necesidad

2. **Desde Render Dashboard**
   - New + → Blueprint
   - Conectar repositorio
   - Render lee `render.yaml` automáticamente
   - Configurar solo `MONGODB_URL` (el resto es automático)

---

## 📈 Monitoreo Post-Despliegue

### Logs en Tiempo Real
```
Render Dashboard → Tu Servicio → Logs
```

### Métricas
```
Render Dashboard → Tu Servicio → Metrics
- CPU Usage
- Memory Usage
- Network (Requests/sec)
```

### Healthcheck Manual
```bash
# Debe devolver: {"status":"UP"}
curl https://tu-app.onrender.com/api/actuator/health

# Ver info de la app
curl https://tu-app.onrender.com/api/actuator/info
```

---

## 🎯 Próximos Pasos Sugeridos

1. ✅ **Frontend Deploy**
   - Desplegar frontend en Netlify/Vercel
   - Actualizar `API_URL` en frontend para apuntar a Render

2. ✅ **Dominio Personalizado**
   - Configurar dominio propio en Render
   - Render provee SSL/TLS gratis con Let's Encrypt

3. ✅ **CI/CD**
   - Configurar GitHub Actions para tests automáticos
   - Auto-deploy en Render al hacer merge a main

4. ✅ **Backups**
   - Configurar backups automáticos en MongoDB Atlas
   - Snapshot diarios recomendados

5. ✅ **Monitoring Avanzado**
   - Integrar Datadog, New Relic, o Sentry
   - Alertas por email/Slack en caso de errores

---

## ⚡ Quick Reference

```bash
# Desarrollo Local
mvn spring-boot:run

# Build Docker
docker build -t furniture-backend .

# Run Docker Local
docker run -p 8080:8080 \
  -e MONGODB_URL="..." \
  -e JWT_SECRET="..." \
  -e SPRING_PROFILES_ACTIVE="prod" \
  furniture-backend

# Generar JWT Secret
openssl rand -base64 64

# Test Healthcheck
curl http://localhost:8080/api/actuator/health

# Ver logs Docker
docker logs -f <container-id>

# Push para auto-deploy en Render
git push origin main
```

---

## 🎉 ¡Listo para Producción!

Tu backend ahora está completamente configurado para:
- ✅ Desarrollo local con Spring Boot
- ✅ Contenedorización con Docker
- ✅ Despliegue en Render (PaaS)
- ✅ Healthchecks automáticos
- ✅ Seguridad con usuario no-root
- ✅ Configuración flexible por entornos
- ✅ Logging apropiado para producción
- ✅ Optimización de tamaño de imagen

**Costo estimado**: $0 - $7/mes (Free tier o Starter en Render)
