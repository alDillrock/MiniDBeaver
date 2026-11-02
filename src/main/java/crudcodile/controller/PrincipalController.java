package crudcodile.controller;

import crudcodile.Main;
import crudcodile.model.ColumnaTabla;
import crudcodile.model.ConexionSSH;
import crudcodile.model.DatosTabla;
import crudcodile.model.ModeloMariaDB;
import crudcodile.model.ModeloSQLite;
import crudcodile.model.ModeloScript;
import crudcodile.view.DialogoRegistro;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PrincipalController {

    @FXML private ListView<String> listaTablas;
    @FXML private TableView<ObservableList<Object>> tablaDatos;

    @FXML private Button botonDescargar;
    @FXML private Button botonAbrirLocal;
    @FXML private Button botonNuevo;
    @FXML private Button botonEditar;
    @FXML private Button botonEliminar;
    @FXML private Button botonScript;
    @FXML private Button botonAbrirScript;

    @FXML private Label labelTablaActual;
    @FXML private Label labelEstado;
    @FXML private Label labelConexion;
    @FXML private Label labelOrigenTablas;
    @FXML private Label labelEsquemaOrigen;

    private ModeloMariaDB modeloMariaDB;
    private ModeloSQLite modeloSQLite;
    private ConexionSSH conexionSSH;
    private ModeloScript modeloScript;

    private String tablaLocalActual;
    private DatosTabla datosLocalesActuales;
    private boolean modoLocal;

    public void configurarRemoto(
            ModeloMariaDB modeloMariaDB,
            ModeloSQLite modeloSQLite,
            ConexionSSH conexionSSH,
            List<String> tablasRemotas
    ) {
        this.modeloMariaDB = modeloMariaDB;
        this.modeloSQLite = modeloSQLite;
        this.conexionSSH = conexionSSH;
        this.modoLocal = false;

        prepararModeloScript();

        listaTablas.setItems(FXCollections.observableArrayList(tablasRemotas));
        labelOrigenTablas.setText("TABLAS REMOTAS");
        labelEsquemaOrigen.setText("Esquema: its5a");
        labelConexion.setText("SSH: conectado | MariaDB: its5a | " + tablasRemotas.size() + " tablas");
        labelEstado.setText("SQLite local: " + modeloSQLite.getRutaBaseDatos());

        botonDescargar.setVisible(true);
        botonDescargar.setManaged(true);
        botonAbrirLocal.setText("Abrir copia local");
    }

    public void configurarLocal(
            ModeloSQLite modeloSQLite,
            List<String> tablasLocales
    ) {
        this.modeloSQLite = modeloSQLite;
        this.modeloMariaDB = null;
        this.conexionSSH = null;
        this.modoLocal = true;

        prepararModeloScript();

        listaTablas.setItems(FXCollections.observableArrayList(tablasLocales));
        labelOrigenTablas.setText("TABLAS LOCALES");
        labelEsquemaOrigen.setText("Archivo: crudcodile.db");
        labelConexion.setText("Modo local | SQLite | " + tablasLocales.size() + " tablas");
        labelEstado.setText("Base local: " + modeloSQLite.getRutaBaseDatos());

        botonDescargar.setVisible(false);
        botonDescargar.setManaged(false);
        botonAbrirLocal.setText("Abrir tabla local");

        if (tablasLocales.isEmpty()) {
            labelTablaActual.setText("No hay tablas locales. Inicia sesion para descargar una copia.");
        }
    }

    private void prepararModeloScript() {
        try {
            modeloScript = new ModeloScript();
        } catch (IOException ex) {
            mostrarError("No fue posible preparar la carpeta de scripts.\n" + ex.getMessage());
        }
    }

    @FXML
    private void descargarTabla() {
        if (modoLocal || modeloMariaDB == null) {
            mostrarError("Para descargar o actualizar tablas debes iniciar sesion.");
            return;
        }

        String tabla = listaTablas.getSelectionModel().getSelectedItem();

        if (tabla == null) {
            mostrarError("Selecciona una tabla remota.");
            return;
        }

        try {
            if (modeloSQLite.existeTabla(tabla)
                    && !confirmar(
                    "Ya existe una copia local de '" + tabla + "'.\n"
                            + "Al descargarla de nuevo se perderan los cambios CRUD locales.\n\n"
                            + "¿Deseas continuar?"
            )) {
                return;
            }
        } catch (SQLException ex) {
            mostrarError(ex.getMessage());
            return;
        }

        setOcupado(true, "Descargando '" + tabla + "' mediante el tunel SSH...");

        Task<DatosTabla> tarea = new Task<>() {
            @Override
            protected DatosTabla call() throws Exception {
                DatosTabla remota = modeloMariaDB.obtenerTabla(tabla);
                modeloSQLite.guardarCopia(remota);
                return modeloSQLite.leerTabla(tabla);
            }
        };

        tarea.setOnSucceeded(event -> {
            tablaLocalActual = tabla;
            mostrarDatos(tarea.getValue());
            setOcupado(false, "Copia local actualizada en " + modeloSQLite.getRutaBaseDatos());
        });

        tarea.setOnFailed(event -> {
            Throwable error = tarea.getException();
            setOcupado(false, "La descarga no se completo.");
            mostrarError(error == null ? "Error desconocido." : error.getMessage());
        });

        Thread hilo = new Thread(tarea, "crudcodile-descarga");
        hilo.setDaemon(true);
        hilo.start();
    }

    @FXML
    private void abrirTablaLocal() {
        String tabla = listaTablas.getSelectionModel().getSelectedItem();

        if (tabla == null) {
            mostrarError("Selecciona una tabla.");
            return;
        }

        try {
            if (!modeloSQLite.existeTabla(tabla)) {
                mostrarError("Esa tabla aun no tiene una copia local. Primero descargala.");
                return;
            }

            tablaLocalActual = tabla;
            refrescarTablaLocal();
            labelEstado.setText("Trabajando localmente con '" + tabla + "'.");
        } catch (SQLException ex) {
            mostrarError(ex.getMessage());
        }
    }

    @FXML
    private void nuevoRegistro() {
        if (!validarTablaLocal()) {
            return;
        }

        try {
            List<ColumnaTabla> columnas = modeloSQLite.obtenerColumnasEditables(tablaLocalActual);
            DialogoRegistro dialogo = new DialogoRegistro();

            Optional<Map<String, String>> resultado = dialogo.mostrar(
                    tablaDatos.getScene().getWindow(),
                    "Nuevo registro - " + tablaLocalActual,
                    columnas,
                    new LinkedHashMap<>()
            );

            if (resultado.isPresent()) {
                modeloSQLite.insertar(tablaLocalActual, resultado.get());
                refrescarTablaLocal();
                labelEstado.setText("Registro insertado en SQLite.");
            }
        } catch (SQLException ex) {
            mostrarError(ex.getMessage());
        }
    }

    @FXML
    private void editarRegistro() {
        if (!validarTablaLocal()) {
            return;
        }

        ObservableList<Object> fila = tablaDatos.getSelectionModel().getSelectedItem();

        if (fila == null) {
            mostrarError("Selecciona una fila para editar.");
            return;
        }

        try {
            long idLocal = obtenerIdLocal(fila);
            List<ColumnaTabla> columnas = modeloSQLite.obtenerColumnasEditables(tablaLocalActual);
            Map<String, String> actuales = modeloSQLite.obtenerValoresEditables(datosLocalesActuales, fila);

            DialogoRegistro dialogo = new DialogoRegistro();
            Optional<Map<String, String>> resultado = dialogo.mostrar(
                    tablaDatos.getScene().getWindow(),
                    "Editar registro - " + tablaLocalActual,
                    columnas,
                    actuales
            );

            if (resultado.isPresent()) {
                modeloSQLite.actualizar(tablaLocalActual, idLocal, resultado.get());
                refrescarTablaLocal();
                labelEstado.setText("Registro actualizado en SQLite.");
            }
        } catch (SQLException ex) {
            mostrarError(ex.getMessage());
        }
    }

    @FXML
    private void eliminarRegistro() {
        if (!validarTablaLocal()) {
            return;
        }

        ObservableList<Object> fila = tablaDatos.getSelectionModel().getSelectedItem();

        if (fila == null) {
            mostrarError("Selecciona una fila para eliminar.");
            return;
        }

        try {
            long idLocal = obtenerIdLocal(fila);

            if (!confirmar("¿Eliminar el registro local con ID " + idLocal + "?")) {
                return;
            }

            modeloSQLite.eliminar(tablaLocalActual, idLocal);
            refrescarTablaLocal();
            labelEstado.setText("Registro eliminado de SQLite.");
        } catch (SQLException ex) {
            mostrarError(ex.getMessage());
        }
    }

    @FXML
    private void nuevoScript() {
        abrirEditorScript(null);
    }

    @FXML
    private void abrirScriptExistente() {
        if (modeloScript == null) {
            mostrarError("La carpeta de scripts no esta disponible.");
            return;
        }

        FileChooser selector = new FileChooser();
        selector.setTitle("Abrir script SQL");
        selector.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Scripts SQL (*.sql)", "*.sql")
        );

        File carpeta = modeloScript.getCarpetaScripts().toFile();
        if (carpeta.isDirectory()) {
            selector.setInitialDirectory(carpeta);
        }

        File archivo = selector.showOpenDialog(tablaDatos.getScene().getWindow());

        if (archivo != null) {
            abrirEditorScript(archivo.toPath());
        }
    }

    @FXML
    private void volverInicio() {
        cerrarConexiones();

        try {
            Main.mostrarLogin();
        } catch (IOException ex) {
            mostrarError("No fue posible volver al inicio.\n" + ex.getMessage());
        }
    }

    private void abrirEditorScript(Path archivoInicial) {
        if (modeloScript == null) {
            mostrarError("La carpeta de scripts no esta disponible.");
            return;
        }

        try {
            Main.mostrarEditorScript(
                    modeloSQLite,
                    modeloScript,
                    archivoInicial,
                    this::actualizarDespuesDeScript,
                    tablaDatos.getScene().getWindow()
            );
        } catch (IOException ex) {
            mostrarError("No fue posible abrir el editor SQL.\n" + ex.getMessage());
        }
    }

    private void actualizarDespuesDeScript() {
        if (tablaLocalActual == null) {
            labelEstado.setText("Sentencia CRUD ejecutada sobre SQLite local.");
            return;
        }

        try {
            if (modeloSQLite.existeTabla(tablaLocalActual)) {
                refrescarTablaLocal();
                labelEstado.setText("SQLite local actualizado desde la consola SQL.");
            }
        } catch (SQLException ex) {
            labelEstado.setText("La base local cambio. Vuelve a abrir la tabla para actualizarla.");
        }
    }

    private void mostrarDatos(DatosTabla datos) {
        datosLocalesActuales = datos;
        tablaDatos.getColumns().clear();

        for (int i = 0; i < datos.getColumnas().size(); i++) {
            final int indice = i;
            ColumnaTabla columna = datos.getColumnas().get(i);

            TableColumn<ObservableList<Object>, String> tableColumn =
                    new TableColumn<>(columna.getNombre());

            tableColumn.setCellValueFactory(celda -> {
                Object valor = celda.getValue().get(indice);
                return new SimpleStringProperty(valor == null ? "NULL" : valor.toString());
            });

            tableColumn.setPrefWidth(Math.max(120, columna.getNombre().length() * 11.0));
            tablaDatos.getColumns().add(tableColumn);
        }

        ObservableList<ObservableList<Object>> filas = FXCollections.observableArrayList();

        for (List<Object> fila : datos.getFilas()) {
            filas.add(FXCollections.observableArrayList(fila));
        }

        tablaDatos.setItems(filas);
        labelTablaActual.setText(
                datos.getNombreTabla() + " - " + datos.getFilas().size() + " registros locales"
        );
    }

    private long obtenerIdLocal(List<Object> fila) throws SQLException {
        if (fila.isEmpty() || fila.get(0) == null) {
            throw new SQLException("No fue posible identificar el registro local.");
        }

        Object id = fila.get(0);

        if (id instanceof Number numero) {
            return numero.longValue();
        }

        try {
            return Long.parseLong(id.toString());
        } catch (NumberFormatException ex) {
            throw new SQLException("El identificador local no es valido.");
        }
    }

    private void refrescarTablaLocal() throws SQLException {
        mostrarDatos(modeloSQLite.leerTabla(tablaLocalActual));
    }

    private boolean validarTablaLocal() {
        if (tablaLocalActual == null || datosLocalesActuales == null) {
            mostrarError("Abre una tabla local antes de utilizar el CRUD.");
            return false;
        }

        return true;
    }

    private void setOcupado(boolean ocupado, String mensaje) {
        botonDescargar.setDisable(ocupado);
        botonAbrirLocal.setDisable(ocupado);
        botonNuevo.setDisable(ocupado);
        botonEditar.setDisable(ocupado);
        botonEliminar.setDisable(ocupado);
        botonScript.setDisable(ocupado);
        botonAbrirScript.setDisable(ocupado);
        labelEstado.setText(mensaje);
    }

    private boolean confirmar(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION);
        alerta.setTitle("CRUDCODILE");
        alerta.setHeaderText("Confirmar operacion");
        alerta.setContentText(mensaje);

        Optional<ButtonType> resultado = alerta.showAndWait();
        return resultado.isPresent() && resultado.get() == ButtonType.OK;
    }

    private void mostrarError(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.ERROR);
        alerta.setTitle("CRUDCODILE");
        alerta.setHeaderText("Ocurrio un problema");
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }

    public void cerrarConexiones() {
        if (modeloMariaDB != null) {
            modeloMariaDB.cerrar();
        }

        if (conexionSSH != null) {
            conexionSSH.cerrar();
        }
    }
}
