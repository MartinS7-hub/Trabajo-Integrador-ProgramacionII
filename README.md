                                                  🍔 Food Store 
Sistema de Gestión de Pedidos
Food Store es un sistema de gestión integral desarrollado en Java (POO) diseñado para administrar categorías, productos, usuarios y pedidos de un negocio de comidas. El sistema destaca por su arquitectura multicapa y el uso de SQLite como motor de persistencia, garantizando la integridad de los datos mediante operaciones ACID.

                                           🚀 Características Principales
Arquitectura Multicapa: Separación clara entre entidades, lógica de negocio (Servicios), persistencia (Configuración) y vista (Consola).

Persistencia Robusta: Uso de JDBC con SQLite para almacenamiento en base de datos local (food_store.db).

Gestión de Datos: Soporte para altas, bajas lógicas, modificaciones y consultas (CRUD).

Manejo de Errores: Excepciones personalizadas para reglas de negocio y validación de datos.

Lógica de Pedidos: Implementación de interfaz Calcular y gestión de detalles de pedido.

                                              🛠 Tecnologías Utilizadas
Lenguaje: Java 21.

Base de Datos: SQLite

Conector: SQLite-JDBC Driver

Patrones de Diseño: Singleton (Conexión)

                                               📂 Estructura del Proyecto
<img width="445" height="157" alt="{4B988458-4E38-4D06-B8FC-3DE668453609}" src="https://github.com/user-attachments/assets/1c33c72a-0fed-49e0-9ca5-07b3b380dd65" />



                                               ⚙️ Configuración y Ejecución
Requisitos:

Tener instalado el JDK 17 o superior.

Tener configurado el driver de SQLite (sqlite-jdbc) en tu Classpath.

                                                        🕊️ Ejecución

Compila el proyecto.

Configura el driver de SQLite el cual guardamos en la carpeta 📂 Drive SQLite.

<img width="490" height="164" alt="{A4AA0B25-45CF-4B18-A439-FBF4B8B1F591}" src="https://github.com/user-attachments/assets/aa1ed0a2-fe92-4fd7-aabd-3eb50e70d889" />

Ejecuta la clase integrado.prog2.Main.

Al iniciarse, el sistema creará automáticamente el archivo food_store.db en la raíz del proyecto.

Reiniciar el estado:

Si deseas limpiar los datos de prueba, simplemente elimina el archivo food_store.db y vuelve a ejecutar el programa.

                                              📝 Reglas de Negocio Implementadas
Baja Lógica: Los elementos eliminados no se borran físicamente, lo que permite mantener el historial de auditoría (eliminado = 1).

Validaciones: Se validan precios, stock, y unicidad de correos electrónicos antes de cualquier persistencia.

Transaccionalidad: Los pedidos se crean asegurando que tanto el pedido principal como sus detalles mantengan la integridad referencial.

Desarrollado como Trabajo Práctico Integrador para la cátedra de Programación 2.
