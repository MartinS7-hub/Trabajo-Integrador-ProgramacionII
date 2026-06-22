package integrado.prog2.excepcion;

public class StockInvalidoExcepcion extends RuntimeException {
    public StockInvalidoExcepcion(String mensaje) {
        super(mensaje);
    }
}