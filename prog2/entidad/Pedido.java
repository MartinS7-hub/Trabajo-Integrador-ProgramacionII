package integrado.prog2.entidad;

import integrado.prog2.enums.Estado;
import integrado.prog2.enums.FormaPago;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Pedido extends Base implements Calcular {
    private Double total;
    private Estado estado;
    private FormaPago formaPago;
    private Usuario usuario; // Cliente que hace el pedido
    private List<DetallePedido> detalles; // Lista de renglones del pedido
    private LocalDateTime fecha;
    private boolean eliminado;

    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public void setEliminado(boolean eliminado) { this.eliminado = eliminado; }

    public Pedido() {
        super();
        this.detalles = new ArrayList<>();
        this.total = 0.0;
        this.estado = Estado.PENDIENTE; // Todo pedido arranca pendiente
    }

    public Pedido(Long id, Usuario usuario, FormaPago formaPago) {
        super(id);
        this.usuario = usuario;
        this.formaPago = formaPago;
        this.detalles = new ArrayList<>();
        this.total = 0.0;
        this.estado = Estado.PENDIENTE;
    }

    // Método OBLIGATORIO requerido por la consigna para agregar detalles
    public void addDetallePedido(DetallePedido detalle) {
        if (detalle != null) {
            this.detalles.add(detalle);
            // Cada vez que agregamos un ítem, actualizamos el total general
            this.total = calcularTotal();
        }
    }

    // Implementación OBLIGATORIA de la interfaz Calculable
    @Override
    public Double calcularTotal() {
        Double sumaTotal = 0.0;
        for (DetallePedido detalle : detalles) {
            if (!detalle.isEliminado()) { // Solo sumamos lo que no fue borrado lógicamente
                sumaTotal += detalle.getSubtotal();
            }
        }
        return sumaTotal;
    }

    // Getters y Setters
    public Double getTotal() { return total; }
    public void setTotal(Double total) { this.total = total; }

    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }

    public FormaPago getFormaPago() { return formaPago; }
    public void setFormaPago(FormaPago formaPago) { this.formaPago = formaPago; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public List<DetallePedido> getDetalles() { return detalles; }
    public void setDetalles(List<DetallePedido> detalles) {
        this.detalles = detalles;
        this.total = calcularTotal(); // Recalculamos por las dudas
    }

    @Override
    public String toString() {
        String cliente = (usuario != null) ? usuario.getApellido() + ", " + usuario.getNombre() : "Anónimo";
        StringBuilder sb = new StringBuilder();
        sb.append("=====================================\n");
        sb.append("PEDIDO ID: ").append(getId()).append(" | Cliente: ").append(cliente).append("\n");
        sb.append("Estado: ").append(estado).append(" | Pago: ").append(formaPago).append("\n");
        sb.append("Detalles:\n");
        for (DetallePedido d : detalles) {
            sb.append(d.toString()).append("\n");
        }
        sb.append("TOTAL COMPRA: $").append(total).append("\n");
        sb.append("=====================================");
        return sb.toString();
    }
}