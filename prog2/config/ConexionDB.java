package integrado.prog2.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class ConexionDB {
    // El archivo de la base de datos se creará en la raíz de tu proyecto con este nombre
    private static final String URL = "jdbc:sqlite:food_store.db";
    private static Connection conexion = null;

    public static Connection getConexion() {
        try {
            if (conexion == null || conexion.isClosed()) {
                Class.forName("org.sqlite.JDBC");
                // Usamos una ruta absoluta para no dejar lugar a dudas
                java.io.File dbFile = new java.io.File("food_store.db");
                String path = dbFile.getAbsolutePath();
                System.out.println("DEBUG: La base de datos está en: " + path);

                conexion = DriverManager.getConnection("jdbc:sqlite:" + path);
            }
        } catch (Exception e) {
            System.out.println("Error crítico: " + e.getMessage());
        }
        return conexion;
    }

    public static void inicializarTodo() {
        // 1. Crear las tablas
        inicializarTablas();

        // 2. Darle un respiro al disco (opcional pero recomendado en Windows)
        try { Thread.sleep(200); } catch (InterruptedException e) {}

        // 3. Activar el modo de escritura rápida
        try (Statement stmt = getConexion().createStatement()) {
            stmt.execute("PRAGMA journal_mode = WAL;");
        } catch (SQLException e) {
            System.out.println("Error configurando modo WAL: " + e.getMessage());
        }
    }
    // Metodo para crear las tablas de forma automática si no existen
    public static void inicializarTablas() {
        Connection conn = getConexion();
        if (conn == null) return;

        try (Statement stmt = conn.createStatement()) {
            // Habilitar soporte de Claves Foráneas en SQLite
            stmt.execute("PRAGMA foreign_keys = ON;");

            // 1. Tabla Categorías
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS categoria (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    nombre TEXT NOT NULL,
                    descripcion TEXT,
                    eliminado BOOLEAN DEFAULT 0,
                    created_at TEXT NOT NULL
                );
            """);

            // 2. Tabla Productos (Relación N:1 con Categoría)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS producto (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    nombre TEXT NOT NULL,
                    precio REAL NOT NULL,
                    descripcion TEXT,
                    stock INTEGER NOT NULL,
                    imagen TEXT,
                    disponible BOOLEAN DEFAULT 1,
                    categoria_id INTEGER,
                    eliminado BOOLEAN DEFAULT 0,
                    created_at TEXT NOT NULL,
                    FOREIGN KEY (categoria_id) REFERENCES categoria(id)
                );
            """);

            // 3. Tabla Usuarios
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS usuario (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    nombre TEXT NOT NULL,
                    apellido TEXT NOT NULL,
                    mail TEXT NOT NULL UNIQUE,
                    celular TEXT,
                    contraseña TEXT NOT NULL,
                    rol TEXT NOT NULL,
                    eliminado BOOLEAN DEFAULT 0,
                    created_at TEXT NOT NULL
                );
            """);

            // 4. Tabla Pedidos (Relación N:1 con Usuario)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS pedido (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    usuario_id INTEGER NOT NULL,
                    fecha TEXT NOT NULL,
                    estado TEXT NOT NULL,
                    total REAL NOT NULL,
                    forma_pago TEXT NOT NULL,
                    eliminado BOOLEAN DEFAULT 0,
                    created_at TEXT NOT NULL,
                    FOREIGN KEY (usuario_id) REFERENCES usuario(id)
                );
            """);

            // 5. Tabla DetallePedido (Relación Composición N:1 con Pedido y Producto)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS detalle_pedido (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    pedido_id INTEGER NOT NULL,
                    producto_id INTEGER NOT NULL,
                    cantidad INTEGER NOT NULL,
                    subtotal REAL NOT NULL,
                    eliminado BOOLEAN DEFAULT 0,
                    created_at TEXT NOT NULL,
                    FOREIGN KEY (pedido_id) REFERENCES pedido(id) ON DELETE CASCADE,
                    FOREIGN KEY (producto_id) REFERENCES producto(id)
                );
            """);

            System.out.println("🗄️ Base de datos SQLite verificada/inicializada con éxito.");

        } catch (SQLException e) {
            System.out.println("Error al ajustar las tablas en SQLite: " + e.getMessage());
        }
    }
}