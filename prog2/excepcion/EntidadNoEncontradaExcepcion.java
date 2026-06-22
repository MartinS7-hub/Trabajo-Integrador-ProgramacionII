package integrado.prog2.excepcion;

public class EntidadNoEncontradaExcepcion extends RuntimeException {
    public EntidadNoEncontradaExcepcion(String mensaje) {
        super(mensaje);
    }
}