# 🛋️ Furniture Store API - Sistema de Mueblería con JWT

API REST completa para gestión de mueblería con autenticación JWT y MongoDB.

## 🚀 Tecnologías

- **Java 21**
- **Spring Boot 3.2.1**
- **Spring Security 6** con JWT
- **Spring Data MongoDB**
- **Lombok**
- **Bean Validation**

## 📋 Requisitos Previos

- JDK 21 o superior
- Maven 3.6+
- MongoDB 4.4+ corriendo en `localhost:27017`

## 🔧 Instalación y Ejecución

### 1. Clonar o descargar el proyecto

```bash
cd C:\Users\Patricio.Sanchez\Documents\Proyectos\psanchezs\forniture_store\backend
```

### 2. Asegurarse de que MongoDB está corriendo

```bash
# En Windows, puedes iniciar MongoDB con:
net start MongoDB

# O ejecutar mongod directamente:
mongod
```

### 3. Compilar el proyecto

```bash
mvn clean install
```

### 4. Ejecutar la aplicación

```bash
mvn spring-boot:run
```

La aplicación estará disponible en: **`http://localhost:8080/api`**

## 📡 Endpoints de la API

### 🔐 Autenticación (Endpoints Públicos)

#### Registrar Nuevo Usuario

```http
POST http://localhost:8080/api/auth/register
Content-Type: application/json

{
  "username": "juan",
  "email": "juan@example.com",
  "password": "123456"
}
```

**Respuesta:**
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJqdWFuIiwiaWF0IjoxNjQwOTk1MjAwLCJleHAiOjE2NDA5OTg4MDB9...",
  "type": "Bearer",
  "username": "juan",
  "email": "juan@example.com",
  "roles": ["ROLE_USER"]
}
```

#### Iniciar Sesión

```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "username": "juan",
  "password": "123456"
}
```

**Respuesta:**
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "type": "Bearer",
  "username": "juan",
  "email": "juan@example.com",
  "roles": ["ROLE_USER"]
}
```

---

### 🛋️ Productos (Requieren Autenticación JWT)

**⚠️ IMPORTANTE:** Todos los endpoints de productos requieren el header de autorización:

```
Authorization: Bearer <tu-token-jwt>
```

#### Listar Todos los Productos

```http
GET http://localhost:8080/api/products
Authorization: Bearer <token>
```

#### Obtener Producto por ID

```http
GET http://localhost:8080/api/products/{id}
Authorization: Bearer <token>
```

#### Buscar Productos por Categoría

```http
GET http://localhost:8080/api/products/categoria/mesas
Authorization: Bearer <token>
```

#### Buscar Productos por Nombre

```http
GET http://localhost:8080/api/products/search?nombre=mesa
Authorization: Bearer <token>
```

#### Crear Nuevo Producto

```http
POST http://localhost:8080/api/products
Authorization: Bearer <token>
Content-Type: application/json

{
  "nombre": "Mesa de Comedor",
  "descripcion": "Mesa de madera maciza para 6 personas",
  "precio": 15000.00,
  "categoria": "mesas",
  "stock": 10,
  "imageUrl": "https://ejemplo.com/imagenes/mesa-comedor.jpg"
}
```

#### Actualizar Producto

```http
PUT http://localhost:8080/api/products/{id}
Authorization: Bearer <token>
Content-Type: application/json

{
  "nombre": "Mesa de Comedor Premium",
  "descripcion": "Mesa de madera maciza premium para 8 personas",
  "precio": 18000.00,
  "categoria": "mesas",
  "stock": 8,
  "imageUrl": "https://ejemplo.com/imagenes/mesa-premium.jpg"
}
```

#### Actualizar Solo el Stock

```http
PATCH http://localhost:8080/api/products/{id}/stock?stock=15
Authorization: Bearer <token>
```

#### Eliminar Producto

```http
DELETE http://localhost:8080/api/products/{id}
Authorization: Bearer <token>
```

---

## 🗄️ Modelo de Datos

### User (Usuario)

```json
{
  "id": "String (MongoDB ObjectId)",
  "username": "String (único)",
  "email": "String (único)",
  "password": "String (encriptado con BCrypt)",
  "roles": ["ROLE_USER"],
  "createdAt": "2024-04-14T10:30:00",
  "active": true
}
```

### Product (Producto)

```json
{
  "id": "String (MongoDB ObjectId)",
  "nombre": "Mesa de Comedor",
  "descripcion": "Mesa de madera maciza",
  "precio": 15000.00,
  "categoria": "mesas",
  "stock": 10,
  "imageUrl": "https://...",
  "fechaCreacion": "2024-04-14T10:30:00",
  "fechaActualizacion": "2024-04-14T15:45:00",
  "disponible": true
}
```

---

## 🧪 Pruebas con cURL

### 1. Registrar un Usuario

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"test\",\"email\":\"test@test.com\",\"password\":\"123456\"}"
```

### 2. Iniciar Sesión (Guardar el Token)

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"test\",\"password\":\"123456\"}"
```

**Guarda el token de la respuesta para los siguientes comandos**

### 3. Crear un Producto

```bash
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TU_TOKEN_AQUI" \
  -d "{\"nombre\":\"Silla Moderna\",\"descripcion\":\"Silla ergonómica de oficina\",\"precio\":5000,\"categoria\":\"sillas\",\"stock\":20}"
```

### 4. Listar Todos los Productos

```bash
curl -X GET http://localhost:8080/api/products \
  -H "Authorization: Bearer TU_TOKEN_AQUI"
```

### 5. Buscar por Categoría

```bash
curl -X GET http://localhost:8080/api/products/categoria/sillas \
  -H "Authorization: Bearer TU_TOKEN_AQUI"
```

### 6. Actualizar Stock de un Producto

```bash
curl -X PATCH "http://localhost:8080/api/products/{ID_DEL_PRODUCTO}/stock?stock=25" \
  -H "Authorization: Bearer TU_TOKEN_AQUI"
```

### 7. Eliminar un Producto

```bash
curl -X DELETE http://localhost:8080/api/products/{ID_DEL_PRODUCTO} \
  -H "Authorization: Bearer TU_TOKEN_AQUI"
```

---

## 🎯 Categorías Sugeridas

- `mesas`
- `sillas`
- `sofas`
- `camas`
- `armarios`
- `escritorios`
- `estanterias`
- `mesas-de-noche`
- `libreros`
- `comedores`

---

## 🔒 Seguridad

- ✅ Contraseñas encriptadas con **BCrypt**
- ✅ Tokens JWT con expiración de **24 horas**
- ✅ CORS configurado para `localhost:3000` y `localhost:4200`
- ✅ Sesiones **Stateless** (sin estado en el servidor)
- ✅ Validación de datos con **Bean Validation**

---

## 📦 Estructura del Proyecto

```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/muebleria/
│   │   │   ├── config/              # SecurityConfig
│   │   │   ├── controller/          # AuthController, ProductController
│   │   │   ├── dto/                 # LoginRequest, RegisterRequest, etc.
│   │   │   ├── exception/           # Manejo global de excepciones
│   │   │   ├── model/               # User, Product
│   │   │   ├── repository/          # UserRepository, ProductRepository
│   │   │   ├── security/            # JWT (Token Provider, Filter)
│   │   │   ├── service/             # AuthService, ProductService
│   │   │   └── FurnitureStoreApplication.java
│   │   └── resources/
│   │       └── application.yml      # Configuración
│   └── test/
└── pom.xml
```

---

## 🛠️ Configuración

El archivo `application.yml` contiene la configuración principal:

```yaml
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/furniture_store

server:
  port: 8080
  servlet:
    context-path: /api

jwt:
  secret: 5Jf8kM2pN9qR3sT6vW8xY1zA4bC7dE0fG3hJ5kL8mN1oP4qR7sT0uV3wX6yZ9aB2
  expiration: 86400000  # 24 horas
```

### Para usar MongoDB Atlas (Cloud)

Modifica el `application.yml`:

```yaml
spring:
  data:
    mongodb:
      uri: mongodb+srv://<username>:<password>@cluster.mongodb.net/furniture_store?retryWrites=true&w=majority
```

---

## 🐛 Solución de Problemas

### MongoDB no se conecta

```bash
# Verificar si MongoDB está corriendo
mongosh

# Si no está corriendo, iniciarlo:
net start MongoDB
```

### Error de compilación con Lombok

```bash
# Asegúrate de tener el plugin de Lombok instalado en tu IDE
# IntelliJ IDEA: File > Settings > Plugins > Buscar "Lombok"
# Eclipse: Descargar lombok.jar y ejecutarlo
```

### Puerto 8080 ya está en uso

Modifica el puerto en `application.yml`:

```yaml
server:
  port: 8081  # Cambiar a otro puerto
```

---

## 📝 Licencia

Este proyecto fue creado para fines educativos y de demostración.

---

## 👨‍💻 Autor

Patricio Sánchez - 2024

---

## ✅ ¡Proyecto Listo!

El proyecto está completamente funcional. Para comenzar:

1. Asegúrate de que MongoDB esté corriendo
2. Ejecuta `mvn spring-boot:run`
3. Prueba los endpoints con Postman, cURL, o tu herramienta favorita
4. Disfruta tu API de mueblería 🛋️

---

## 🐳 Docker

### Build y Ejecución con Docker

```bash
# Build de la imagen
docker build -t furniture-store-backend .

# Ejecutar contenedor
docker run -d \
  --name furniture-backend \
  -p 8080:8080 \
  -e MONGODB_URL="mongodb+srv://user:pass@cluster.mongodb.net/furniture_store" \
  -e JWT_SECRET="tu-secret-super-seguro" \
  -e SPRING_PROFILES_ACTIVE="prod" \
  furniture-store-backend

# Ver logs
docker logs -f furniture-backend

# Detener
docker stop furniture-backend
```

### Probar Localmente con Docker

Ejecuta el script incluido:

```bash
chmod +x docker-test.sh
./docker-test.sh
```

---

## ☁️ Despliegue en Render

Para desplegar esta aplicación en Render (PaaS gratuito), consulta la guía completa:

📄 **[DEPLOY_RENDER.md](./DEPLOY_RENDER.md)**

### Resumen Rápido:

1. **Push tu código a GitHub**
2. **Ir a [Render.com](https://render.com)** y crear un nuevo Web Service
3. **Conectar tu repositorio**
4. **Configurar**:
   - Environment: Docker
   - Root Directory: `backend`
5. **Agregar variables de entorno**:
   - `MONGODB_URL`: Tu URI de MongoDB Atlas
   - `JWT_SECRET`: Genera con `openssl rand -base64 64`
   - `SPRING_PROFILES_ACTIVE`: `prod`
6. **Deploy** - Render construirá automáticamente desde el Dockerfile

**Healthcheck automático**: `https://tu-app.onrender.com/api/actuator/health`

---

## 🌐 Configuración de CORS para Producción

Si tu frontend está en otro dominio, actualiza `WebConfig.java`:

```java
@Override
public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/**")
            .allowedOrigins(
                "http://localhost:3000",
                "http://localhost:5173", 
                "https://tu-frontend.netlify.app",  // Agregar tu dominio
                "https://tu-frontend.vercel.app"
            )
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true);
}
```

---

## 📚 Archivos de Configuración

- `Dockerfile` - Construcción multi-stage para producción
- `render.yaml` - Blueprint para despliegue automático en Render
- `application.yml` - Configuración para desarrollo local
- `application-prod.yml` - Configuración para producción
- `.dockerignore` - Archivos excluidos del build Docker
- `docker-test.sh` - Script para probar Docker localmente
- `DEPLOY_RENDER.md` - Guía completa de despliegue

---
