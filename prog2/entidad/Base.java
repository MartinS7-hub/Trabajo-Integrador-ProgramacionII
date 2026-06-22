package integrado.prog2.entidad;

import java.time.LocalDateTime;

public abstract class Base {
    private Long id;
    private boolean eliminado;
    private LocalDateTime createdAt;
    private double subtotal;

    // Constructor por defecto que inicializa la fecha de creación automáticamente
    public Base() {
        this.createdAt = LocalDateTime.now();
        this.eliminado = false; // Por defecto arranca activo
    }

    // Constructor completo por si necesitas setear los datos manualmente
    public Base(Long id) {
        this.id = id;
        this.createdAt = LocalDateTime.now();
        this.eliminado = false;
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean isEliminado() {
        return eliminado;
    }

    public void setEliminado(boolean eliminado) {
        this.eliminado = eliminado;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }

    public Double getSubtotal() {
        return getSubtotal();
    }
}