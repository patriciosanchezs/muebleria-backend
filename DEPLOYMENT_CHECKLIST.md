# ✅ Checklist de Despliegue - Furniture Store Backend

## 📋 Pre-Despliegue

### MongoDB Atlas
- [ ] Crear cuenta en MongoDB Atlas (https://www.mongodb.com/cloud/atlas)
- [ ] Crear cluster (Free M0 tier disponible)
- [ ] Crear usuario de base de datos con permisos `readWrite`
- [ ] Configurar Network Access: Agregar `0.0.0.0/0`
- [ ] Obtener connection string (URI)
- [ ] Probar conexión localmente

### Código
- [ ] Dockerfile creado y probado localmente
- [ ] .dockerignore configurado
- [ ] application-prod.yml configurado con variables de entorno
- [ ] pom.xml con dependencia de actuator
- [ ] .gitignore actualizado (no commitear .env)
- [ ] README.md actualizado con instrucciones de despliegue
- [ ] Código pusheado a GitHub/GitLab

### Seguridad
- [ ] Generar JWT_SECRET seguro (mínimo 256 bits)
  ```bash
  openssl rand -base64 64
  ```
- [ ] Verificar que no hay secretos hardcodeados en el código
- [ ] Verificar que .env y .env.local están en .gitignore

---

## 🚀 Despliegue en Render

### Crear Web Service
- [ ] Ir a https://dashboard.render.com
- [ ] Click en "New +" → "Web Service"
- [ ] Conectar repositorio (GitHub/GitLab)
- [ ] Seleccionar repositorio del proyecto

### Configuración Básica
- [ ] **Name**: `furniture-store-api` (o tu nombre preferido)
- [ ] **Region**: Seleccionar región más cercana
- [ ] **Branch**: `main` (o tu rama principal)
- [ ] **Root Directory**: `backend`
- [ ] **Environment**: `Docker`
- [ ] **Dockerfile Path**: `./Dockerfile`
- [ ] **Instance Type**: Seleccionar plan (Free o Starter)

### Variables de Entorno
Agregar todas estas variables en la sección "Environment Variables":

- [ ] **MONGODB_URL**
  - Valor: `mongodb+srv://usuario:password@cluster.mongodb.net/furniture_store`
  - ⚠️ Reemplazar usuario, password, y cluster con tus valores

- [ ] **JWT_SECRET**
  - Valor: (generado con `openssl rand -base64 64`)
  - ⚠️ NUNCA uses el mismo valor del código local

- [ ] **JWT_EXPIRATION**
  - Valor: `86400000` (24 horas en milisegundos)

- [ ] **SPRING_PROFILES_ACTIVE**
  - Valor: `prod`

- [ ] **PORT** (opcional)
  - Valor: `8080`
  - Render lo asigna automáticamente, pero Spring Boot usa 8080 internamente

### Deploy
- [ ] Click en "Create Web Service"
- [ ] Esperar build (5-10 minutos aprox.)
- [ ] Verificar que no haya errores en los logs

---

## ✅ Verificación Post-Despliegue

### Healthcheck
- [ ] Probar endpoint de salud:
  ```bash
  curl https://tu-app.onrender.com/api/actuator/health
  ```
  Debe devolver: `{"status":"UP"}`

### Endpoints de Autenticación
- [ ] Probar registro:
  ```bash
  curl -X POST https://tu-app.onrender.com/api/auth/register \
    -H "Content-Type: application/json" \
    -d '{"username":"test","email":"test@test.com","password":"test123"}'
  ```
  Debe devolver: token JWT

- [ ] Probar login:
  ```bash
  curl -X POST https://tu-app.onrender.com/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username":"test","password":"test123"}'
  ```
  Debe devolver: token JWT

### Endpoints de Productos
- [ ] Listar productos (con token):
  ```bash
  curl -H "Authorization: Bearer <TOKEN>" \
    https://tu-app.onrender.com/api/products
  ```
  Debe devolver: array de productos (puede estar vacío)

- [ ] Crear producto (con token):
  ```bash
  curl -X POST https://tu-app.onrender.com/api/products \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer <TOKEN>" \
    -d '{
      "nombre":"Mesa de Prueba",
      "descripcion":"Producto de prueba",
      "precio":100000,
      "categoria":"mesas",
      "stock":10
    }'
  ```
  Debe devolver: producto creado con ID

### Logs
- [ ] Revisar logs en Render Dashboard
- [ ] Verificar que no hay errores críticos
- [ ] Verificar que Spring Boot inició correctamente
- [ ] Verificar conexión exitosa a MongoDB

### Performance
- [ ] Verificar tiempo de respuesta aceptable (<2s)
- [ ] Probar con múltiples requests concurrentes
- [ ] Verificar que no hay memory leaks (en Metrics)

---

## 🌐 Configuración del Frontend

### Actualizar API URL
- [ ] En tu frontend, actualizar la URL de la API:
  ```typescript
  // src/services/api.ts
  const API_URL = 'https://tu-app.onrender.com/api';
  ```
  O usar variable de entorno:
  ```typescript
  const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';
  ```

### Configurar CORS (si es necesario)
- [ ] Si frontend está en otro dominio, agregar en `WebConfig.java`:
  ```java
  .allowedOrigins(
    "http://localhost:3000",
    "https://tu-frontend.netlify.app",  // ⬅️ Agregar aquí
    "https://tu-frontend.vercel.app"
  )
  ```
- [ ] Re-deployar backend después de cambio de CORS

### Desplegar Frontend
- [ ] Deployar frontend en Netlify/Vercel/etc
- [ ] Probar integración completa frontend-backend
- [ ] Verificar que login funciona
- [ ] Verificar que CRUD de productos funciona

---

## 🔧 Configuración Adicional (Opcional)

### Dominio Personalizado
- [ ] En Render: Settings → Custom Domain
- [ ] Agregar tu dominio
- [ ] Configurar DNS según instrucciones de Render
- [ ] Esperar propagación DNS (puede tardar hasta 48h)
- [ ] Verificar SSL/TLS automático (Let's Encrypt)

### Monitoring
- [ ] Configurar Sentry para error tracking
- [ ] Configurar Datadog/New Relic para APM (opcional)
- [ ] Configurar alertas por email/Slack
- [ ] Configurar uptime monitoring (UptimeRobot, Pingdom, etc)

### Backups
- [ ] Configurar backups automáticos en MongoDB Atlas
- [ ] Probar restore desde backup
- [ ] Documentar proceso de backup/restore

### CI/CD (Opcional)
- [ ] Configurar GitHub Actions para tests automáticos
- [ ] Configurar auto-deploy en merge a main
- [ ] Configurar environment variables en GitHub Secrets

---

## 📊 Monitoreo Post-Deploy

### Primera Semana
- [ ] Día 1: Revisar logs cada hora
- [ ] Día 2-3: Revisar logs 2-3 veces al día
- [ ] Día 4-7: Revisar logs 1 vez al día
- [ ] Verificar métricas de CPU/Memory en Render
- [ ] Verificar que no hay errores 500
- [ ] Verificar tiempos de respuesta

### Primera Mes
- [ ] Revisar logs semanalmente
- [ ] Revisar métricas de uso
- [ ] Optimizar si es necesario (escalar, optimizar queries, etc)
- [ ] Revisar costos (si no es free tier)

---

## 🐛 Troubleshooting

### Build Falla
- [ ] Revisar logs de build en Render
- [ ] Verificar que Dockerfile es correcto
- [ ] Verificar que todas las dependencias en pom.xml son resolvibles
- [ ] Probar build localmente: `docker build -t test .`

### Deploy Falla
- [ ] Revisar logs de deploy
- [ ] Verificar variables de entorno configuradas
- [ ] Verificar MONGODB_URL es válida
- [ ] Probar conexión a MongoDB desde tu máquina

### App No Responde
- [ ] Verificar en Render Dashboard que el servicio está "Running"
- [ ] Revisar logs para errores
- [ ] Verificar healthcheck: `/api/actuator/health`
- [ ] Reiniciar servicio manualmente si es necesario

### MongoDB Connection Error
- [ ] Verificar Network Access en MongoDB Atlas (0.0.0.0/0)
- [ ] Verificar usuario/password correctos
- [ ] Verificar URI bien formada
- [ ] Probar conexión desde mongosh

### JWT Errors
- [ ] Verificar que JWT_SECRET está configurado
- [ ] Verificar que JWT_SECRET tiene mínimo 256 bits
- [ ] Regenerar JWT_SECRET si es necesario
- [ ] Re-deployar después de cambiar JWT_SECRET

---

## 📈 Scaling

### Cuándo Escalar Verticalmente (más recursos)
- CPU constantemente >70%
- Memory constantemente >80%
- Tiempos de respuesta >2 segundos
- Errores de timeout frecuentes

### Plan de Escalamiento
1. **Free → Starter ($7/mes)**
   - 512 MB RAM, 0.5 CPU
   - Siempre activo (no se duerme)
   - Bueno para 100-1000 usuarios

2. **Starter → Standard ($25/mes)**
   - 2 GB RAM, 1 CPU
   - Mejor performance
   - Bueno para 1000-10000 usuarios

3. **Horizontal Scaling (Pro+)**
   - Múltiples instancias
   - Load balancing automático
   - Para >10000 usuarios

---

## 💰 Costos Estimados

### Render
- **Free**: $0/mes (se duerme después de 15 min)
- **Starter**: $7/mes (siempre activo)
- **Standard**: $25/mes (más recursos)
- **Pro**: $85/mes (múltiples instancias)

### MongoDB Atlas
- **M0 (Free)**: $0/mes (512 MB storage, shared)
- **M10**: $0.08/hora (~$57/mes) (2 GB RAM, 10 GB storage)
- **M20**: $0.20/hora (~$146/mes) (4 GB RAM, 20 GB storage)

### Total Mensual (Estimado)
- **Desarrollo/Hobby**: $0 (Free tier ambos)
- **Pequeño negocio**: $7-15/mes (Starter + Free MongoDB)
- **Mediano negocio**: $64-82/mes (Standard + M10)
- **Grande**: $142+/mes (Pro + M20+)

---

## ✅ Checklist Final

Marca cuando todo esté completado:

- [ ] Backend deployado en Render
- [ ] MongoDB Atlas configurado y conectado
- [ ] Todas las variables de entorno configuradas
- [ ] Healthcheck pasando
- [ ] Endpoints de auth funcionando
- [ ] Endpoints de productos funcionando
- [ ] Frontend desplegado
- [ ] Frontend conectado al backend
- [ ] CORS configurado correctamente
- [ ] SSL/TLS funcionando (https)
- [ ] Logs revisados sin errores críticos
- [ ] Performance aceptable
- [ ] Monitoring configurado
- [ ] Backups configurados
- [ ] Documentación actualizada

---

## 🎉 ¡Felicitaciones!

Si marcaste todo, tu aplicación está en producción y lista para usuarios reales.

### Próximos Pasos Sugeridos:
1. ✅ Configurar Google Analytics en frontend
2. ✅ Implementar rate limiting en backend
3. ✅ Agregar más tests automatizados
4. ✅ Configurar pipeline CI/CD completo
5. ✅ Implementar feature flags
6. ✅ Agregar documentación de API (Swagger/OpenAPI)

---

**Guarda este checklist y úsalo en futuros deploys** 🚀
