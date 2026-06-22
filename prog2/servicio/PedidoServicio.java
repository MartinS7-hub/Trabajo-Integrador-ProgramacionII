package integrado.prog2.servicio;

import integrado.prog2.config.ConexionDB;
import integrado.prog2.entidad.DetallePedido;
import integrado.prog2.entidad.Pedido;
import integrado.prog2.entidad.Producto;
import integrado.prog2.entidad.Usuario;
import integrado.prog2.enums.Estado;
import integrado.prog2.enums.FormaPago;
import integrado.prog2.excepcion.EntidadNoEncontradaExcepcion;
import integrado.prog2.excepcion.ReglaNegocioExcepcion;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class PedidoServicio {

    private final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private final UsuarioServicio usuarioServicio = new UsuarioServicio();
    private final ProductoServicio productoServicio = new ProductoServicio();

    // HU-PED-02: Crear Pedido con lógica Transaccional (ACID)
    public void crearPedido(Long usuarioId, FormaPago formaPago, List<DetallePedido> detallesInput) {
        if (detallesInput == null || detallesInput.isEmpty()) {
            throw new ReglaNegocioExcepcion("No se puede crear un pedido sin productos.");
        }

        // Validamos que el usuario exista antes de continuar
        Usuario usuario = usuarioServicio.buscarPorId(usuarioId);

        Connection conn = null;
        try {
            conn = ConexionDB.getConexion();
            // Iniciamos transacción manual
            conn.setAutoCommit(false);

            double totalPedido = 0.0;
            List<DetallePedido> detallesProcesados = new ArrayList<>();

            // 1. Validar stock de todos los productos y calcular subtotales
            for (DetallePedido detalle : detallesInput) {
                Producto prod = productoServicio.buscarPorIdConConexion(detalle.getProducto().getId(), conn);

                if (prod.getStock() < detalle.getCantidad()) {
                    throw new ReglaNegocioExcepcion("Stock insuficiente para '" + prod.getNombre() +
                            "'. Solicitado: " + detalle.getCantidad() + " | Disponible: " + prod.getStock());
                }

                // Calcular subtotal de esta línea
                double subtotal = prod.getPrecio() * detalle.getCantidad();
                totalPedido += subtotal;

                // Descontamos el stock en la BD
                productoServicio.reducirStockConConexion(prod.getId(), detalle.getCantidad(), conn);

                // Armamos el detalle definitivo
                detalle.setProducto(prod);
                detalle.setSubtotal(subtotal);
                detallesProcesados.add(detalle);
            }

            // 2. Insertar la cabecera del Pedido en la BD (la tabla conserva los campos de control)
            String sqlPedido = """
                INSERT INTO pedido (usuario_id, fecha, estado, total, forma_pago, eliminado, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

            long pedidoIdGenerated = -1;
            try (PreparedStatement pstmtPed = conn.prepareStatement(sqlPedido, Statement.RETURN_GENERATED_KEYS)) {
                pstmtPed.setLong(1, usuario.getId());
                pstmtPed.setString(2, LocalDateTime.now().format(formatter)); // fecha de control en BD
                pstmtPed.setString(3, Estado.PENDIENTE.name());              // "PENDIENTE"
                pstmtPed.setDouble(4, totalPedido);
                pstmtPed.setString(5, formaPago.name());                    // Guardamos el Enum como String
                pstmtPed.setBoolean(6, false);
                pstmtPed.setString(7, LocalDateTime.now().format(formatter));

                pstmtPed.executeUpdate();

                try (ResultSet rsKeys = pstmtPed.getGeneratedKeys()) {
                    if (rsKeys.next()) {
                        pedidoIdGenerated = rsKeys.getLong(1);
                    }
                }
            }

            // 3. Insertar los detalles vinculados al ID del pedido
            String sqlDetalle = """
                INSERT INTO detalle_pedido (pedido_id, producto_id, cantidad, subtotal, eliminado, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
            """;

            try (PreparedStatement pstmtDet = conn.prepareStatement(sqlDetalle)) {
                for (DetallePedido det : detallesProcesados) {
                    pstmtDet.setLong(1, pedidoIdGenerated);
                    pstmtDet.setLong(2, det.getProducto().getId());
                    pstmtDet.setInt(3, det.getCantidad());
                    pstmtDet.setDouble(4, det.getSubtotal());
                    pstmtDet.setBoolean(5, false);
                    pstmtDet.setString(6, LocalDateTime.now().format(formatter));

                    pstmtDet.addBatch();
                }
                pstmtDet.executeBatch();
            }

            // Confirmamos si no hubo fallos
            conn.commit();
            System.out.println("🛒 ¡Pedido #" + pedidoIdGenerated + " registrado con éxito! Total: $" + totalPedido);

        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                    System.out.println("🔄 Transacción abortada. Se restauraron los cambios correctamente.");
                } catch (SQLException ex) {
                    System.out.println("Error al ejecutar rollback: " + ex.getMessage());
                }
            }
            throw new ReglaNegocioExcepcion(e.getMessage());
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    System.out.println("Error al cerrar conexión: " + e.getMessage());
                }
            }
        }
    }

    public List<Pedido> listar() {
        List<Pedido> lista = new ArrayList<>();

        String sql = """
    SELECT id, usuario_id, estado, total, forma_pago
    FROM pedido
    WHERE eliminado = 0
""";

        // 1. PRIMERA PASADA: leemos solo los datos crudos del pedido, sin llamar a otros servicios
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Pedido ped = new Pedido();

                ped.setId(rs.getLong("id"));
                ped.setTotal(rs.getDouble("total"));

                String estadoStr = rs.getString("estado");
                if (estadoStr != null) {
                    ped.setEstado(Estado.valueOf(estadoStr));
                }

                String formaPagoStr = rs.getString("forma_pago");
                if (formaPagoStr != null) {
                    ped.setFormaPago(FormaPago.valueOf(formaPagoStr));
                }

                // Guardamos solo el ID del usuario para resolverlo después
                Usuario usuarioTemporal = new Usuario();
                usuarioTemporal.setId(rs.getLong("usuario_id"));
                ped.setUsuario(usuarioTemporal);

                lista.add(ped);
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Error al listar pedidos: " + e.getMessage(), e
            );
        }

        // 2. SEGUNDA PASADA: aquí el ResultSet ya está cerrado, así que es seguro
        //    llamar a otros servicios que abren su propia conexión
        for (Pedido ped : lista) {
            if (ped.getUsuario() != null && ped.getUsuario().getId() != null) {
                try {
                    Usuario usuarioReal = usuarioServicio.buscarPorId(ped.getUsuario().getId());
                    ped.setUsuario(usuarioReal);
                } catch (Exception e) {
                    ped.setUsuario(null);
                }
            }
        }

        return lista;
    }

    // EL MeTODO AUXILIAR AHORA ABRE SU PROPIA CONEXIÓN
    private List<DetallePedido> buscarDetallesDePedido(Long pedidoId) {
        List<DetallePedido> detalles = new ArrayList<>();
        String sql = "SELECT id, producto_id, cantidad, subtotal FROM detalle_pedido WHERE pedido_id = ? AND eliminado = 0";

        try (Connection conn = ConexionDB.getConexion(); // ABRIR AQUÍ
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, pedidoId);
            try (ResultSet rs = pstmt.executeQuery()) {
                // ... (lógica de llenado) ...
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return detalles;
    }

    // HU-PED-04: Cancelar / Eliminar lógicamente un pedido
    public void eliminar(Long id) {
        buscarPorId(id);

        String sql = "UPDATE pedido SET eliminado = 1 WHERE id = ?";
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            pstmt.executeUpdate();
            System.out.println("🗑️ Pedido marcado como eliminado lógicamente.");
        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar lógicamente el pedido: " + e.getMessage(), e);
        }
    }

    // Auxiliar: Buscar pedido individual por ID
    public Pedido buscarPorId(Long id) {
        String sql = "SELECT id, usuario_id, estado, total, forma_pago FROM pedido WHERE id = ? AND eliminado = 0";

        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Pedido ped = new Pedido();
                    ped.setId(rs.getLong("id"));
                    ped.setTotal(rs.getDouble("total"));

                    String estadoStr = rs.getString("estado");
                    if (estadoStr != null) {
                        ped.setEstado(Estado.valueOf(estadoStr.toUpperCase().trim()));
                    }

                    String formaPagoStr = rs.getString("forma_pago");
                    if (formaPagoStr != null) {
                        ped.setFormaPago(FormaPago.valueOf(formaPagoStr.toUpperCase().trim()));
                    }

                    ped.setUsuario(usuarioServicio.buscarPorId(rs.getLong("usuario_id")));
                    ped.setDetalles(buscarDetallesDePedido(ped.getId(), conn));
                    return ped;
                } else {
                    throw new EntidadNoEncontradaExcepcion("No se encontró ningún pedido activo con el ID: " + id);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar pedido por ID: " + e.getMessage(), e);
        }
    }

    // Metodo privado auxiliar para rellenar detalles
    private List<DetallePedido> buscarDetallesDePedido(Long pedidoId, Connection conn) throws SQLException {
        List<DetallePedido> detalles = new ArrayList<>();
        String sql = "SELECT id, producto_id, cantidad, subtotal FROM detalle_pedido WHERE pedido_id = ? AND eliminado = 0";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, pedidoId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    DetallePedido det = new DetallePedido();
                    det.setId(rs.getLong("id"));
                    det.setCantidad(rs.getInt("cantidad"));
                    det.setSubtotal(rs.getDouble("subtotal"));

                    try {
                        det.setProducto(productoServicio.buscarPorId(rs.getLong("producto_id")));
                    } catch (Exception e) {
                        det.setProducto(null);
                    }

                    detalles.add(det);
                }
            }
        }
        return detalles;
    }
    // CORRECCIÓN
    public void actualizarEstado(Long id, Estado est) {
        buscarPorId(id); // Valida existencia
        String sql = "UPDATE pedido SET estado = ? WHERE id = ? AND eliminado = 0";
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, est.name());
            pstmt.setLong(2, id);

            int filas = pstmt.executeUpdate(); // Solo una vez
            if (filas == 0) throw new EntidadNoEncontradaExcepcion("Error al actualizar");
            System.out.println("✅ Estado actualizado");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}