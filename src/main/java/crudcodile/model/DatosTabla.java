package crudcodile.model;

import java.util.List;

public class DatosTabla {

    private final String nombreTabla;
    private final List<ColumnaTabla> columnas;
    private final List<List<Object>> filas;

    public DatosTabla(String nombreTabla, List<ColumnaTabla> columnas, List<List<Object>> filas) {
        this.nombreTabla = nombreTabla;
        this.columnas = columnas;
        this.filas = filas;
    }

    public String getNombreTabla() {
        return nombreTabla;
    }

    public List<ColumnaTabla> getColumnas() {
        return columnas;
    }

    public List<List<Object>> getFilas() {
        return filas;
    }
}
