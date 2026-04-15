package com.muebleria.controller;

import com.muebleria.dto.ProductRequest;
import com.muebleria.model.Product;
import com.muebleria.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {
    
    private final ProductService productService;
    
    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable String id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }
    
    @GetMapping("/categoria/{categoria}")
    public ResponseEntity<List<Product>> getProductsByCategoria(@PathVariable String categoria) {
        return ResponseEntity.ok(productService.getProductsByCategoria(categoria));
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<Product>> searchProducts(@RequestParam String nombre) {
        return ResponseEntity.ok(productService.searchProductsByNombre(nombre));
    }
    
    @PostMapping
    public ResponseEntity<Product> createProduct(@Valid @RequestBody ProductRequest request) {
        Product product = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(product);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable String id, 
                                                  @Valid @RequestBody ProductRequest request) {
        Product product = productService.updateProduct(id, request);
        return ResponseEntity.ok(product);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable String id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
    
    @PatchMapping("/{id}/stock")
    public ResponseEntity<Product> updateStock(@PathVariable String id, 
                                                @RequestParam Integer stock) {
        Product product = productService.updateStock(id, stock);
        return ResponseEntity.ok(product);
    }
}
