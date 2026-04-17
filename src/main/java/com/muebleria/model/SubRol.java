package com.muebleria.model;

/**
 * Sub-roles para vendedores y encargados locales
 * Define el tipo de canal de venta que puede utilizar el usuario
 */
public enum SubRol {
    VENDEDOR_LOCAL,         // Vende en local físico
    ONLINE_CON_BUSINESS,    // Vende online a través de business/WhatsApp Business
    ONLINE_SIN_BUSINESS     // Vende online directo (redes sociales, web, etc.)
}
