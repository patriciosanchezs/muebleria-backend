# Script para probar Docker localmente antes de desplegar

# 1. Build de la imagen
echo "Building Docker image..."
docker build -t furniture-store-backend .

# 2. Ejecutar contenedor con variables de entorno
echo "Starting container..."
docker run -d \
  --name furniture-backend \
  -p 8080:8080 \
  -e MONGODB_URL="mongodb+srv://tu-usuario:tu-password@cluster.mongodb.net/furniture_store" \
  -e JWT_SECRET="tu-secret-aqui-debe-ser-largo-y-seguro" \
  -e JWT_EXPIRATION="86400000" \
  -e SPRING_PROFILES_ACTIVE="prod" \
  furniture-store-backend

# 3. Ver logs
echo "Viewing logs (Ctrl+C to stop)..."
docker logs -f furniture-backend

# Para detener y eliminar el contenedor:
# docker stop furniture-backend
# docker rm furniture-backend

# Para eliminar la imagen:
# docker rmi furniture-store-backend
