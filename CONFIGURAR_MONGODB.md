# 🔧 Configurar MongoDB - 2 Opciones

## ❌ ERROR ACTUAL:
```
Cannot resolve reference to bean 'mongoTemplate'
```

Esto significa que **Spring Boot no puede conectarse a MongoDB**.

---

## ✅ OPCIÓN 1: MongoDB Atlas (Cloud - Recomendado) ⭐

Ya tienes MongoDB Atlas configurado, pero necesitas **permitir tu IP**.

### 📋 Pasos:

1. **Ve a MongoDB Atlas**: https://cloud.mongodb.com/
   - Login con tu cuenta

2. **Network Access (Acceso de Red)**:
   - En el menú izquierdo, click en **"Network Access"**
   - Click en **"+ ADD IP ADDRESS"**
   - Opción 1: Click **"ADD CURRENT IP ADDRESS"** (tu IP actual)
   - Opción 2: Para desarrollo, usa **"ALLOW ACCESS FROM ANYWHERE"** (0.0.0.0/0)
   - Click **"Confirm"**

3. **Verifica tu Base de Datos**:
   - Click en **"Database"** en el menú izquierdo
   - Click en **"Browse Collections"**
   - Si no existe la base de datos `furniture_store`, créala:
     - Click **"+ Create Database"**
     - Database Name: `furniture_store`
     - Collection Name: `users`

4. **Ejecuta la aplicación**:
```bash
cd C:\Users\Patricio.Sanchez\Documents\Proyectos\psanchezs\forniture_store\backend
mvn spring-boot:run
```

**✅ VENTAJAS:**
- No necesitas instalar nada
- Funciona desde cualquier lugar
- Gratis hasta 512 MB
- Ya está configurado en `application.yml`

---

## 🏠 OPCIÓN 2: MongoDB Local

### 📥 Instalación MongoDB Community (Windows):

1. **Descarga MongoDB**:
   - https://www.mongodb.com/try/download/community
   - Selecciona: Windows, MSI
   - Click **"Download"**

2. **Instala MongoDB**:
   - Ejecuta el archivo `.msi`
   - Tipo de instalación: **Complete**
   - ✅ Marca: **"Install MongoDB as a Service"**
   - ✅ Marca: **"Run service as Network Service user"**
   - Desinstala MongoDB Compass si no lo necesitas (es una GUI)

3. **Verifica la instalación**:
```bash
# Verifica que MongoDB está corriendo
sc query MongoDB

# Debería decir: STATE: 4 RUNNING
```

4. **Usa el profile local**:
```bash
cd C:\Users\Patricio.Sanchez\Documents\Proyectos\psanchezs\forniture_store\backend

# Ejecuta con el perfil local
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

**✅ VENTAJAS:**
- Control total
- No depende de internet
- Más rápido

**❌ DESVENTAJAS:**
- Requiere instalación
- Ocupa espacio en disco
- Solo funciona en tu máquina

---

## 🎯 RECOMENDACIÓN:

### Para empezar rápido: **Usa MongoDB Atlas (Opción 1)**

Solo necesitas:
1. Ir a https://cloud.mongodb.com/
2. Network Access → Add IP Address → Allow from Anywhere
3. Ejecutar: `mvn spring-boot:run`

---

## 🚀 DESPUÉS DE CONFIGURAR:

### Ejecuta la aplicación:
```bash
cd C:\Users\Patricio.Sanchez\Documents\Proyectos\psanchezs\forniture_store\backend

# Con MongoDB Atlas (default):
mvn spring-boot:run

# Con MongoDB Local:
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### Prueba que funciona:
```bash
# En otro terminal, prueba el endpoint de health
curl http://localhost:8080/api/auth/test
```

---

## 📊 Verificar conexión a MongoDB Atlas:

Si quieres probar la conexión antes de ejecutar la app:

1. **Descarga MongoDB Compass** (GUI):
   - https://www.mongodb.com/try/download/compass

2. **Conecta con tu URI**:
```
mongodb+srv://psanchezforniture:gI7CmZ3KGUWwK2vo@forniturestore.dd6wgbf.mongodb.net/furniture_store?retryWrites=true&w=majority
```

3. Si se conecta, significa que tu configuración es correcta.

---

## 🐛 TROUBLESHOOTING:

### Error: "not authorized on admin to execute command"
- Ve a Database Access en MongoDB Atlas
- Verifica que el usuario tiene permisos de lectura/escritura

### Error: "connection timeout"
- Verifica Network Access
- Agrega tu IP o permite acceso desde cualquier lugar (0.0.0.0/0)

### Error: "Authentication failed"
- Verifica usuario y contraseña en application.yml
- La contraseña actual es: `gI7CmZ3KGUWwK2vo`

---

## 💡 CAMBIAR ENTRE LOCAL Y ATLAS:

Ya creé 2 archivos de configuración:

- `application.yml` → MongoDB Atlas (default)
- `application-local.yml` → MongoDB Local

Para usar local:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Para usar Atlas:
```bash
mvn spring-boot:run
```

---

## ✅ Siguiente paso:

**Dime cuál opción prefieres y te ayudo a ejecutarla:**

1. **MongoDB Atlas** → Solo necesito que configures el Network Access
2. **MongoDB Local** → Te guío en la instalación
