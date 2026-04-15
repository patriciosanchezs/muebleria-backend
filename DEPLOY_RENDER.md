# Despliegue en Render - Furniture Store Backend

Este documento contiene las instrucciones para desplegar el backend de Furniture Store en Render.

## 📋 Pre-requisitos

1. Cuenta en [Render](https://render.com)
2. Cuenta en [MongoDB Atlas](https://www.mongodb.com/cloud/atlas) (o cualquier MongoDB en la nube)
3. Repositorio Git con el código

## 🚀 Pasos para Desplegar

### 1. Preparar MongoDB Atlas

1. Crear un cluster en MongoDB Atlas (si no tienes uno)
2. Ir a **Database Access** → Crear un usuario con permisos de lectura/escritura
3. Ir a **Network Access** → Agregar `0.0.0.0/0` para permitir acceso desde cualquier IP (Render usa IPs dinámicas)
4. Ir a **Database** → **Connect** → **Connect your application**
5. Copiar la cadena de conexión (URI), debe verse así:
   ```
   mongodb+srv://<username>:<password>@cluster.xxxxx.mongodb.net/furniture_store?retryWrites=true&w=majority
   ```

### 2. Crear Web Service en Render

1. Ir a [Render Dashboard](https://dashboard.render.com/)
2. Click en **"New +"** → **"Web Service"**
3. Conectar tu repositorio de GitHub/GitLab
4. Seleccionar el repositorio del proyecto

### 3. Configurar el Web Service

#### **Configuración Básica:**
- **Name**: `furniture-store-api` (o el nombre que prefieras)
- **Region**: Selecciona la más cercana a tus usuarios
- **Branch**: `main` (o la rama que uses)
- **Root Directory**: `backend`
- **Environment**: `Docker`
- **Dockerfile Path**: `./Dockerfile` (Render lo detectará automáticamente)
- **Docker Build Context Directory**: `./backend`

#### **Instancia:**
- **Instance Type**: 
  - Free (para pruebas, se duerme después de 15 min de inactividad)
  - Starter ($7/mes, siempre activo)

### 4. Variables de Entorno

En la sección **Environment Variables**, agregar las siguientes variables:

| Variable | Valor | Descripción |
|----------|-------|-------------|
| `MONGODB_URL` | `mongodb+srv://...` | URI de conexión a MongoDB Atlas |
| `JWT_SECRET` | `tu-secret-super-seguro-de-al-menos-256-bits` | Clave secreta para JWT (generala con: `openssl rand -base64 64`) |
| `JWT_EXPIRATION` | `86400000` | Expiración del token (24 horas en ms) |
| `SPRING_PROFILES_ACTIVE` | `prod` | Activa el perfil de producción |
| `PORT` | `8080` | Puerto (Render lo asigna automáticamente, pero Spring Boot usa 8080) |

**Generar JWT_SECRET seguro:**
```bash
openssl rand -base64 64
```

### 5. Configurar CORS (si es necesario)

Si tu frontend está en otro dominio, asegúrate de que tu backend permita CORS. Esto ya debería estar configurado en tu código Spring Boot.

### 6. Desplegar

1. Click en **"Create Web Service"**
2. Render automáticamente:
   - Detectará el Dockerfile
   - Construirá la imagen Docker
   - Desplegará el contenedor
   - Asignará una URL pública: `https://furniture-store-api.onrender.com`

### 7. Verificar Despliegue

Una vez desplegado, verifica:

1. **Health Check**: 
   ```
   https://tu-app.onrender.com/api/actuator/health
   ```
   Debe devolver: `{"status":"UP"}`

2. **Endpoints de Auth**:
   ```bash
   # Registrar usuario
   curl -X POST https://tu-app.onrender.com/api/auth/register \
     -H "Content-Type: application/json" \
     -d '{"username":"test","email":"test@test.com","password":"test123"}'

   # Login
   curl -X POST https://tu-app.onrender.com/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username":"test","password":"test123"}'
   ```

## 🔧 Configuración del Frontend

En tu frontend, actualiza la URL de la API:

```typescript
// src/services/api.ts
const API_URL = 'https://furniture-store-api.onrender.com/api';
```

## 📊 Monitoring y Logs

### Ver Logs en Tiempo Real:
1. En el dashboard de Render, ve a tu servicio
2. Click en **"Logs"**
3. Podrás ver los logs de Spring Boot en tiempo real

### Métricas:
1. Click en **"Metrics"**
2. Verás CPU, memoria, y tráfico de red

## 🔄 Re-despliegues Automáticos

Render automáticamente re-despliega cuando:
- Haces push a la rama configurada (main)
- Cambias variables de entorno
- Haces deploy manual desde el dashboard

## ⚠️ Troubleshooting

### Problema: "Application failed to start"
- Revisar logs en el dashboard de Render
- Verificar que todas las variables de entorno estén configuradas
- Verificar que MongoDB Atlas permita conexiones desde cualquier IP

### Problema: "Connection timeout to MongoDB"
- Verificar IP whitelist en MongoDB Atlas
- Verificar que la URI de MongoDB sea correcta
- Verificar que el usuario/password sean correctos

### Problema: "Port already in use"
- Asegurarse de usar `${PORT:8080}` en application-prod.yml
- Render asigna dinámicamente el puerto, pero Spring Boot usa 8080 internamente

### Problema: "JWT Secret error"
- Asegurarse de que JWT_SECRET tenga al menos 256 bits (32 caracteres)
- Regenerar con: `openssl rand -base64 64`

## 💰 Costos

- **Free Tier**: 
  - 750 horas/mes gratis
  - Se duerme después de 15 min de inactividad
  - Tarda ~30 segundos en despertar
  
- **Starter ($7/mes)**:
  - Siempre activo
  - 512 MB RAM
  - 0.5 CPU
  - Suficiente para aplicaciones pequeñas/medianas

## 🔐 Seguridad

1. **Nunca** commitees secrets al repositorio
2. Usa variables de entorno para todas las credenciales
3. Mantén MongoDB Atlas con IP whitelist (0.0.0.0/0 solo para desarrollo)
4. Considera usar VPC peering para producción seria
5. Habilita autenticación de 2 factores en Render y MongoDB Atlas

## 📚 Recursos

- [Render Docs - Docker](https://render.com/docs/docker)
- [Spring Boot Docker](https://spring.io/guides/gs/spring-boot-docker/)
- [MongoDB Atlas Docs](https://docs.atlas.mongodb.com/)

## 🎯 Próximos Pasos

1. Configurar dominio personalizado (opcional)
2. Configurar SSL/TLS (Render lo provee gratis)
3. Configurar monitoreo con herramientas externas (Datadog, New Relic, etc.)
4. Implementar CI/CD con GitHub Actions
5. Configurar backup automático de MongoDB
