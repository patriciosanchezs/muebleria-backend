package com.muebleria.model;

/**
 * Enum que define los roles de usuario en el sistema.
 */
public enum Role {
    /**
     * Administrador: Acceso completo al sistema.
     * Puede gestionar usuarios, productos, ventas y despachos de todos los locales.
     */
    ADMINISTRADOR,
    
    /**
     * Admin Local: Acceso completo pero limitado a locales asignados.
     * Puede gestionar usuarios, productos, ventas y despachos solo de sus locales.
     */
    ADMIN_LOCAL,
    
    /**
     * Vendedor: Acceso a ventas y productos.
     * Puede ver productos y registrar ventas en sus locales asignados.
     */
    VENDEDOR,
    
    /**
     * Fletero: Acceso solo a despachos.
     * Puede ver despachos pendientes de su local asignado y marcarlos como entregados.
     */
    FLETERO
}
