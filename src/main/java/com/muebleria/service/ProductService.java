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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {
    
    private final ProductRepository productRepository;
    private final AuthHelper authHelper;
    
    public List<ProductResponse> getAllProducts(org.springframework.security.core.Authentication authentication) {
        return productRepository.findAll().stream()
                .map(product -> convertToResponse(product, authentication))
                .collect(Collectors.toList());
    }
    
    public Product getProductById(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + id));
    }
    
    public ProductResponse getProductResponseById(String id, org.springframework.security.core.Authentication authentication) {
        return convertToResponse(getProductById(id), authentication);
    }
    
    public List<ProductResponse> getProductsByCategoria(String categoria, org.springframework.security.core.Authentication authentication) {
        return productRepository.findByCategoria(categoria).stream()
                .map(product -> convertToResponse(product, authentication))
                .collect(Collectors.toList());
    }
    
    public List<ProductResponse> searchProductsByNombre(String nombre, org.springframework.security.core.Authentication authentication) {
        return productRepository.findByNombreContainingIgnoreCase(nombre).stream()
                .map(product -> convertToResponse(product, authentication))
                .collect(Collectors.toList());
    }
    
    public ProductResponse createProduct(ProductRequest request, org.springframework.security.core.Authentication authentication) {
        // Validar que todos los stocks sean >= 0
        validateStockPorLocal(request.getStockPorLocal());
        
        // Convertir Map<String, Integer> a Map<Local, Integer>
        Map<Local, Integer> stockMap = convertStockToLocalMap(request.getStockPorLocal());
        
        Product product = Product.builder()
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .precio(request.getPrecio())
                .categoria(request.getCategoria())
                .stockPorLocal(stockMap)
                .imageUrl(request.getImageUrl())
                .fechaCreacion(LocalDateTime.now())
                .disponible(true)
                .build();
        
        Product saved = productRepository.save(product);
        return convertToResponse(saved, authentication);
    }
    
    public ProductResponse updateProduct(String id, ProductRequest request, org.springframework.security.core.Authentication authentication) {
        Product existingProduct = getProductById(id);
        
        // Validar que todos los stocks sean >= 0
        validateStockPorLocal(request.getStockPorLocal());
        
        // Convertir Map<String, Integer> a Map<Local, Integer>
        Map<Local, Integer> stockMap = convertStockToLocalMap(request.getStockPorLocal());
        
        Product updatedProduct = Product.builder()
                .id(existingProduct.getId())
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .precio(request.getPrecio())
                .categoria(request.getCategoria())
                .stockPorLocal(stockMap)
                .imageUrl(request.getImageUrl())
                .fechaCreacion(existingProduct.getFechaCreacion())
                .fechaActualizacion(LocalDateTime.now())
                .disponible(existingProduct.getStockTotal() > 0)
                .build();
        
        Product saved = productRepository.save(updatedProduct);
        return convertToResponse(saved, authentication);
    }
    
    public void deleteProduct(String id) {
        Product product = getProductById(id);
        productRepository.delete(product);
    }
    
    /**
     * Obtiene el stock de un producto en un local específico
     */
    public Integer getStockByLocal(String productId, Local local) {
        Product product = getProductById(productId);
        return product.getStock(local);
    }
    
    /**
     * Descuenta stock de un producto en un local específico. Útil para ventas.
     */
    public void decreaseStock(String productId, Local local, Integer cantidad) {
        Product product = getProductById(productId);
        
        Integer stockActual = product.getStock(local);
        if (stockActual < cantidad) {
            throw new BadRequestException(
                String.format("Stock insuficiente para el producto '%s' en %s. Disponible: %d, Requerido: %d",
                    product.getNombre(), local.name(), stockActual, cantidad)
            );
        }
        
        product.reducirStock(local, cantidad);
        product.setFechaActualizacion(LocalDateTime.now());
        
        // Si el stock total llega a 0, marcar como no disponible
        if (product.getStockTotal() == 0) {
            product.setDisponible(false);
        }
        
        productRepository.save(product);
    }
    
    /**
     * Valida si hay stock disponible para un producto en un local específico.
     */
    public boolean hasStock(String productId, Local local, Integer cantidad) {
        Product product = getProductById(productId);
        return product.getStock(local) >= cantidad && product.isDisponible();
    }
    
    // Helper methods
    private void validateStockPorLocal(Map<String, Integer> stockPorLocal) {
        if (stockPorLocal == null || stockPorLocal.isEmpty()) {
            throw new BadRequestException("Debe especificar stock para al menos un local");
        }
        
        for (Map.Entry<String, Integer> entry : stockPorLocal.entrySet()) {
            if (entry.getValue() < 0) {
                throw new BadRequestException("El stock no puede ser negativo para el local: " + entry.getKey());
            }
        }
    }
    
    private Map<Local, Integer> convertStockToLocalMap(Map<String, Integer> stockPorLocal) {
        Map<Local, Integer> result = new HashMap<>();
        for (Map.Entry<String, Integer> entry : stockPorLocal.entrySet()) {
            try {
                Local local = Local.valueOf(entry.getKey());
                result.put(local, entry.getValue());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Local inválido: " + entry.getKey());
            }
        }
        return result;
    }
    
    private ProductResponse convertToResponse(Product product, org.springframework.security.core.Authentication authentication) {
        // Obtener locales autorizados del usuario
        List<Local> authorizedLocales = authHelper.getAuthorizedLocales(authentication);
        
        // Convertir Map<Local, Integer> a Map<String, Integer>
        // Filtrado por locales autorizados
        Map<String, Integer> stockMap = new HashMap<>();
        Integer stockTotal = 0;
        
        if (product.getStockPorLocal() != null) {
            for (Map.Entry<Local, Integer> entry : product.getStockPorLocal().entrySet()) {
                // Solo incluir stock de locales autorizados
                if (authorizedLocales.contains(entry.getKey())) {
                    stockMap.put(entry.getKey().name(), entry.getValue());
                    stockTotal += entry.getValue();
                }
            }
        }
        
        return ProductResponse.builder()
                .id(product.getId())
                .nombre(product.getNombre())
                .descripcion(product.getDescripcion())
                .precio(product.getPrecio())
                .categoria(product.getCategoria())
                .stockPorLocal(stockMap)
                .stockTotal(stockTotal) // Stock total solo de locales autorizados
                .imageUrl(product.getImageUrl())
                .fechaCreacion(product.getFechaCreacion())
                .fechaActualizacion(product.getFechaActualizacion())
                .disponible(product.isDisponible())
                .build();
    }
}
