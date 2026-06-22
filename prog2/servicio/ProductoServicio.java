package integrado.prog2.servicio;

import integrado.prog2.config.ConexionDB;
import integrado.prog2.entidad.Categoria;
import integrado.prog2.entidad.Producto;
import integrado.prog2.excepcion.EntidadNoEncontradaExcepcion;
import integrado.prog2.excepcion.ReglaNegocioExcepcion;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ProductoServicio {

    private final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private final CategoriaServicio categoriaServicio = new CategoriaServicio();

    // Firma corregida a 7 parámetros para coincidir con Main.java
    public void crear(String nombre, double precio, String descripcion, int stock, String imagen, boolean disponible, Long categoriaId) {
        // Validaciones básicas de reglas de negocio
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new ReglaNegocioExcepcion("El nombre del producto no puede estar vacío.");
        }
        if (precio < 0) {
            throw new ReglaNegocioExcepcion("El precio no puede ser negativo.");
        }
        if (stock < 0) {
            throw new ReglaNegocioExcepcion("El stock inicial no puede ser negativo.");
        }

        // Verificamos que la categoría exista antes de asociarla
        categoriaServicio.buscarPorId(categoriaId);

        String sql = """
            INSERT INTO producto (nombre, precio, descripcion, stock, imagen, disponible, categoria_id, eliminado, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, nombre.trim());
            pstmt.setDouble(2, precio);
            pstmt.setString(3, descripcion);
            pstmt.setInt(4, stock);
            pstmt.setString(5, imagen);
            pstmt.setBoolean(6, disponible); // Usa el booleano que viene desde el Main
            pstmt.setLong(7, categoriaId);
            pstmt.setBoolean(8, false); // eliminado = false
            pstmt.setString(9, LocalDateTime.now().format(formatter));

            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    System.out.println("📦 Producto guardado en BD con ID: " + generatedKeys.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar el producto en la BD: " + e.getMessage(), e);
        }
    }

    // HU-PROD-01: Listar productos activos
    public List<Producto> listar() {
        List<Producto> productos = new ArrayList<>();
        String sql = "SELECT * FROM producto WHERE eliminado = 0";

        // 1. PRIMERA PASADA: Solo leemos los datos crudos (sin llamar a otros servicios)
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Producto prod = new Producto();
                prod.setId(rs.getLong("id"));
                prod.setNombre(rs.getString("nombre"));
                prod.setPrecio(rs.getDouble("precio"));
                prod.setDescripcion(rs.getString("descripcion"));
                prod.setStock(rs.getInt("stock"));
                prod.setImagen(rs.getString("imagen"));
                prod.setDisponible(rs.getBoolean("disponible"));
                prod.setEliminado(rs.getBoolean("eliminado"));
                prod.setCreatedAt(LocalDateTime.parse(rs.getString("created_at"), formatter));

                // Guardamos solo el ID para procesar la relación después
                prod.setCategoria(new Categoria(rs.getLong("categoria_id"), null, null));

                productos.add(prod);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar productos desde la BD: " + e.getMessage(), e);
        }

        // 2. SEGUNDA PASADA: Enriquecemos los objetos (aquí el ResultSet ya está cerrado)
        for (Producto p : productos) {
            if (p.getCategoria() != null && p.getCategoria().getId() != null) {
                try {
                    Categoria cat = categoriaServicio.buscarPorId(p.getCategoria().getId());
                    p.setCategoria(cat);
                } catch (EntidadNoEncontradaExcepcion e) {
                    p.setCategoria(null); // O manejar como prefieras
                }
            }
        }

        return productos;
    }

    // 🛠️ Firma corregida a 8 parámetros para coincidir con Main.java (agrega Boolean disponible)
    public void editar(Long id, String nombre, Double precio, String descripcion, Integer stock, String imagen, Boolean disponible, Long categoriaId) {
        // Validamos que el producto exista
        buscarPorId(id);

        StringBuilder sql = new StringBuilder("UPDATE producto SET ");
        boolean tieneCampos = false;

        if (nombre != null && !nombre.trim().isEmpty()) {
            sql.append("nombre = ?, ");
            tieneCampos = true;
        }
        if (precio != null) {
            if (precio < 0) throw new ReglaNegocioExcepcion("El precio no puede ser negativo.");
            sql.append("precio = ?, ");
            tieneCampos = true;
        }
        if (descripcion != null) {
            sql.append("descripcion = ?, ");
            tieneCampos = true;
        }
        if (stock != null) {
            if (stock < 0) throw new ReglaNegocioExcepcion("El stock no puede ser negativo.");
            sql.append("stock = ?, disponible = ?, ");
            tieneCampos = true;
        }
        if (imagen != null) {
            sql.append("imagen = ?, ");
            tieneCampos = true;
        }
        if (disponible != null) {
            sql.append("disponible = ?, ");
            tieneCampos = true;
        }
        if (categoriaId != null) {
            categoriaServicio.buscarPorId(categoriaId);
            sql.append("categoria_id = ?, ");
            tieneCampos = true;
        }

        if (!tieneCampos) return;

        sql.setLength(sql.length() - 2); // Quitar la última coma y espacio
        sql.append(" WHERE id = ? AND eliminado = 0");

        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            int idx = 1;
            if (nombre != null && !nombre.trim().isEmpty()) pstmt.setString(idx++, nombre.trim());
            if (precio != null) pstmt.setDouble(idx++, precio);
            if (descripcion != null) pstmt.setString(idx++, descripcion);
            if (stock != null) {
                pstmt.setInt(idx++, stock);
                pstmt.setBoolean(idx++, stock > 0);
            }
            if (imagen != null) pstmt.setString(idx++, imagen);
            if (disponible != null) pstmt.setBoolean(idx++, disponible);
            if (categoriaId != null) pstmt.setLong(idx++, categoriaId);

            pstmt.setLong(idx, id);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error al editar el producto en la BD: " + e.getMessage(), e);
        }
    }

    // HU-PROD-04: Baja lógica del producto
    public void eliminar(Long id) {
        buscarPorId(id);

        String sql = "UPDATE producto SET eliminado = 1 WHERE id = ?";
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al realizar la baja lógica del producto: " + e.getMessage(), e);
        }
    }

    // Metodo clave para restar stock cuando se confirma un pedido
    public void reducirStock(Long id, int cantidad) {
        Producto prod = buscarPorId(id);
        if (prod.getStock() < cantidad) {
            throw new ReglaNegocioExcepcion("Stock insuficiente para el producto: " + prod.getNombre());
        }

        int nuevoStock = prod.getStock() - cantidad;
        String sql = "UPDATE producto SET stock = ?, disponible = ? WHERE id = ?";

        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, nuevoStock);
            pstmt.setBoolean(2, nuevoStock > 0);
            pstmt.setLong(3, id);

            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar el stock en la BD: " + e.getMessage(), e);
        }
    }

    // Versión para usar DENTRO de una transacción ya abierta (no abre ni cierra conexión propia)
    public Producto buscarPorIdConConexion(Long id, Connection conn) throws SQLException {
        String sql = "SELECT id, nombre, precio, descripcion, stock, imagen, disponible, categoria_id, eliminado, created_at FROM producto WHERE id = ? AND eliminado = 0";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Producto prod = new Producto();
                    prod.setId(rs.getLong("id"));
                    prod.setNombre(rs.getString("nombre"));
                    prod.setPrecio(rs.getDouble("precio"));
                    prod.setDescripcion(rs.getString("descripcion"));
                    prod.setStock(rs.getInt("stock"));
                    prod.setImagen(rs.getString("imagen"));
                    prod.setDisponible(rs.getBoolean("disponible"));
                    prod.setEliminado(rs.getBoolean("eliminado"));
                    prod.setCreatedAt(LocalDateTime.parse(rs.getString("created_at"), formatter));
                    prod.setCategoria(categoriaServicio.buscarPorIdConConexion(rs.getLong("categoria_id"), conn));
                    return prod;
                } else {
                    throw new EntidadNoEncontradaExcepcion("No se encontró ningún producto activo con el ID: " + id);
                }
            }
        }
    }

    // Versión para usar DENTRO de una transacción ya abierta (no abre ni cierra conexión propia)
    public void reducirStockConConexion(Long id, int cantidad, Connection conn) throws SQLException {
        String sql = "UPDATE producto SET stock = ?, disponible = ? WHERE id = ?";

        // Calculamos el nuevo stock a partir del producto ya consultado en esta misma transacción
        Producto prod = buscarPorIdConConexion(id, conn);
        if (prod.getStock() < cantidad) {
            throw new ReglaNegocioExcepcion("Stock insuficiente para el producto: " + prod.getNombre());
        }
        int nuevoStock = prod.getStock() - cantidad;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, nuevoStock);
            pstmt.setBoolean(2, nuevoStock > 0);
            pstmt.setLong(3, id);
            pstmt.executeUpdate();
        }
    }

    // Auxiliar para buscar un producto por ID
    public Producto buscarPorId(Long id) {
        String sql = "SELECT id, nombre, precio, descripcion, stock, imagen, disponible, categoria_id, eliminado, created_at FROM producto WHERE id = ? AND eliminado = 0";

        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Producto prod = new Producto();
                    prod.setId(rs.getLong("id"));
                    prod.setNombre(rs.getString("nombre"));
                    prod.setPrecio(rs.getDouble("precio"));
                    prod.setDescripcion(rs.getString("descripcion"));
                    prod.setStock(rs.getInt("stock"));
                    prod.setImagen(rs.getString("imagen"));
                    prod.setDisponible(rs.getBoolean("disponible"));
                    prod.setEliminado(rs.getBoolean("eliminado"));
                    prod.setCreatedAt(LocalDateTime.parse(rs.getString("created_at"), formatter));

                    prod.setCategoria(categoriaServicio.buscarPorId(rs.getLong("categoria_id")));
                    return prod;
                } else {
                    throw new EntidadNoEncontradaExcepcion("No se encontró ningún producto activo con el ID: " + id);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar el producto por ID: " + e.getMessage(), e);
        }
    }
}