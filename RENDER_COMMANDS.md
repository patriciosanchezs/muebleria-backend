# 🛠️ Comandos Útiles para Render

## Verificación Post-Despliegue

### Healthcheck
```bash
# Debe devolver: {"status":"UP"}
curl https://tu-app.onrender.com/api/actuator/health

# Con detalles (si está configurado)
curl https://tu-app.onrender.com/api/actuator/health/liveness
curl https://tu-app.onrender.com/api/actuator/health/readiness
```

### Info de la Aplicación
```bash
curl https://tu-app.onrender.com/api/actuator/info
```

---

## Pruebas de Endpoints

### Registrar Usuario
```bash
curl -X POST https://tu-app.onrender.com/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "Test123!"
  }'
```

### Login
```bash
curl -X POST https://tu-app.onrender.com/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "Test123!"
  }'
```

### Crear Producto (con token)
```bash
# Reemplaza <TOKEN> con el token obtenido del login
curl -X POST https://tu-app.onrender.com/api/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{
    "nombre": "Mesa de Comedor",
    "descripcion": "Mesa de madera para 6 personas",
    "precio": 150000,
    "categoria": "mesas",
    "stock": 10,
    "imageUrl": "https://example.com/mesa.jpg"
  }'
```

### Listar Productos
```bash
curl -H "Authorization: Bearer <TOKEN>" \
  https://tu-app.onrender.com/api/products
```

---

## Debug y Troubleshooting

### Ver Logs en Vivo (desde CLI de Render)

Primero instala Render CLI:
```bash
# macOS
brew install render

# Linux/Windows (con npm)
npm install -g @renderinc/cli

# Login
render login
```

Ver logs:
```bash
# Listar servicios
render services list

# Ver logs en tiempo real
render logs -s <service-id>

# Ver últimas 100 líneas
render logs -s <service-id> --tail 100
```

### Verificar Variables de Entorno

Desde el dashboard de Render:
1. Ir a tu servicio
2. Environment → Environment Variables
3. Verificar que todas estén configuradas

### Test de Conectividad a MongoDB

```bash
# Desde tu máquina local, probar si puedes conectarte a MongoDB
mongosh "mongodb+srv://user:pass@cluster.mongodb.net/furniture_store"

# Si falla, verifica:
# 1. Network Access en MongoDB Atlas
# 2. Usuario y contraseña correctos
# 3. URI bien formada
```

---

## Comandos de Gestión en Render CLI

### Desplegar Manualmente
```bash
render deploy -s <service-id>
```

### Reiniciar Servicio
```bash
render restart -s <service-id>
```

### Ver Detalles del Servicio
```bash
render services get <service-id>
```

### Ver Deploys Anteriores
```bash
render deploys list -s <service-id>
```

### Rollback a Deploy Anterior
```bash
render deploys rollback -s <service-id> -d <deploy-id>
```

---

## Monitoreo con cURL (Scripts)

### Script de Healthcheck (Linux/Mac)
```bash
#!/bin/bash
# healthcheck.sh

URL="https://tu-app.onrender.com/api/actuator/health"

while true; do
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" $URL)
  TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')
  
  if [ $STATUS -eq 200 ]; then
    echo "[$TIMESTAMP] ✅ Health: OK (200)"
  else
    echo "[$TIMESTAMP] ❌ Health: FAILED ($STATUS)"
  fi
  
  sleep 30
done
```

### Script de Healthcheck (PowerShell)
```powershell
# healthcheck.ps1

$url = "https://tu-app.onrender.com/api/actuator/health"

while ($true) {
    try {
        $response = Invoke-WebRequest -Uri $url -Method GET -UseBasicParsing
        $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
        
        if ($response.StatusCode -eq 200) {
            Write-Host "[$timestamp] ✅ Health: OK (200)" -ForegroundColor Green
        } else {
            Write-Host "[$timestamp] ❌ Health: FAILED ($($response.StatusCode))" -ForegroundColor Red
        }
    } catch {
        $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
        Write-Host "[$timestamp] ❌ Health: ERROR - $($_.Exception.Message)" -ForegroundColor Red
    }
    
    Start-Sleep -Seconds 30
}
```

---

## Performance Testing

### Test de Carga con Apache Bench
```bash
# 100 requests, 10 concurrent
ab -n 100 -c 10 https://tu-app.onrender.com/api/actuator/health

# POST request (necesitas un token válido)
ab -n 100 -c 10 -p data.json -T "application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  https://tu-app.onrender.com/api/products
```

### Test de Carga con hey
```bash
# Instalar
go install github.com/rakyll/hey@latest

# 200 requests, 50 concurrent, 10 segundos
hey -n 200 -c 50 -t 10 https://tu-app.onrender.com/api/actuator/health
```

---

## Configuración de Alertas

### Script de Alerta por Email (usando SendGrid)
```bash
#!/bin/bash
# alert.sh

URL="https://tu-app.onrender.com/api/actuator/health"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" $URL)

if [ $STATUS -ne 200 ]; then
  # Enviar alerta por email
  curl -X POST https://api.sendgrid.com/v3/mail/send \
    -H "Authorization: Bearer YOUR_SENDGRID_API_KEY" \
    -H "Content-Type: application/json" \
    -d '{
      "personalizations": [{
        "to": [{"email": "admin@example.com"}]
      }],
      "from": {"email": "alerts@example.com"},
      "subject": "🚨 App Down Alert",
      "content": [{
        "type": "text/plain",
        "value": "Furniture Store API is down! Status: '$STATUS'"
      }]
    }'
fi
```

---

## Integración con Frontend

### Actualizar URL de API en Frontend

**React/TypeScript:**
```typescript
// src/services/api.ts
const API_URL = import.meta.env.VITE_API_URL || 'https://furniture-store-api.onrender.com/api';
```

**Archivo .env.production:**
```env
VITE_API_URL=https://furniture-store-api.onrender.com/api
```

### Configurar CORS en Backend (si es necesario)

Si tu frontend está en otro dominio, asegúrate de agregarlo:

```java
// WebConfig.java
@Override
public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/**")
            .allowedOrigins(
                "http://localhost:3000",
                "http://localhost:5173",
                "https://tu-frontend.netlify.app",  // ⬅️ Agregar aquí
                "https://tu-frontend.vercel.app"
            )
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true);
}
```

---

## Backup y Restore

### Backup de MongoDB Atlas (Automático)

MongoDB Atlas hace backups automáticos, pero puedes forzar uno:

1. Ir a MongoDB Atlas Dashboard
2. Clusters → tu cluster → Backup
3. Take Snapshot Now

### Restore desde Snapshot
1. Backups → Find snapshot
2. Click en "..." → Restore
3. Seleccionar cluster de destino

### Backup Manual con mongodump
```bash
# Backup
mongodump --uri="mongodb+srv://user:pass@cluster.mongodb.net/furniture_store" \
  --out=./backup

# Restore
mongorestore --uri="mongodb+srv://user:pass@cluster.mongodb.net/furniture_store" \
  ./backup/furniture_store
```

---

## Scaling en Render

### Escalar Verticalmente (más recursos)
1. Render Dashboard → Tu servicio
2. Settings → Instance Type
3. Cambiar de Free → Starter → Standard, etc.

### Escalar Horizontalmente (más instancias)
1. Solo disponible en planes Pro y superiores
2. Settings → Scaling
3. Configurar número de instancias

---

## Útiles para Desarrollo

### Probar Localmente Antes de Push

```bash
# 1. Build local con Maven
mvn clean package

# 2. Ejecutar JAR local
java -jar target/*.jar \
  --spring.profiles.active=prod \
  --MONGODB_URL="mongodb+srv://..." \
  --JWT_SECRET="..."

# 3. Probar endpoints
curl http://localhost:8080/api/actuator/health
```

### Probar con Docker Local (simula Render)

```bash
# Build
docker build -t furniture-test .

# Run con variables de entorno de producción
docker run -p 8080:8080 \
  -e MONGODB_URL="mongodb+srv://..." \
  -e JWT_SECRET="$(openssl rand -base64 64)" \
  -e SPRING_PROFILES_ACTIVE="prod" \
  furniture-test

# Probar
curl http://localhost:8080/api/actuator/health
```

---

## Generadores de Datos de Prueba

### Crear Múltiples Productos de Prueba

```bash
#!/bin/bash
# seed-products.sh

TOKEN="<TU_TOKEN_AQUI>"
API_URL="https://tu-app.onrender.com/api"

# Array de productos
declare -a products=(
  '{"nombre":"Mesa de Roble","descripcion":"Mesa resistente","precio":180000,"categoria":"mesas","stock":5,"imageUrl":"https://example.com/1.jpg"}'
  '{"nombre":"Silla Ergonómica","descripcion":"Silla de oficina","precio":75000,"categoria":"sillas","stock":20,"imageUrl":"https://example.com/2.jpg"}'
  '{"nombre":"Sofá 3 Plazas","descripcion":"Sofá cómodo","precio":350000,"categoria":"sofas","stock":3,"imageUrl":"https://example.com/3.jpg"}'
)

for product in "${products[@]}"; do
  echo "Creating product: $product"
  curl -X POST "$API_URL/products" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $TOKEN" \
    -d "$product"
  echo ""
done
```

---

## 🎯 Checklist Post-Despliegue

- [ ] Healthcheck responde 200
- [ ] Login funciona
- [ ] Crear producto funciona
- [ ] Listar productos funciona
- [ ] Frontend puede conectarse al backend
- [ ] Logs no muestran errores críticos
- [ ] MongoDB está conectado correctamente
- [ ] CORS configurado para frontend
- [ ] Variables de entorno todas configuradas
- [ ] SSL/TLS funciona (https)
- [ ] Dominio personalizado (opcional)
- [ ] Monitoreo configurado (opcional)

---

## 📚 Referencias Rápidas

- **Render Dashboard**: https://dashboard.render.com
- **Render Docs**: https://render.com/docs
- **Spring Boot Actuator**: https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html
- **MongoDB Atlas**: https://cloud.mongodb.com
- **Docker Hub**: https://hub.docker.com/_/eclipse-temurin

---

**Tip**: Guarda este archivo en tus favoritos para referencia rápida 🔖
