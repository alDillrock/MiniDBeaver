package crudcodile.model;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class ModeloMariaDB {

    private Connection conexion;

    public void conectar(int puertoLocal, ConfiguracionConexion configuracion) throws SQLException {
        cerrar();

        String url = "jdbc:mariadb://127.0.0.1:" + puertoLocal
                + "/" + configuracion.getDbNombre();

        conexion = DriverManager.getConnection(
                url,
                configuracion.getDbUsuario(),
                configuracion.getDbPassword()
        );
    }

    public List<String> listarTablas() throws SQLException {
        verificarConexion();

        List<String> tablas = new ArrayList<>();
        DatabaseMetaData metadata = conexion.getMetaData();

        try (ResultSet resultado = metadata.getTables(
                conexion.getCatalog(),
                null,
                "%",
                new String[]{"TABLE"}
        )) {
            while (resultado.next()) {
                tablas.add(resultado.getString("TABLE_NAME"));
            }
        }

        tablas.sort(String.CASE_INSENSITIVE_ORDER);
        return tablas;
    }

    public DatosTabla obtenerTabla(String nombreTabla) throws SQLException {
        verificarConexion();
        validarTablaExistente(nombreTabla);

        String sql = "SELECT * FROM " + escaparMariaDB(nombreTabla);

        try (Statement sentencia = conexion.createStatement();
             ResultSet resultado = sentencia.executeQuery(sql)) {

            ResultSetMetaData metadata = resultado.getMetaData();
            List<ColumnaTabla> columnas = crearColumnas(metadata);
            List<List<Object>> filas = new ArrayList<>();

            while (resultado.next()) {
                List<Object> fila = new ArrayList<>();

                for (int i = 1; i <= metadata.getColumnCount(); i++) {
                    fila.add(resultado.getObject(i));
                }

                filas.add(fila);
            }

            return new DatosTabla(nombreTabla, columnas, filas);
        }
    }

    private List<ColumnaTabla> crearColumnas(ResultSetMetaData metadata) throws SQLException {
        List<ColumnaTabla> columnas = new ArrayList<>();

        for (int i = 1; i <= metadata.getColumnCount(); i++) {
            String nombre = metadata.getColumnLabel(i);
            String tipoSQLite = convertirTipo(metadata.getColumnType(i));
            boolean permiteNulo = metadata.isNullable(i) != ResultSetMetaData.columnNoNulls;

            columnas.add(new ColumnaTabla(nombre, tipoSQLite, permiteNulo));
        }

        return columnas;
    }

    private String convertirTipo(int tipoSql) {
        return switch (tipoSql) {
            case Types.TINYINT, Types.SMALLINT, Types.INTEGER, Types.BIGINT,
                    Types.BIT, Types.BOOLEAN -> "INTEGER";
            case Types.FLOAT, Types.REAL, Types.DOUBLE,
                    Types.NUMERIC, Types.DECIMAL -> "REAL";
            case Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY,
                    Types.BLOB -> "BLOB";
            default -> "TEXT";
        };
    }

    private void validarTablaExistente(String nombreTabla) throws SQLException {
        if (!listarTablas().contains(nombreTabla)) {
            throw new SQLException("La tabla seleccionada no existe en el esquema its5a.");
        }
    }

    private String escaparMariaDB(String identificador) {
        return "`" + identificador.replace("`", "``") + "`";
    }

    private void verificarConexion() throws SQLException {
        if (conexion == null || conexion.isClosed()) {
            throw new SQLException("No existe una conexion activa con MariaDB.");
        }
    }

    public boolean estaConectado() {
        try {
            return conexion != null && !conexion.isClosed();
        } catch (SQLException ex) {
            return false;
        }
    }

    public void cerrar() {
        if (conexion != null) {
            try {
                conexion.close();
            } catch (SQLException ignored) {
            }
        }

        conexion = null;
    }
}
