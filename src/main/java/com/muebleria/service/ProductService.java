package com.muebleria.service;

import com.muebleria.dto.ProductRequest;
import com.muebleria.exception.BadRequestException;
import com.muebleria.exception.ResourceNotFoundException;
import com.muebleria.model.Product;
import com.muebleria.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {
    
    private final ProductRepository productRepository;
    
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }
    
    public Product getProductById(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + id));
    }
    
    public List<Product> getProductsByCategoria(String categoria) {
        return productRepository.findByCategoria(categoria);
    }
    
    public List<Product> searchProductsByNombre(String nombre) {
        return productRepository.findByNombreContainingIgnoreCase(nombre);
    }
    
    public Product createProduct(ProductRequest request) {
        Product product = Product.builder()
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .precio(request.getPrecio())
                .categoria(request.getCategoria())
                .stock(request.getStock())
                .imageUrl(request.getImageUrl())
                .fechaCreacion(LocalDateTime.now())
                .disponible(true)
                .build();
        
        return productRepository.save(product);
    }
    
    public Product updateProduct(String id, ProductRequest request) {
        Product existingProduct = getProductById(id);
        
        Product updatedProduct = Product.builder()
                .id(existingProduct.getId())
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .precio(request.getPrecio())
                .categoria(request.getCategoria())
                .stock(request.getStock())
                .imageUrl(request.getImageUrl())
                .fechaCreacion(existingProduct.getFechaCreacion())
                .fechaActualizacion(LocalDateTime.now())
                .disponible(request.getStock() > 0)
                .build();
        
        return productRepository.save(updatedProduct);
    }
    
    public void deleteProduct(String id) {
        Product product = getProductById(id);
        productRepository.delete(product);
    }
    
    public Product updateStock(String id, Integer nuevoStock) {
        Product existingProduct = getProductById(id);
        
        Product updatedProduct = Product.builder()
                .id(existingProduct.getId())
                .nombre(existingProduct.getNombre())
                .descripcion(existingProduct.getDescripcion())
                .precio(existingProduct.getPrecio())
                .categoria(existingProduct.getCategoria())
                .stock(nuevoStock)
                .imageUrl(existingProduct.getImageUrl())
                .fechaCreacion(existingProduct.getFechaCreacion())
                .fechaActualizacion(LocalDateTime.now())
                .disponible(nuevoStock > 0)
                .build();
        
        return productRepository.save(updatedProduct);
    }
    
    /**
     * Descuenta stock de un producto. Útil para ventas.
     */
    public void decreaseStock(String productId, Integer cantidad) {
        Product product = getProductById(productId);
        
        if (product.getStock() < cantidad) {
            throw new BadRequestException(
                String.format("Stock insuficiente para el producto '%s'. Disponible: %d, Requerido: %d",
                    product.getNombre(), product.getStock(), cantidad)
            );
        }
        
        Integer nuevoStock = product.getStock() - cantidad;
        updateStock(productId, nuevoStock);
    }
    
    /**
     * Valida si hay stock disponible para un producto.
     */
    public boolean hasStock(String productId, Integer cantidad) {
        Product product = getProductById(productId);
        return product.getStock() >= cantidad && product.isDisponible();
    }
}
