package integrado.prog2.servicio;

import integrado.prog2.config.ConexionDB;
import integrado.prog2.entidad.Categoria;
import integrado.prog2.excepcion.EntidadNoEncontradaExcepcion;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategoriaServicio {

    // Metodo fundamental para validación y reuso
    public Categoria buscarPorId(Long id) {
        String sql = "SELECT * FROM categoria WHERE id = ? AND eliminado = 0";
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Categoria(rs.getLong("id"), rs.getString("nombre"), rs.getString("descripcion"));
                } else {
                    throw new EntidadNoEncontradaExcepcion("Categoría no encontrada con ID: " + id);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error en base de datos al buscar por ID: " + e.getMessage());
        }
    }

    // Versión para usar DENTRO de una transacción ya abierta (no abre ni cierra conexión propia)
    public Categoria buscarPorIdConConexion(Long id, Connection conn) throws SQLException {
        String sql = "SELECT * FROM categoria WHERE id = ? AND eliminado = 0";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Categoria(rs.getLong("id"), rs.getString("nombre"), rs.getString("descripcion"));
                } else {
                    throw new EntidadNoEncontradaExcepcion("Categoría no encontrada con ID: " + id);
                }
            }
        }
    }

    // HU-CAT-01: Listar categorías activas
    public List<Categoria> listar() {
        List<Categoria> lista = new ArrayList<>();
        String sql = "SELECT * FROM categoria WHERE eliminado = 0";

        try (Connection conn = ConexionDB.getConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                lista.add(new Categoria(
                        rs.getLong("id"),
                        rs.getString("nombre"),
                        rs.getString("descripcion")
                ));
            }
        } catch (SQLException e) {
            System.out.println("Error al listar categorías: " + e.getMessage());
        }
        return lista;
    }

    // HU-CAT-02: Crear categoría
    public void create(String nombre, String descripcion) {
        String sql = "INSERT INTO categoria (nombre, descripcion, created_at) VALUES (?, ?, ?)";
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, nombre);
            pstmt.setString(2, descripcion);
            pstmt.setString(3, java.time.LocalDateTime.now().toString());
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.out.println("Error al insertar categoría: " + e.getMessage());
        }
    }

    // HU-CAT-03: Editar categoría existente
    public void editar(Long id, String nombre, String desc) {
        // Validamos existencia usando el metodo centralizado
        buscarPorId(id);

        String sqlUpdate = "UPDATE categoria SET nombre = ?, descripcion = ? WHERE id = ?";

        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement pstmtUpdate = conn.prepareStatement(sqlUpdate)) {

            pstmtUpdate.setString(1, nombre);
            pstmtUpdate.setString(2, desc);
            pstmtUpdate.setLong(3, id);
            pstmtUpdate.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar la categoría en BD: " + e.getMessage());
        }
    }

    // HU-CAT-04: Eliminar categoría
    public void eliminar(Long id) {
        // Validamos existencia antes de borrar
        buscarPorId(id);

        String sql = "UPDATE categoria SET eliminado = 1 WHERE id = ?";
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar: " + e.getMessage());
        }
    }

}