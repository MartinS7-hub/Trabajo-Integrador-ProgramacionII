package integrado.prog2.excepcion;

public class ReglaNegocioExcepcion extends RuntimeException {
    public ReglaNegocioExcepcion(String mensaje) {
        super(mensaje);
    }
}
