package crudcodile.model;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ModeloSQLite {

    public static final String COLUMNA_ID_LOCAL = "__crudcodile_id";

    private final Path rutaBaseDatos;
    private final String connectionString;

    public ModeloSQLite() throws SQLException {
        try {
            Path carpeta = Paths.get("data");
            Files.createDirectories(carpeta);
            rutaBaseDatos = carpeta.resolve("crudcodile.db").toAbsolutePath();
        } catch (IOException ex) {
            throw new SQLException("No fue posible crear la carpeta local data.", ex);
        }

        connectionString = "jdbc:sqlite:" + rutaBaseDatos;

        try (Connection ignored = conectar()) {
        }
    }

    private Connection conectar() throws SQLException {
        return DriverManager.getConnection(connectionString);
    }

    public String getRutaBaseDatos() {
        return rutaBaseDatos.toString();
    }


    public List<String> listarTablasLocales() throws SQLException {
        List<String> tablas = new ArrayList<>();
        String sql = "SELECT name FROM sqlite_master "
                + "WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name";

        try (Connection conexion = conectar();
             Statement sentencia = conexion.createStatement();
             ResultSet resultado = sentencia.executeQuery(sql)) {

            while (resultado.next()) {
                tablas.add(resultado.getString("name"));
            }
        }

        return tablas;
    }

    public boolean existeTabla(String nombreTabla) throws SQLException {
        String sql = "SELECT name FROM sqlite_master WHERE type='table' AND name=?";

        try (Connection conexion = conectar();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {

            sentencia.setString(1, nombreTabla);

            try (ResultSet resultado = sentencia.executeQuery()) {
                return resultado.next();
            }
        }
    }

    public void guardarCopia(DatosTabla datosTabla) throws SQLException {
        validarNombreInterno(datosTabla);

        try (Connection conexion = conectar()) {
            conexion.setAutoCommit(false);

            try {
                eliminarTabla(conexion, datosTabla.getNombreTabla());
                crearTabla(conexion, datosTabla);
                insertarFilas(conexion, datosTabla);
                conexion.commit();
            } catch (SQLException ex) {
                conexion.rollback();
                throw ex;
            } finally {
                conexion.setAutoCommit(true);
            }
        }
    }

    public DatosTabla leerTabla(String nombreTabla) throws SQLException {
        if (!existeTabla(nombreTabla)) {
            throw new SQLException("La tabla '" + nombreTabla + "' no existe en SQLite.");
        }

        List<ColumnaTabla> columnas = obtenerColumnas(nombreTabla, true);
        List<List<Object>> filas = new ArrayList<>();
        String sql = "SELECT * FROM " + escaparSQLite(nombreTabla);

        try (Connection conexion = conectar();
             Statement sentencia = conexion.createStatement();
             ResultSet resultado = sentencia.executeQuery(sql)) {

            while (resultado.next()) {
                List<Object> fila = new ArrayList<>();

                for (ColumnaTabla columna : columnas) {
                    Object valor = resultado.getObject(columna.getNombre());
                    fila.add(valor instanceof byte[] bytes
                            ? new String(bytes, StandardCharsets.UTF_8)
                            : valor);
                }

                filas.add(fila);
            }
        }

        return new DatosTabla(nombreTabla, columnas, filas);
    }

    public List<ColumnaTabla> obtenerColumnasEditables(String nombreTabla) throws SQLException {
        return obtenerColumnas(nombreTabla, false);
    }

    public void insertar(String tabla, Map<String, String> valores) throws SQLException {
        List<ColumnaTabla> columnas = obtenerColumnasEditables(tabla);

        StringBuilder nombres = new StringBuilder();
        StringBuilder parametros = new StringBuilder();

        for (int i = 0; i < columnas.size(); i++) {
            if (i > 0) {
                nombres.append(", ");
                parametros.append(", ");
            }

            nombres.append(escaparSQLite(columnas.get(i).getNombre()));
            parametros.append("?");
        }

        String sql = "INSERT INTO " + escaparSQLite(tabla)
                + " (" + nombres + ") VALUES (" + parametros + ")";

        try (Connection conexion = conectar();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {

            colocarParametros(sentencia, columnas, valores);
            sentencia.executeUpdate();
        }
    }

    public void actualizar(String tabla, long idLocal, Map<String, String> valores) throws SQLException {
        List<ColumnaTabla> columnas = obtenerColumnasEditables(tabla);
        StringBuilder asignaciones = new StringBuilder();

        for (int i = 0; i < columnas.size(); i++) {
            if (i > 0) {
                asignaciones.append(", ");
            }

            asignaciones.append(escaparSQLite(columnas.get(i).getNombre())).append(" = ?");
        }

        String sql = "UPDATE " + escaparSQLite(tabla)
                + " SET " + asignaciones
                + " WHERE " + escaparSQLite(COLUMNA_ID_LOCAL) + " = ?";

        try (Connection conexion = conectar();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {

            colocarParametros(sentencia, columnas, valores);
            sentencia.setLong(columnas.size() + 1, idLocal);

            if (sentencia.executeUpdate() == 0) {
                throw new SQLException("No se encontro el registro local a actualizar.");
            }
        }
    }

    public void eliminar(String tabla, long idLocal) throws SQLException {
        String sql = "DELETE FROM " + escaparSQLite(tabla)
                + " WHERE " + escaparSQLite(COLUMNA_ID_LOCAL) + " = ?";

        try (Connection conexion = conectar();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {

            sentencia.setLong(1, idLocal);

            if (sentencia.executeUpdate() == 0) {
                throw new SQLException("No se encontro el registro local a eliminar.");
            }
        }
    }


    public ResultadoSQL ejecutarSql(String sql) throws SQLException {
        validarComandoCrud(sql);

        try (Connection conexion = conectar();
             Statement sentencia = conexion.createStatement()) {

            boolean devuelveFilas = sentencia.execute(sql);

            if (devuelveFilas) {
                try (ResultSet resultado = sentencia.getResultSet()) {
                    ResultSetMetaData meta = resultado.getMetaData();
                    List<String> columnas = new ArrayList<>();
                    List<List<Object>> filas = new ArrayList<>();

                    for (int i = 1; i <= meta.getColumnCount(); i++) {
                        columnas.add(meta.getColumnLabel(i));
                    }

                    while (resultado.next()) {
                        List<Object> fila = new ArrayList<>();

                        for (int i = 1; i <= meta.getColumnCount(); i++) {
                            Object valor = resultado.getObject(i);

                            if (valor instanceof byte[] bytes) {
                                valor = "[BLOB " + bytes.length + " bytes]";
                            }

                            fila.add(valor);
                        }

                        filas.add(fila);
                    }

                    return new ResultadoSQL(
                            true,
                            columnas,
                            filas,
                            -1,
                            "Consulta completada: " + filas.size() + " fila(s)."
                    );
                }
            }

            int filasAfectadas = sentencia.getUpdateCount();
            String mensaje = filasAfectadas >= 0
                    ? "Sentencia ejecutada: " + filasAfectadas + " fila(s) afectada(s)."
                    : "Sentencia ejecutada correctamente.";

            return new ResultadoSQL(
                    false,
                    new ArrayList<>(),
                    new ArrayList<>(),
                    filasAfectadas,
                    mensaje
            );
        }
    }

    public static String validarComandoCrud(String sql) throws SQLException {
        if (sql == null || sql.isBlank()) {
            throw new SQLException("La sentencia SQL esta vacia.");
        }

        String normalizado = sql.stripLeading().toUpperCase();
        String comando = normalizado.split("\\s+", 2)[0];

        if (!comando.equals("SELECT")
                && !comando.equals("INSERT")
                && !comando.equals("UPDATE")
                && !comando.equals("DELETE")) {
            throw new SQLException(
                    "Comando no permitido. La consola solo acepta SELECT, INSERT, UPDATE y DELETE."
            );
        }

        return comando;
    }

    public Map<String, String> obtenerValoresEditables(
            DatosTabla datosTabla,
            List<Object> filaSeleccionada
    ) {
        Map<String, String> valores = new LinkedHashMap<>();

        for (int i = 0; i < datosTabla.getColumnas().size(); i++) {
            ColumnaTabla columna = datosTabla.getColumnas().get(i);

            if (COLUMNA_ID_LOCAL.equals(columna.getNombre())) {
                continue;
            }

            Object valor = filaSeleccionada.get(i);
            valores.put(columna.getNombre(), valor == null ? "" : valor.toString());
        }

        return valores;
    }

    private void eliminarTabla(Connection conexion, String nombreTabla) throws SQLException {
        try (Statement sentencia = conexion.createStatement()) {
            sentencia.executeUpdate("DROP TABLE IF EXISTS " + escaparSQLite(nombreTabla));
        }
    }

    private void crearTabla(Connection conexion, DatosTabla datosTabla) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE ")
                .append(escaparSQLite(datosTabla.getNombreTabla()))
                .append(" (")
                .append(escaparSQLite(COLUMNA_ID_LOCAL))
                .append(" INTEGER PRIMARY KEY AUTOINCREMENT");

        for (ColumnaTabla columna : datosTabla.getColumnas()) {
            sql.append(", ")
                    .append(escaparSQLite(columna.getNombre()))
                    .append(" ")
                    .append(columna.getTipoSQLite());

            if (!columna.isPermiteNulo()) {
                sql.append(" NOT NULL");
            }
        }

        sql.append(")");

        try (Statement sentencia = conexion.createStatement()) {
            sentencia.executeUpdate(sql.toString());
        }
    }

    private void insertarFilas(Connection conexion, DatosTabla datosTabla) throws SQLException {
        if (datosTabla.getColumnas().isEmpty()) {
            return;
        }

        StringBuilder nombres = new StringBuilder();
        StringBuilder parametros = new StringBuilder();

        for (int i = 0; i < datosTabla.getColumnas().size(); i++) {
            if (i > 0) {
                nombres.append(", ");
                parametros.append(", ");
            }

            nombres.append(escaparSQLite(datosTabla.getColumnas().get(i).getNombre()));
            parametros.append("?");
        }

        String sql = "INSERT INTO " + escaparSQLite(datosTabla.getNombreTabla())
                + " (" + nombres + ") VALUES (" + parametros + ")";

        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            for (List<Object> fila : datosTabla.getFilas()) {
                for (int i = 0; i < fila.size(); i++) {
                    sentencia.setObject(i + 1, normalizarValorRemoto(fila.get(i)));
                }

                sentencia.addBatch();
            }

            sentencia.executeBatch();
        }
    }

    private Object normalizarValorRemoto(Object valor) {
        if (valor instanceof java.sql.Date
                || valor instanceof java.sql.Time
                || valor instanceof Timestamp) {
            return valor.toString();
        }

        if (valor instanceof BigDecimal decimal) {
            return decimal.doubleValue();
        }

        if (valor instanceof Boolean booleano) {
            return booleano ? 1 : 0;
        }

        return valor;
    }

    private List<ColumnaTabla> obtenerColumnas(String nombreTabla, boolean incluirIdLocal) throws SQLException {
        List<ColumnaTabla> columnas = new ArrayList<>();
        String sql = "PRAGMA table_info(" + escaparSQLite(nombreTabla) + ")";

        try (Connection conexion = conectar();
             Statement sentencia = conexion.createStatement();
             ResultSet resultado = sentencia.executeQuery(sql)) {

            while (resultado.next()) {
                String nombre = resultado.getString("name");

                if (!incluirIdLocal && COLUMNA_ID_LOCAL.equals(nombre)) {
                    continue;
                }

                String tipo = resultado.getString("type");
                boolean permiteNulo = resultado.getInt("notnull") == 0;
                columnas.add(new ColumnaTabla(nombre, tipo, permiteNulo));
            }
        }

        return columnas;
    }

    private void colocarParametros(
            PreparedStatement sentencia,
            List<ColumnaTabla> columnas,
            Map<String, String> valores
    ) throws SQLException {

        for (int i = 0; i < columnas.size(); i++) {
            ColumnaTabla columna = columnas.get(i);
            String texto = valores.get(columna.getNombre());
            Object valor = convertirTexto(texto, columna);
            sentencia.setObject(i + 1, valor);
        }
    }

    private Object convertirTexto(String texto, ColumnaTabla columna) throws SQLException {
        if (texto == null || texto.isBlank()) {
            if (!columna.isPermiteNulo()) {
                throw new SQLException(
                        "La columna '" + columna.getNombre() + "' no puede quedar vacia."
                );
            }

            return null;
        }

        try {
            return switch (columna.getTipoSQLite().toUpperCase()) {
                case "INTEGER" -> Long.parseLong(texto);
                case "REAL" -> Double.parseDouble(texto);
                case "BLOB" -> texto.getBytes(StandardCharsets.UTF_8);
                default -> texto;
            };
        } catch (NumberFormatException ex) {
            throw new SQLException(
                    "El valor de '" + columna.getNombre()
                            + "' no coincide con el tipo " + columna.getTipoSQLite() + "."
            );
        }
    }

    private void validarNombreInterno(DatosTabla datosTabla) throws SQLException {
        for (ColumnaTabla columna : datosTabla.getColumnas()) {
            if (COLUMNA_ID_LOCAL.equalsIgnoreCase(columna.getNombre())) {
                throw new SQLException(
                        "La tabla remota usa el nombre reservado " + COLUMNA_ID_LOCAL + "."
                );
            }
        }
    }

    private String escaparSQLite(String identificador) {
        return "\"" + identificador.replace("\"", "\"\"") + "\"";
    }
}
