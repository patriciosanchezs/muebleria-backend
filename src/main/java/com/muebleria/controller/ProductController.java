package com.muebleria.controller;

import com.muebleria.dto.ProductRequest;
import com.muebleria.dto.ProductResponse;
import com.muebleria.model.Local;
import com.muebleria.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {
    
    private final ProductService productService;
    
    /**
     * Obtener todos los productos (ADMINISTRADOR, ADMIN_LOCAL y VENDEDOR)
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN_LOCAL', 'VENDEDOR')")
    public ResponseEntity<List<ProductResponse>> getAllProducts(org.springframework.security.core.Authentication authentication) {
        return ResponseEntity.ok(productService.getAllProducts(authentication));
    }
    
    /**
     * Obtener producto por ID (ADMINISTRADOR, ADMIN_LOCAL y VENDEDOR)
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN_LOCAL', 'VENDEDOR')")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable String id, org.springframework.security.core.Authentication authentication) {
        return ResponseEntity.ok(productService.getProductResponseById(id, authentication));
    }
    
    /**
     * Obtener productos por categoría (ADMINISTRADOR, ADMIN_LOCAL y VENDEDOR)
     */
    @GetMapping("/categoria/{categoria}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN_LOCAL', 'VENDEDOR')")
    public ResponseEntity<List<ProductResponse>> getProductsByCategoria(@PathVariable String categoria, org.springframework.security.core.Authentication authentication) {
        return ResponseEntity.ok(productService.getProductsByCategoria(categoria, authentication));
    }
    
    /**
     * Buscar productos por nombre (ADMINISTRADOR, ADMIN_LOCAL y VENDEDOR)
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN_LOCAL', 'VENDEDOR')")
    public ResponseEntity<List<ProductResponse>> searchProducts(@RequestParam String nombre, org.springframework.security.core.Authentication authentication) {
        return ResponseEntity.ok(productService.searchProductsByNombre(nombre, authentication));
    }
    
    /**
     * Obtener stock de un producto en un local específico
     */
    @GetMapping("/{id}/stock/{local}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN_LOCAL', 'VENDEDOR')")
    public ResponseEntity<Map<String, Integer>> getStockByLocal(@PathVariable String id, 
                                                                  @PathVariable String local) {
        Local localEnum = Local.valueOf(local);
        Integer stock = productService.getStockByLocal(id, localEnum);
        Map<String, Integer> response = new HashMap<>();
        response.put("stock", stock);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Crear producto (ADMINISTRADOR o ADMIN_LOCAL)
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN_LOCAL')")
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request, org.springframework.security.core.Authentication authentication) {
        ProductResponse product = productService.createProduct(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(product);
    }
    
    /**
     * Actualizar producto (ADMINISTRADOR o ADMIN_LOCAL)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN_LOCAL')")
    public ResponseEntity<ProductResponse> updateProduct(@PathVariable String id, 
                                                  @Valid @RequestBody ProductRequest request,
                                                  org.springframework.security.core.Authentication authentication) {
        ProductResponse product = productService.updateProduct(id, request, authentication);
        return ResponseEntity.ok(product);
    }
    
    /**
     * Eliminar producto (ADMINISTRADOR o ADMIN_LOCAL)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN_LOCAL')")
    public ResponseEntity<Void> deleteProduct(@PathVariable String id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
