package crudcodile.model;

public class ColumnaTabla {

    private final String nombre;
    private final String tipoSQLite;
    private final boolean permiteNulo;

    public ColumnaTabla(String nombre, String tipoSQLite, boolean permiteNulo) {
        this.nombre = nombre;
        this.tipoSQLite = tipoSQLite;
        this.permiteNulo = permiteNulo;
    }

    public String getNombre() {
        return nombre;
    }

    public String getTipoSQLite() {
        return tipoSQLite;
    }

    public boolean isPermiteNulo() {
        return permiteNulo;
    }
}
