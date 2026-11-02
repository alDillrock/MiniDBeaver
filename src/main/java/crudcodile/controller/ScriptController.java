package crudcodile.controller;

import crudcodile.model.ModeloSQLite;
import crudcodile.model.ModeloScript;
import crudcodile.model.ResultadoSQL;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class ScriptController {

    @FXML private TextArea areaSql;
    @FXML private TableView<ObservableList<Object>> tablaResultado;
    @FXML private Label labelArchivo;
    @FXML private Label labelResultado;
    @FXML private Button botonEjecutar;

    private ModeloSQLite modeloSQLite;
    private ModeloScript modeloScript;
    private Path archivoActual;
    private Runnable alModificarBaseLocal;

    public void configurar(
            ModeloSQLite modeloSQLite,
            ModeloScript modeloScript,
            Path archivoInicial,
            Runnable alModificarBaseLocal
    ) {
        this.modeloSQLite = modeloSQLite;
        this.modeloScript = modeloScript;
        this.alModificarBaseLocal = alModificarBaseLocal;

        if (archivoInicial != null) {
            cargarArchivo(archivoInicial);
        } else {
            labelArchivo.setText("Nuevo script sin guardar");
        }
    }

    @FXML
    private void abrirScript() {
        FileChooser selector = crearSelector("Abrir script SQL");
        File archivo = selector.showOpenDialog(obtenerVentana());

        if (archivo != null) {
            cargarArchivo(archivo.toPath());
        }
    }

    @FXML
    private void guardarScript() {
        if (archivoActual == null) {
            guardarComo();
            return;
        }

        guardarEn(archivoActual);
    }

    @FXML
    private void guardarComo() {
        FileChooser selector = crearSelector("Guardar script SQL");
        selector.setInitialFileName(
                archivoActual == null ? "nuevo_script.sql" : archivoActual.getFileName().toString()
        );

        File archivo = selector.showSaveDialog(obtenerVentana());

        if (archivo != null) {
            guardarEn(archivo.toPath());
        }
    }

    @FXML
    private void ejecutarSql() {
        String sql = areaSql.getText().trim();

        if (sql.isBlank()) {
            mostrarError("Escribe una sentencia SQL antes de ejecutarla.");
            return;
        }

        if (contieneVariasSentencias(sql)) {
            mostrarError("Ejecuta una sentencia SQL por vez.");
            return;
        }

        try {
            ModeloSQLite.validarComandoCrud(sql);
        } catch (SQLException ex) {
            mostrarError(ex.getMessage());
            return;
        }

        if (esOperacionPeligrosa(sql)
                && !confirmar("La sentencia puede modificar muchos registros locales.\n\n¿Deseas ejecutarla?")) {
            return;
        }

        botonEjecutar.setDisable(true);
        labelResultado.setText("Ejecutando sobre SQLite local...");

        Task<ResultadoSQL> tarea = new Task<>() {
            @Override
            protected ResultadoSQL call() throws Exception {
                return modeloSQLite.ejecutarSql(sql);
            }
        };

        tarea.setOnSucceeded(event -> {
            botonEjecutar.setDisable(false);
            ResultadoSQL resultado = tarea.getValue();
            mostrarResultado(resultado);

            if (!resultado.isConsulta() && alModificarBaseLocal != null) {
                alModificarBaseLocal.run();
            }
        });

        tarea.setOnFailed(event -> {
            botonEjecutar.setDisable(false);
            Throwable error = tarea.getException();
            labelResultado.setText("La sentencia no se pudo ejecutar.");
            mostrarError(error == null ? "Error desconocido." : error.getMessage());
        });

        Thread hilo = new Thread(tarea, "crudcodile-script-sql");
        hilo.setDaemon(true);
        hilo.start();
    }

    private void cargarArchivo(Path archivo) {
        try {
            areaSql.setText(modeloScript.leer(archivo));
            archivoActual = archivo.toAbsolutePath();
            labelArchivo.setText(archivoActual.toString());
            labelResultado.setText("Script cargado. Listo para editar o ejecutar.");
        } catch (Exception ex) {
            mostrarError("No fue posible abrir el script.\n" + ex.getMessage());
        }
    }

    private void guardarEn(Path archivo) {
        try {
            archivoActual = modeloScript.guardar(archivo, areaSql.getText());
            labelArchivo.setText(archivoActual.toString());
            labelResultado.setText("Script guardado correctamente.");
        } catch (Exception ex) {
            mostrarError("No fue posible guardar el script.\n" + ex.getMessage());
        }
    }

    private void mostrarResultado(ResultadoSQL resultado) {
        tablaResultado.getColumns().clear();
        tablaResultado.getItems().clear();
        labelResultado.setText(resultado.getMensaje());

        if (!resultado.isConsulta()) {
            return;
        }

        for (int i = 0; i < resultado.getColumnas().size(); i++) {
            final int indice = i;
            String nombreColumna = resultado.getColumnas().get(i);

            TableColumn<ObservableList<Object>, String> columna = new TableColumn<>(nombreColumna);
            columna.setCellValueFactory(celda -> {
                Object valor = celda.getValue().get(indice);
                return new SimpleStringProperty(valor == null ? "NULL" : valor.toString());
            });
            columna.setPrefWidth(Math.max(120, nombreColumna.length() * 11.0));
            tablaResultado.getColumns().add(columna);
        }

        ObservableList<ObservableList<Object>> filas = FXCollections.observableArrayList();

        for (List<Object> fila : resultado.getFilas()) {
            filas.add(FXCollections.observableArrayList(fila));
        }

        tablaResultado.setItems(filas);
    }

    private FileChooser crearSelector(String titulo) {
        FileChooser selector = new FileChooser();
        selector.setTitle(titulo);
        selector.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Scripts SQL (*.sql)", "*.sql")
        );

        File carpeta = modeloScript.getCarpetaScripts().toFile();
        if (carpeta.isDirectory()) {
            selector.setInitialDirectory(carpeta);
        }

        return selector;
    }

    private boolean contieneVariasSentencias(String sql) {
        boolean comillaSimple = false;
        boolean comillaDoble = false;
        int sentencias = 0;
        StringBuilder actual = new StringBuilder();

        for (int i = 0; i < sql.length(); i++) {
            char caracter = sql.charAt(i);

            if (caracter == '\'' && !comillaDoble) {
                if (comillaSimple && i + 1 < sql.length() && sql.charAt(i + 1) == '\'') {
                    actual.append(caracter).append(sql.charAt(++i));
                    continue;
                }

                comillaSimple = !comillaSimple;
            } else if (caracter == '"' && !comillaSimple) {
                if (comillaDoble && i + 1 < sql.length() && sql.charAt(i + 1) == '"') {
                    actual.append(caracter).append(sql.charAt(++i));
                    continue;
                }

                comillaDoble = !comillaDoble;
            }

            if (caracter == ';' && !comillaSimple && !comillaDoble) {
                if (!actual.toString().trim().isEmpty()) {
                    sentencias++;
                }
                actual.setLength(0);

                if (sentencias > 1) {
                    return true;
                }
            } else {
                actual.append(caracter);
            }
        }

        if (!actual.toString().trim().isEmpty()) {
            sentencias++;
        }

        return sentencias > 1;
    }

    private boolean esOperacionPeligrosa(String sql) {
        String normalizado = sql.trim().toLowerCase().replaceAll("\\s+", " ");

        if (normalizado.startsWith("delete ") && !normalizado.contains(" where ")) {
            return true;
        }

        return normalizado.startsWith("update ") && !normalizado.contains(" where ");
    }

    private boolean confirmar(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION);
        alerta.setTitle("CRUDCODILE - SQL");
        alerta.setHeaderText("Confirmar sentencia");
        alerta.setContentText(mensaje);

        Optional<ButtonType> resultado = alerta.showAndWait();
        return resultado.isPresent() && resultado.get() == ButtonType.OK;
    }

    private Window obtenerVentana() {
        return areaSql.getScene().getWindow();
    }

    private void mostrarError(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.ERROR);
        alerta.setTitle("CRUDCODILE - SQL");
        alerta.setHeaderText("Ocurrio un problema");
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}
