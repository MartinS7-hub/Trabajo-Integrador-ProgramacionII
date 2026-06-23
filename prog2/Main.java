package integrado.prog2;

import integrado.prog2.config.ConexionDB;
import integrado.prog2.entidad.*;
import integrado.prog2.enums.*;
import integrado.prog2.excepcion.*;
import integrado.prog2.servicio.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {

    private static final CategoriaServicio categoriaServicio = new CategoriaServicio();
    private static final ProductoServicio productoServicio = new ProductoServicio();
    private static final UsuarioServicio usuarioServicio = new UsuarioServicio();
    private static final PedidoServicio pedidoServicio = new PedidoServicio();

    public static void main(String[] args) {
        ConexionDB.inicializarTodo();
        Scanner scanner = new Scanner(System.in);

        // Precarga controlada para evitar duplicados en SQLite
        cargarDatosIniciales();

        int opcion;
        do {
            System.out.println("\n=== SISTEMA DE PEDIDOS ===");
            System.out.println("1. Gestionar Categorías");
            System.out.println("2. Gestionar Productos");
            System.out.println("3. Gestionar Usuarios");
            System.out.println("4. Gestionar Pedidos");
            System.out.println("0. Salir del Sistema");
            System.out.print("Seleccione una opción: ");

            try {
                opcion = scanner.nextInt();
                scanner.nextLine();

                switch (opcion) {
                    case 1 -> menuCategorias(scanner);
                    case 2 -> menuProductos(scanner);
                    case 3 -> menuUsuarios(scanner);
                    case 4 -> menuPedidos(scanner);
                    case 0 -> System.out.println("¡Gracias por utilizar Food Store! Saliendo...");
                    default -> System.out.println(" Opción inválida. Intente de nuevo.");
                }
            } catch (Exception e) {
                System.out.println("Error: Ingrese un número válido.");
                scanner.nextLine();
                opcion = -1;
            }
        } while (opcion != 0);

        scanner.close();
    }

    private static void cargarDatosIniciales() {
        // 1. Si está vacío, creamos categorías
        if (categoriaServicio.listar().isEmpty()) {
            categoriaServicio.create("Hamburguesas", "Variedad de hamburguesas caseras");
            categoriaServicio.create("Bebidas", "Gaseosas y jugos bien fríos");
        }

        List<Categoria> lista = categoriaServicio.listar();
        Long idHamburguesas = null;
        Long idBebidas = null;

        for (Categoria c : lista) {
            if (c.getNombre().equalsIgnoreCase("Hamburguesas")) idHamburguesas = c.getId();
            if (c.getNombre().equalsIgnoreCase("Bebidas")) idBebidas = c.getId();
        }

        if (productoServicio.listar().isEmpty() && idHamburguesas != null && idBebidas != null) {
            productoServicio.crear("Burger Clásica", 4500.0, "Carne, queso y lechuga", 15, "burger.png", true, idHamburguesas);
            productoServicio.crear("Burger Completa", 5500.0, "Carne, queso, huevo y bacon", 10, "burger_comp.png", true, idHamburguesas);
            productoServicio.crear("Gaseosa Cola 500ml", 1200.0, "Línea Coca Cola", 30, "coca.png", true, idBebidas);
        }

        if (usuarioServicio.listar().isEmpty()) {
            usuarioServicio.crear("Juan", "Pérez", "juan@mail.com", "261555555", "1234", Rol.ADMIN);
            usuarioServicio.crear("María", "Gómez", "maria@mail.com", "261666666", "abcd", Rol.USUARIO);
        }
    }

    private static void menuCategorias(Scanner scanner) {
        int opt;
        do {
            System.out.println("\n--- GESTIÓN DE CATEGORÍAS ---");
            System.out.println("1. Listar Categorías");
            System.out.println("2. Crear Categoría");
            System.out.println("3. Editar Categoría");
            System.out.println("4. Eliminar Categoría");
            System.out.println("0. Volver al menú principal");
            System.out.print("Seleccione: ");
            opt = scanner.nextInt();
            scanner.nextLine();

            try {
                switch (opt) {
                    case 1 -> {
                        List<Categoria> lista = categoriaServicio.listar();
                        if (lista.isEmpty()) System.out.println("No hay categorías activas.");
                        else lista.forEach(System.out::println);
                    }
                    case 2 -> {
                        System.out.print("Nombre: "); String nom = scanner.nextLine();
                        System.out.print("Descripción: "); String desc = scanner.nextLine();
                        categoriaServicio.create(nom, desc);
                        System.out.println("¡Categoría creada con éxito!");
                    }
                    case 3 -> {
                        System.out.print("ID de Categoría a editar: "); Long id = scanner.nextLong();
                        scanner.nextLine();
                        System.out.print("Nuevo Nombre (vacío para no cambiar): "); String nom = scanner.nextLine();
                        System.out.print("Nueva Descripción (vacío para no cambiar): "); String desc = scanner.nextLine();
                        categoriaServicio.editar(id, nom, desc);
                        System.out.println("¡Categoría modificada con éxito!");
                    }
                    case 4 -> {
                        System.out.print("ID de Categoría a eliminar: "); Long id = scanner.nextLong();
                        categoriaServicio.eliminar(id);
                        System.out.println("¡Categoría eliminada (lógicamente) con éxito!");
                    }
                }
            } catch (Exception e) {
                System.out.println(" X " + e.getMessage());
            }
        } while (opt != 0);
    }

    private static void menuProductos(Scanner scanner) {
        int opt;
        do {
            System.out.println("\n--- GESTIÓN DE PRODUCTOS ---");
            System.out.println("1. Listar Productos");
            System.out.println("2. Crear Producto");
            System.out.println("3. Editar Producto");
            System.out.println("4. Eliminar Producto");
            System.out.println("0. Volver");
            System.out.print("Seleccione: ");
            opt = scanner.nextInt();
            scanner.nextLine();

            try {
                switch (opt) {
                    case 1 -> {
                        List<Producto> lista = productoServicio.listar();
                        if (lista.isEmpty()) System.out.println("No hay productos activos.");
                        else lista.forEach(System.out::println);
                    }
                    case 2 -> {
                        System.out.print("Nombre: "); String nom = scanner.nextLine();
                        System.out.print("Precio: "); Double pre = scanner.nextDouble();
                        scanner.nextLine();
                        System.out.print("Descripción: "); String desc = scanner.nextLine();
                        System.out.print("Stock Inicial: "); int stk = scanner.nextInt();
                        scanner.nextLine();
                        System.out.print("Nombre de Imagen: "); String img = scanner.nextLine();
                        System.out.print("ID Categoría asociada: "); Long catId = scanner.nextLong();

                        productoServicio.crear(nom, pre, desc, stk, img, true, catId);
                        System.out.println("¡Producto creado con éxito!");
                    }
                    case 3 -> {
                        System.out.print("ID del Producto a editar: "); Long id = scanner.nextLong();
                        scanner.nextLine();
                        System.out.print("Nuevo Nombre (vacío para mantener): "); String nom = scanner.nextLine();
                        System.out.print("Nuevo Precio (0 para mantener): "); Double pre = scanner.nextDouble();
                        scanner.nextLine();
                        System.out.print("Nueva Descripción (vacío para mantener): "); String desc = scanner.nextLine();
                        System.out.print("Nuevo Stock (-1 para mantener): "); int stk = scanner.nextInt();
                        scanner.nextLine();

                        Double pEnv = (pre == 0) ? null : pre;
                        Integer sEnv = (stk == -1) ? null : stk;

                        productoServicio.editar(id, nom, pEnv, desc, sEnv, null, null, null);
                        System.out.println("¡Producto modificado!");
                    }
                    case 4 -> {
                        System.out.print("ID del Producto a eliminar: "); Long id = scanner.nextLong();
                        productoServicio.eliminar(id);
                        System.out.println("¡Producto eliminado lógicamente!");
                    }
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        } while (opt != 0);
    }

    private static void menuUsuarios(Scanner scanner) {
        int opt;
        do {
            System.out.println("\n--- GESTIÓN DE USUARIOS ---");
            System.out.println("1. Listar Usuarios");
            System.out.println("2. Crear Usuario");
            System.out.println("3. Eliminar Usuario");
            System.out.println("0. Volver");
            System.out.print("Seleccione: ");
            opt = scanner.nextInt();
            scanner.nextLine();

            try {
                switch (opt) {
                    case 1 -> usuarioServicio.listar().forEach(System.out::println);
                    case 2 -> {
                        System.out.print("Nombre: "); String n = scanner.nextLine();
                        System.out.print("Apellido: "); String a = scanner.nextLine();
                        System.out.print("Email: "); String m = scanner.nextLine();
                        System.out.print("Celular: "); String c = scanner.nextLine();
                        System.out.print("Contraseña: "); String pass = scanner.nextLine();
                        System.out.print("Rol (1 para ADMIN, 2 para USUARIO): "); int rOpt = scanner.nextInt();
                        Rol rol = (rOpt == 1) ? Rol.ADMIN : Rol.USUARIO;

                        usuarioServicio.crear(n, a, m, c, pass, rol);
                        System.out.println("¡Usuario registrado con éxito!");
                    }
                    case 3 -> {
                        System.out.print("ID del Usuario a eliminar: "); Long id = scanner.nextLong();
                        usuarioServicio.eliminar(id);
                        System.out.println("¡Usuario eliminado lógicamente!");
                    }
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        } while (opt != 0);
    }

    private static void menuPedidos(Scanner scanner) {
        int opt;
        do {
            System.out.println("\n--- GESTIÓN DE PEDIDOS ---");
            System.out.println("1. Listar Historial de Pedidos");
            System.out.println("2. Registrar una Nueva Compra ");
            System.out.println("3. Cambiar Estado de un Pedido");
            System.out.println("4. Cancelar Pedido ");
            System.out.println("0. Volver");
            System.out.print("Seleccione: ");
            opt = scanner.nextInt();
            scanner.nextLine();

            try {
                switch (opt) {
                    case 1 -> {
                        List<Pedido> peds = pedidoServicio.listar();
                        if (peds.isEmpty()) {
                            System.out.println("No hay pedidos registrados en el sistema.");
                        } else {
                            peds.forEach(System.out::println);
                        }
                    }
                    case 2 -> {
                        System.out.print("ID del Usuario (Cliente) que compra: "); Long userId = scanner.nextLong();
                        System.out.println("Seleccione Forma de Pago: 1.TARJETA | 2.TRANSFERENCIA | 3.EFECTIVO");
                        int fpOpt = scanner.nextInt();
                        FormaPago fp = switch (fpOpt) {
                            case 1 -> FormaPago.TARJETA;
                            case 2 -> FormaPago.TRANSFERENCIA;
                            default -> FormaPago.EFECTIVO;
                        };

                        List<DetallePedido> detalles = new ArrayList<>();
                        String agregarMas;
                        do {
                            System.out.print("ID del Producto a comprar: "); Long pId = scanner.nextLong();
                            System.out.print("Cantidad: "); int cantidad = scanner.nextInt();
                            scanner.nextLine();

                            Producto prodReal = productoServicio.buscarPorId(pId);

                            DetallePedido det = new DetallePedido();
                            det.setProducto(prodReal);
                            det.setCantidad(cantidad);

                            detalles.add(det);
                            System.out.println("Agregado: " + prodReal.getNombre() + " | Subtotal: $" + det.getSubtotal());

                            System.out.print("¿Quiere agregar otro producto a este pedido? (S/N): ");
                            agregarMas = scanner.nextLine().trim().toLowerCase();
                        } while (agregarMas.equals("s"));

                        pedidoServicio.crearPedido(userId, fp, detalles);
                    }
                    case 3 -> {
                        System.out.print("ID del Pedido a modificar: "); Long id = scanner.nextLong();
                        System.out.println("Nuevo Estado: 1.PENDIENTE | 2.CONFIRMADO | 3.TERMINADO | 4.CANCELADO");
                        int estOpt = scanner.nextInt();
                        Estado est = switch (estOpt) {
                            case 1 -> Estado.PENDIENTE;
                            case 2 -> Estado.CONFIRMADO;
                            case 3 -> Estado.TERMINADO;
                            case 4 -> Estado.CANCELADO;
                            default -> Estado.PENDIENTE;
                        };
                        pedidoServicio.actualizarEstado(id, est);
                        System.out.println("¡Estado actualizado con éxito!");
                    }
                    case 4 -> {
                        System.out.print("ID del Pedido a eliminar lógicamente: "); Long id = scanner.nextLong();
                        pedidoServicio.eliminar(id);
                        System.out.println("¡Pedido removido de las pantallas activas!");
                    }
                }
            } catch (Exception e) {
                System.out.println("\n  OPERACIÓN CANCELADA: " + e.getMessage());
            }
        } while (opt != 0);
    }
}
