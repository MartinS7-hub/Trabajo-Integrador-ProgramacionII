package integrado.prog2.entidad;

public class DetallePedido extends Base {
    private int cantidad;
    private Double subtotal;
    private Producto producto; // Relación con el producto vendido

    public DetallePedido() {
        super();
    }

    public DetallePedido(Long id, int cantidad, Producto producto) {
        super(id);
        this.cantidad = cantidad;
        this.producto = producto;
        // El subtotal se calcula automáticamente al crearse el detalle
        this.subtotal = (producto != null) ? producto.getPrecio() * cantidad : 0.0;
    }

    // Getters y Setters
    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
        recalcularSubtotal();
    }

    public Double getSubtotal() { return subtotal; }

    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) {
        this.producto = producto;
        recalcularSubtotal();
    }

    // Método interno para actualizar el subtotal si cambian el producto o la cantidad
    private void recalcularSubtotal() {
        this.subtotal = (producto != null) ? producto.getPrecio() * cantidad : 0.0;
    }

    @Override
    public String toString() {
        String prodNombre = (producto != null) ? producto.getNombre() : "Producto Desconocido";
        return " - " + prodNombre + " x" + cantidad + " | Subtotal: $" + subtotal;
    }
}