package com.muebleria.service;

import com.muebleria.dto.ProductRequest;
import com.muebleria.dto.ProductResponse;
import com.muebleria.exception.BadRequestException;
import com.muebleria.exception.ResourceNotFoundException;
import com.muebleria.model.Product;
import com.muebleria.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {
    
    private final ProductRepository productRepository;
    private final LocalService localService;
    
    /**
     * Obtiene productos filtrados por local.
     * @param local ID del Local para filtrar productos (requerido)
     */
    public List<ProductResponse> getAllProductsByLocal(String local) {
        if (local == null || local.isEmpty()) {
            throw new BadRequestException("El parámetro 'local' es requerido");
        }
        
        // Validar que el local existe y está activo
        localService.validateActiveLocalId(local);
        
        return productRepository.findAll().stream()
                .filter(p -> local.equals(p.getLocalId()))
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    public Product getProductById(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + id));
    }
    
    public ProductResponse getProductResponseById(String id) {
        return convertToResponse(getProductById(id));
    }
    
    public List<ProductResponse> getProductsByCategoria(String categoria, String local) {
        if (local == null || local.isEmpty()) {
            throw new BadRequestException("El parámetro 'local' es requerido");
        }
        
        // Validar que el local existe y está activo
        localService.validateActiveLocalId(local);
        
        return productRepository.findByCategoria(categoria).stream()
                .filter(p -> local.equals(p.getLocalId()))
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    public List<ProductResponse> searchProductsByNombre(String nombre, String local) {
        if (local == null || local.isEmpty()) {
            throw new BadRequestException("El parámetro 'local' es requerido");
        }
        
        // Validar que el local existe y está activo
        localService.validateActiveLocalId(local);
        
        return productRepository.findByNombreContainingIgnoreCase(nombre).stream()
                .filter(p -> local.equals(p.getLocalId()))
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    public ProductResponse createProduct(ProductRequest request) {
        // Validar local
        localService.validateActiveLocalId(request.getLocal());
        
        Product product = Product.builder()
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .precio(request.getPrecio())
                .categoria(request.getCategoria())
                .localId(request.getLocal())
                .stock(request.getStock())
                .imageUrl(request.getImageUrl())
                .fechaCreacion(LocalDateTime.now())
                .disponible(true)
                .build();
        
        Product saved = productRepository.save(product);
        return convertToResponse(saved);
    }
    
    public ProductResponse updateProduct(String id, ProductRequest request) {
        Product existingProduct = getProductById(id);
        
        // Validar local
        localService.validateActiveLocalId(request.getLocal());
        
        Product updatedProduct = Product.builder()
                .id(existingProduct.getId())
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .precio(request.getPrecio())
                .categoria(request.getCategoria())
                .localId(request.getLocal())
                .stock(request.getStock())
                .stockReservado(existingProduct.getStockReservado())
                .imageUrl(request.getImageUrl())
                .fechaCreacion(existingProduct.getFechaCreacion())
                .fechaActualizacion(LocalDateTime.now())
                .disponible(request.getStock() > 0)
                .build();
        
        Product saved = productRepository.save(updatedProduct);
        return convertToResponse(saved);
    }
    
    public void deleteProduct(String id) {
        Product product = getProductById(id);
        productRepository.delete(product);
    }
    
    /**
     * Obtiene el stock de un producto (ya es específico por local)
     */
    public Integer getStockByLocal(String productId, String localId) {
        Product product = getProductById(productId);
        
        // Validar que el producto pertenece al local solicitado
        if (!localId.equals(product.getLocalId())) {
            throw new BadRequestException(
                String.format("El producto '%s' no pertenece al local con ID %s", 
                    product.getNombre(), localId)
            );
        }
        
        return product.getStock();
    }
    
    /**
     * Descuenta stock de un producto. Útil para ventas.
     */
    @Transactional
    public void decreaseStock(String productId, String localId, Integer cantidad) {
        Product product = getProductById(productId);
        
        // Validar que el producto pertenece al local
        if (!localId.equals(product.getLocalId())) {
            throw new BadRequestException(
                String.format("El producto '%s' no pertenece al local con ID %s", 
                    product.getNombre(), localId)
            );
        }
        
        Integer stockActual = product.getStock();
        if (stockActual < cantidad) {
            throw new BadRequestException(
                String.format("Stock insuficiente para el producto '%s' en local %s. Disponible: %d, Requerido: %d",
                    product.getNombre(), localId, stockActual, cantidad)
            );
        }
        
        product.setStock(stockActual - cantidad);
        product.setFechaActualizacion(LocalDateTime.now());
        
        // Si el stock llega a 0, marcar como no disponible
        if (product.getStock() == 0) {
            product.setDisponible(false);
        }
        
        productRepository.save(product);
    }
    
    /**
     * Incrementa el stock de un producto.
     * Se usa al rechazar ventas pendientes para devolver el stock.
     */
    @Transactional
    public void increaseStock(String productId, String localId, Integer cantidad) {
        Product product = getProductById(productId);
        
        // Validar que el producto pertenece al local
        if (!localId.equals(product.getLocalId())) {
            throw new BadRequestException(
                String.format("El producto '%s' no pertenece al local con ID %s", 
                    product.getNombre(), localId)
            );
        }
        
        product.setStock(product.getStock() + cantidad);
        product.setFechaActualizacion(LocalDateTime.now());
        
        // Si el producto estaba marcado como no disponible y ahora tiene stock, marcarlo como disponible
        if (!product.isDisponible() && product.getStock() > 0) {
            product.setDisponible(true);
        }
        
        productRepository.save(product);
    }
    
    /**
     * Reserva stock temporalmente para una venta en proceso.
     * Si esEncargo=true, no reserva stock (permite vender sin stock disponible).
     * @return true si la reserva fue exitosa, false si no hay stock suficiente
     */
    public boolean reservarStock(String productId, String localId, Integer cantidad, boolean esEncargo) {
        // Si es encargo, no es necesario reservar stock
        if (esEncargo) {
            return true;
        }
        
        Product product = getProductById(productId);
        
        if (!localId.equals(product.getLocalId())) {
            throw new BadRequestException(
                String.format("El producto '%s' no pertenece al local con ID %s", 
                    product.getNombre(), localId)
            );
        }
        
        int stockActual = product.getStock() != null ? product.getStock() : 0;
        int stockReservado = product.getStockReservado() != null ? product.getStockReservado() : 0;
        int stockDisponible = stockActual - stockReservado;
        
        if (stockDisponible < cantidad) {
            return false;
        }
        
        product.setStockReservado(stockReservado + cantidad);
        product.setReservaTimestamp(LocalDateTime.now());
        product.setFechaActualizacion(LocalDateTime.now());
        productRepository.save(product);
        
        return true;
    }
    
    /**
     * Reserva stock temporalmente para una venta en proceso.
     * @return true si la reserva fue exitosa, false si no hay stock suficiente
     * @deprecated Usar reservarStock(productId, localId, cantidad, esEncargo) en su lugar
     */
    public boolean reservarStock(String productId, String localId, Integer cantidad) {
        return reservarStock(productId, localId, cantidad, false);
    }
    
    /**
     * Libera stock reservado previamente.
     */
    public void liberarStock(String productId, String localId, Integer cantidad) {
        Product product = getProductById(productId);
        
        if (!localId.equals(product.getLocalId())) {
            throw new BadRequestException(
                String.format("El producto '%s' no pertenece al local con ID %s", 
                    product.getNombre(), localId)
            );
        }
        
        int stockReservado = product.getStockReservado() != null ? product.getStockReservado() : 0;
        if (stockReservado <= 0) {
            return;
        }
        
        int cantidadReal = Math.min(cantidad, stockReservado);
        int nuevoStockReservado = stockReservado - cantidadReal;
        product.setStockReservado(nuevoStockReservado);
        if (nuevoStockReservado == 0) {
            product.setReservaTimestamp(null);
        }
        product.setFechaActualizacion(LocalDateTime.now());
        productRepository.save(product);
    }
    
    /**
     * Confirma la reserva y deduce del stock real.
     */
    public void confirmarReserva(String productId, String localId, Integer cantidad) {
        Product product = getProductById(productId);
        
        if (!localId.equals(product.getLocalId())) {
            throw new BadRequestException(
                String.format("El producto '%s' no pertenece al local con ID %s", 
                    product.getNombre(), localId)
            );
        }
        
        int stockReservado = product.getStockReservado() != null ? product.getStockReservado() : 0;
        int stockActual = product.getStock() != null ? product.getStock() : 0;
        
        // Si hay reserva existente, usarla
        if (stockReservado > 0) {
            if (stockReservado < cantidad) {
                throw new BadRequestException(
                    String.format("No hay reserva suficiente. Reservado: %d, requerido: %d",
                        stockReservado, cantidad)
                );
            }
            
            if (stockActual < cantidad) {
                throw new BadRequestException(
                    String.format("Stock insuficiente. Stock actual: %d, requerido: %d",
                        stockActual, cantidad)
                );
            }
            
            // Liberar reserva y descontar stock
            int nuevoStockReservado = stockReservado - cantidad;
            product.setStockReservado(nuevoStockReservado);
            if (nuevoStockReservado == 0) {
                product.setReservaTimestamp(null);
            }
            product.setStock(stockActual - cantidad);
            product.setFechaActualizacion(LocalDateTime.now());
            
            if (product.getStock() == 0) {
                product.setDisponible(false);
            }
        } else {
            // No hay reserva - es un encargo o nunca se reservó
            // Solo descontar stock si hay suficiente
            if (stockActual < cantidad) {
                throw new BadRequestException(
                    String.format("Stock insuficiente. Stock actual: %d, requerido: %d",
                        stockActual, cantidad)
                );
            }
            
            // Solo descontar stock (sin tocar reserva)
            product.setStock(stockActual - cantidad);
            product.setFechaActualizacion(LocalDateTime.now());
            
            if (product.getStock() == 0) {
                product.setDisponible(false);
            }
        }
        
        productRepository.save(product);
    }
    
    /**
     * Cancela todas las reservas de un producto.
     */
    public void cancelarReservas(String productId, String localId) {
        Product product = getProductById(productId);
        
        if (!localId.equals(product.getLocalId())) {
            return;
        }
        
        product.setStockReservado(0);
        product.setReservaTimestamp(null);
        product.setFechaActualizacion(LocalDateTime.now());
        productRepository.save(product);
    }
    
    /**
     * Limpia reservas expiradas. Las reservas sin timestamp o con timestamp
     * anterior a 30 minutos son liberadas.
     */
    public void limpiarReservasExpiradas() {
        List<Product> expiredProducts = productRepository.findExpiredReservations(
            LocalDateTime.now().minusMinutes(30)
        );
        
        for (Product product : expiredProducts) {
            int stockReservado = product.getStockReservado() != null ? product.getStockReservado() : 0;
            if (stockReservado > 0) {
                product.setStockReservado(0);
                product.setReservaTimestamp(null);
                product.setFechaActualizacion(LocalDateTime.now());
                productRepository.save(product);
            }
        }
    }
    
    /**
     * Valida si hay stock disponible para un producto.
     */
    public boolean hasStock(String productId, String localId, Integer cantidad) {
        Product product = getProductById(productId);
        
        if (!localId.equals(product.getLocalId())) {
            return false;
        }
        
        return product.getStock() >= cantidad && product.isDisponible();
    }
    
    // Helper methods
    private ProductResponse convertToResponse(Product product) {
        int stockReservado = product.getStockReservado() != null ? product.getStockReservado() : 0;
        return ProductResponse.builder()
                .id(product.getId())
                .nombre(product.getNombre())
                .descripcion(product.getDescripcion())
                .precio(product.getPrecio())
                .categoria(product.getCategoria())
                .local(product.getLocalId())
                .stock(product.getStock())
                .stockReservado(stockReservado)
                .stockDisponible(product.getStockDisponible())
                .imageUrl(product.getImageUrl())
                .fechaCreacion(product.getFechaCreacion())
                .fechaActualizacion(product.getFechaActualizacion())
                .disponible(product.isDisponible())
                .build();
    }
}
