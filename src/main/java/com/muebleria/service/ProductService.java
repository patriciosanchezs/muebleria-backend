package com.muebleria.service;

import com.muebleria.dto.ProductRequest;
import com.muebleria.dto.ProductResponse;
import com.muebleria.exception.BadRequestException;
import com.muebleria.exception.ResourceNotFoundException;
import com.muebleria.model.Local;
import com.muebleria.model.Product;
import com.muebleria.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {
    
    private final ProductRepository productRepository;
    
    /**
     * Obtiene productos filtrados por local.
     * @param local Local para filtrar productos (requerido)
     */
    public List<ProductResponse> getAllProductsByLocal(String local) {
        if (local == null || local.isEmpty()) {
            throw new BadRequestException("El parámetro 'local' es requerido");
        }
        
        Local localEnum;
        try {
            localEnum = Local.valueOf(local);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Local inválido: " + local);
        }
        
        return productRepository.findAll().stream()
                .filter(p -> localEnum.equals(p.getLocal()))
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
        
        Local localEnum;
        try {
            localEnum = Local.valueOf(local);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Local inválido: " + local);
        }
        
        return productRepository.findByCategoria(categoria).stream()
                .filter(p -> localEnum.equals(p.getLocal()))
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    public List<ProductResponse> searchProductsByNombre(String nombre, String local) {
        if (local == null || local.isEmpty()) {
            throw new BadRequestException("El parámetro 'local' es requerido");
        }
        
        Local localEnum;
        try {
            localEnum = Local.valueOf(local);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Local inválido: " + local);
        }
        
        return productRepository.findByNombreContainingIgnoreCase(nombre).stream()
                .filter(p -> localEnum.equals(p.getLocal()))
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    public ProductResponse createProduct(ProductRequest request) {
        // Validar local
        Local local;
        try {
            local = Local.valueOf(request.getLocal());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Local inválido: " + request.getLocal());
        }
        
        Product product = Product.builder()
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .precio(request.getPrecio())
                .categoria(request.getCategoria())
                .local(local)
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
        Local local;
        try {
            local = Local.valueOf(request.getLocal());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Local inválido: " + request.getLocal());
        }
        
        Product updatedProduct = Product.builder()
                .id(existingProduct.getId())
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .precio(request.getPrecio())
                .categoria(request.getCategoria())
                .local(local)
                .stock(request.getStock())
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
    public Integer getStockByLocal(String productId, Local local) {
        Product product = getProductById(productId);
        
        // Validar que el producto pertenece al local solicitado
        if (!local.equals(product.getLocal())) {
            throw new BadRequestException(
                String.format("El producto '%s' no pertenece al local %s", 
                    product.getNombre(), local.name())
            );
        }
        
        return product.getStock();
    }
    
    /**
     * Descuenta stock de un producto. Útil para ventas.
     */
    public void decreaseStock(String productId, Local local, Integer cantidad) {
        Product product = getProductById(productId);
        
        // Validar que el producto pertenece al local
        if (!local.equals(product.getLocal())) {
            throw new BadRequestException(
                String.format("El producto '%s' no pertenece al local %s", 
                    product.getNombre(), local.name())
            );
        }
        
        Integer stockActual = product.getStock();
        if (stockActual < cantidad) {
            throw new BadRequestException(
                String.format("Stock insuficiente para el producto '%s' en %s. Disponible: %d, Requerido: %d",
                    product.getNombre(), local.name(), stockActual, cantidad)
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
    public void increaseStock(String productId, Local local, Integer cantidad) {
        Product product = getProductById(productId);
        
        // Validar que el producto pertenece al local
        if (!local.equals(product.getLocal())) {
            throw new BadRequestException(
                String.format("El producto '%s' no pertenece al local %s", 
                    product.getNombre(), local.name())
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
     * Valida si hay stock disponible para un producto.
     */
    public boolean hasStock(String productId, Local local, Integer cantidad) {
        Product product = getProductById(productId);
        
        // Validar que el producto pertenece al local
        if (!local.equals(product.getLocal())) {
            return false;
        }
        
        return product.getStock() >= cantidad && product.isDisponible();
    }
    
    // Helper methods
    private ProductResponse convertToResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .nombre(product.getNombre())
                .descripcion(product.getDescripcion())
                .precio(product.getPrecio())
                .categoria(product.getCategoria())
                .local(product.getLocal() != null ? product.getLocal().name() : null)
                .stock(product.getStock())
                .imageUrl(product.getImageUrl())
                .fechaCreacion(product.getFechaCreacion())
                .fechaActualizacion(product.getFechaActualizacion())
                .disponible(product.isDisponible())
                .build();
    }
}
