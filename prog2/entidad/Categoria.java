package integrado.prog2.entidad;

public class Categoria extends Base {
    private String nombre;
    private String descripcion;

    // Constructor vacío
    public Categoria() {
        super();
    }

    // Constructor completo
    public Categoria(Long id, String nombre, String descripcion) {
        super(id);
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    // Getters y Setters
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    // El toString() nos va a servir muchísimo para los listados de consola
    @Override
    public String toString() {
        return "ID: " + getId() + " | Categoría: " + nombre + " (" + descripcion + ")";
    }
}