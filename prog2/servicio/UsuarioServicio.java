package integrado.prog2.servicio;

import integrado.prog2.config.ConexionDB;
import integrado.prog2.entidad.Usuario;
import integrado.prog2.enums.Rol;
import integrado.prog2.excepcion.EntidadNoEncontradaExcepcion;
import integrado.prog2.excepcion.ReglaNegocioExcepcion;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class UsuarioServicio {

    private final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // HU-USU-02: Registrar usuario con validación de Email único
    public void registrar(String nombre, String apellido, String mail, String celular, String contraseña, Rol rol) {
        // Validaciones básicas de entrada
        if (nombre == null || nombre.trim().isEmpty() || apellido == null || apellido.trim().isEmpty()) {
            throw new ReglaNegocioExcepcion("El nombre y el apellido son obligatorios.");
        }
        if (mail == null || mail.trim().isEmpty()) {
            throw new ReglaNegocioExcepcion("El correo electrónico no puede estar vacío.");
        }
        if (contraseña == null || contraseña.isEmpty()) {
            throw new ReglaNegocioExcepcion("La contraseña no puede estar vacía.");
        }
        if (rol == null) {
            throw new ReglaNegocioExcepcion("El rol asignado no es válido.");
        }

        // 1. Validar que el mail no esté duplicado en usuarios activos (eliminado = 0)
        String sqlCheckMail = "SELECT COUNT(*) FROM usuario WHERE mail = ? AND eliminado = 0";

        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement pstmtCheck = conn.prepareStatement(sqlCheckMail)) {

            pstmtCheck.setString(1, mail.trim().toLowerCase());
            try (ResultSet rs = pstmtCheck.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    throw new ReglaNegocioExcepcion("El correo electrónico '" + mail + "' ya está registrado en el sistema.");
                }
            }

            // 2. Si el mail es único, hacemos el INSERT en la base de datos
            String sqlInsert = """
                INSERT INTO usuario (nombre, apellido, mail, celular, contraseña, rol, eliminado, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

            try (PreparedStatement pstmtInsert = conn.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS)) {
                pstmtInsert.setString(1, nombre.trim());
                pstmtInsert.setString(2, apellido.trim());
                pstmtInsert.setString(3, mail.trim().toLowerCase());
                pstmtInsert.setString(4, celular);
                pstmtInsert.setString(5, contraseña); // En un sistema real iría encriptada, acá pasa directo
                pstmtInsert.setString(6, rol.name()); // Guardamos el Enum como String (ADMIN, CLIENTE, etc.)
                pstmtInsert.setBoolean(7, false);     // eliminado = false
                pstmtInsert.setString(8, LocalDateTime.now().format(formatter));

                pstmtInsert.executeUpdate();

                try (ResultSet generatedKeys = pstmtInsert.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        System.out.println("👤 Usuario registrado en BD con ID: " + generatedKeys.getLong(1));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error de BD al registrar usuario: " + e.getMessage(), e);
        }
    }

    // HU-USU-01: Listar todos los usuarios activos
    public List<Usuario> listar() {
        List<Usuario> usuarios = new ArrayList<>();
        String sql = "SELECT id, nombre, apellido, mail, celular, contraseña, rol, eliminado, created_at FROM usuario WHERE eliminado = 0";

        try (Connection conn = ConexionDB.getConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Usuario user = new Usuario();
                user.setId(rs.getLong("id"));
                user.setNombre(rs.getString("nombre"));
                user.setApellido(rs.getString("apellido"));
                user.setMail(rs.getString("mail"));
                user.setCellular(rs.getString("celular"));
                user.setContraseña(rs.getString("contraseña"));
                user.setRol(Rol.valueOf(rs.getString("rol"))); // Convertimos el String de la BD de vuelta a Enum
                user.setEliminado(rs.getBoolean("eliminado"));
                user.setCreatedAt(LocalDateTime.parse(rs.getString("created_at"), formatter));

                usuarios.add(user);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error de BD al listar usuarios: " + e.getMessage(), e);
        }
        return usuarios;
    }

    // HU-USU-03: Editar perfil de usuario dinámicamente
    public void editar(Long id, String nombre, String apellido, String mail, String celular, String contraseña, Rol rol) {
        // Validamos primero que el usuario exista
        buscarPorId(id);

        StringBuilder sql = new StringBuilder("UPDATE usuario SET ");
        boolean tieneCampos = false;

        if (nombre != null && !nombre.trim().isEmpty()) {
            sql.append("nombre = ?, ");
            tieneCampos = true;
        }
        if (apellido != null && !apellido.trim().isEmpty()) {
            sql.append("apellido = ?, ");
            tieneCampos = true;
        }
        if (mail != null && !mail.trim().isEmpty()) {
            // Validar que el nuevo mail no lo tenga otra persona
            validarMailParaEditar(id, mail.trim().toLowerCase());
            sql.append("mail = ?, ");
            tieneCampos = true;
        }
        if (celular != null) {
            sql.append("celular = ?, ");
            tieneCampos = true;
        }
        if (contraseña != null && !contraseña.isEmpty()) {
            sql.append("contraseña = ?, ");
            tieneCampos = true;
        }
        if (rol != null) {
            sql.append("rol = ?, ");
            tieneCampos = true;
        }

        if (!tieneCampos) return;

        sql.setLength(sql.length() - 2); // Remover la última coma
        sql.append(" WHERE id = ? AND eliminado = 0");

        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            int idx = 1;
            if (nombre != null && !nombre.trim().isEmpty()) pstmt.setString(idx++, nombre.trim());
            if (apellido != null && !apellido.trim().isEmpty()) pstmt.setString(idx++, apellido.trim());
            if (mail != null && !mail.trim().isEmpty()) pstmt.setString(idx++, mail.trim().toLowerCase());
            if (celular != null) pstmt.setString(idx++, celular);
            if (contraseña != null && !contraseña.isEmpty()) pstmt.setString(idx++, contraseña);
            if (rol != null) pstmt.setString(idx++, rol.name());

            pstmt.setLong(idx, id);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error de BD al editar usuario: " + e.getMessage(), e);
        }
    }


    public void eliminar(Long id) {
        buscarPorId(id);


        String sql = "UPDATE usuario SET eliminado = 1 WHERE id = ?";

        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error de BD al eliminar usuario: " + e.getMessage(), e);
        }
    }

    // Auxiliar para buscar un usuario específico por su ID
    public Usuario buscarPorId(Long id) {
        String sql = "SELECT id, nombre, apellido, mail, celular, contraseña, rol, eliminado, created_at FROM usuario WHERE id = ? AND eliminado = 0";

        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Usuario user = new Usuario();
                    user.setId(rs.getLong("id"));
                    user.setNombre(rs.getString("nombre"));
                    user.setApellido(rs.getString("apellido"));
                    user.setMail(rs.getString("mail"));
                    user.setCellular(rs.getString("celular"));
                    user.setContraseña(rs.getString("contraseña"));
                    user.setRol(Rol.valueOf(rs.getString("rol")));
                    user.setEliminado(rs.getBoolean("eliminado"));
                    user.setCreatedAt(LocalDateTime.parse(rs.getString("created_at"), formatter));
                    return user;
                } else {
                    throw new EntidadNoEncontradaExcepcion("No se encontró ningún usuario activo con el ID: " + id);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error de BD al buscar usuario por ID: " + e.getMessage(), e);
        }
    }

    // Metodo auxiliar para evitar que al editar se elija el mail de OTRA persona
    private void validarMailParaEditar(Long usuarioIdActual, String nuevoMail) {
        String sql = "SELECT COUNT(*) FROM usuario WHERE mail = ? AND id <> ? AND eliminado = 0";
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, nuevoMail);
            pstmt.setLong(2, usuarioIdActual);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    throw new ReglaNegocioExcepcion("No se puede actualizar el perfil: el email '" + nuevoMail + "' ya está siendo usado por otro usuario.");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void crear(String nombre, String apellido, String mail, String celular, String contraseña, Rol rol) {
        registrar(nombre, apellido, mail, celular, contraseña, rol);
    }
}