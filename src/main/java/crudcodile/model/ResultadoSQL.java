package crudcodile.model;

import java.util.List;

public class ResultadoSQL {

    private final boolean consulta;
    private final List<String> columnas;
    private final List<List<Object>> filas;
    private final int filasAfectadas;
    private final String mensaje;

    public ResultadoSQL(
            boolean consulta,
            List<String> columnas,
            List<List<Object>> filas,
            int filasAfectadas,
            String mensaje
    ) {
        this.consulta = consulta;
        this.columnas = columnas;
        this.filas = filas;
        this.filasAfectadas = filasAfectadas;
        this.mensaje = mensaje;
    }

    public boolean isConsulta() {
        return consulta;
    }

    public List<String> getColumnas() {
        return columnas;
    }

    public List<List<Object>> getFilas() {
        return filas;
    }

    public int getFilasAfectadas() {
        return filasAfectadas;
    }

    public String getMensaje() {
        return mensaje;
    }
}
