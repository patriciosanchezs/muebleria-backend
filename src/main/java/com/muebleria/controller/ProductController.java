package com.muebleria.controller;

import com.muebleria.dto.ProductRequest;
import com.muebleria.dto.ProductResponse;
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
     * Obtener todos los productos filtrados por local
     * @param local Local para filtrar productos (QUILLOTA, COQUIMBO, MUEBLES_SANCHEZ) - REQUERIDO
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN_LOCAL', 'ENCARGADO_LOCAL', 'VENDEDOR', 'VENDEDOR_SIN_COMISION', 'BODEGUERO')")
    public ResponseEntity<List<ProductResponse>> getAllProducts(@RequestParam String local) {
        return ResponseEntity.ok(productService.getAllProductsByLocal(local));
    }
    
    /**
     * Obtener producto por ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN_LOCAL', 'ENCARGADO_LOCAL', 'VENDEDOR', 'VENDEDOR_SIN_COMISION', 'BODEGUERO')")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable String id) {
        return ResponseEntity.ok(productService.getProductResponseById(id));
    }
    
    /**
     * Obtener productos por categoría
     * @param local Local para filtrar productos - REQUERIDO
     */
    @GetMapping("/categoria/{categoria}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN_LOCAL', 'ENCARGADO_LOCAL', 'VENDEDOR', 'VENDEDOR_SIN_COMISION', 'BODEGUERO')")
    public ResponseEntity<List<ProductResponse>> getProductsByCategoria(@PathVariable String categoria, @RequestParam String local) {
        return ResponseEntity.ok(productService.getProductsByCategoria(categoria, local));
    }
    
    /**
     * Buscar productos por nombre
     * @param local Local para filtrar productos - REQUERIDO
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN_LOCAL', 'ENCARGADO_LOCAL', 'VENDEDOR', 'VENDEDOR_SIN_COMISION', 'BODEGUERO')")
    public ResponseEntity<List<ProductResponse>> searchProducts(@RequestParam String nombre, @RequestParam String local) {
        return ResponseEntity.ok(productService.searchProductsByNombre(nombre, local));
    }
    
    /**
     * Obtener stock de un producto
     */
    @GetMapping("/{id}/stock/{local}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN_LOCAL', 'ENCARGADO_LOCAL', 'VENDEDOR', 'VENDEDOR_SIN_COMISION', 'BODEGUERO')")
    public ResponseEntity<Map<String, Integer>> getStockByLocal(@PathVariable String id,
                                                                  @PathVariable String local) {
        Integer stock = productService.getStockByLocal(id, local);
        Map<String, Integer> response = new HashMap<>();
        response.put("stock", stock);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Crear producto (ADMINISTRADOR, ADMIN_LOCAL, ENCARGADO_LOCAL o BODEGUERO)
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN_LOCAL', 'ENCARGADO_LOCAL', 'BODEGUERO')")
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        ProductResponse product = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(product);
    }
    
    /**
     * Actualizar producto (ADMINISTRADOR, ADMIN_LOCAL, ENCARGADO_LOCAL o BODEGUERO)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN_LOCAL', 'ENCARGADO_LOCAL', 'BODEGUERO')")
    public ResponseEntity<ProductResponse> updateProduct(@PathVariable String id, 
                                                  @Valid @RequestBody ProductRequest request) {
        ProductResponse product = productService.updateProduct(id, request);
        return ResponseEntity.ok(product);
    }
    
    /**
     * Eliminar producto (ADMINISTRADOR, ADMIN_LOCAL, ENCARGADO_LOCAL o BODEGUERO)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN_LOCAL', 'ENCARGADO_LOCAL', 'BODEGUERO')")
    public ResponseEntity<Void> deleteProduct(@PathVariable String id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
