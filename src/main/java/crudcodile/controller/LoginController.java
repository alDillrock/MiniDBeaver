package crudcodile.controller;

import crudcodile.Main;
import crudcodile.model.ConfiguracionConexion;
import crudcodile.model.ConexionSSH;
import crudcodile.model.ModeloMariaDB;
import crudcodile.model.ModeloSQLite;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.util.List;

public class LoginController {

    @FXML private TextField fieldSshHost;
    @FXML private TextField fieldSshPort;
    @FXML private TextField fieldSshUsuario;
    @FXML private PasswordField fieldSshPassword;

    @FXML private TextField fieldDbHost;
    @FXML private TextField fieldDbPort;
    @FXML private TextField fieldDbUsuario;
    @FXML private PasswordField fieldDbPassword;

    @FXML private Button botonConectar;
    @FXML private Button botonAccesoLocal;
    @FXML private Label labelEstado;

    private ConexionSSH conexionSSH;
    private ModeloMariaDB modeloMariaDB;
    private ModeloSQLite modeloSQLite;

    @FXML
    private void initialize() {
        fieldSshPort.setText("22");
        fieldDbHost.setText("127.0.0.1");
        fieldDbPort.setText("3306");
        labelEstado.setText("Esquema destino: its5a");
    }

    @FXML
    private void mostrarIntegrantes() {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setTitle("CRUDCODILE");
        alerta.setHeaderText("Integrantes del equipo");
        alerta.setContentText(
                "Ricardo Castillo\n"
                        + "Alan Campos\n"
                        + "Emmanuel Mora\n"
                        + "Fernanda Suarez\n"
                        + "Lucia Jaramillo"
        );
        alerta.showAndWait();
    }

    @FXML
    private void abrirBaseLocal() {
        try {
            ModeloSQLite sqlite = new ModeloSQLite();
            List<String> tablasLocales = sqlite.listarTablasLocales();
            Main.mostrarPrincipalLocal(sqlite, tablasLocales);
        } catch (Exception ex) {
            mostrarError("No fue posible abrir la base SQLite local.\n" + ex.getMessage());
        }
    }

    @FXML
    private void conectar() {
        ConfiguracionConexion configuracion;

        try {
            configuracion = obtenerConfiguracion();
            conexionSSH = new ConexionSSH();
            modeloMariaDB = new ModeloMariaDB();
            modeloSQLite = new ModeloSQLite();
        } catch (Exception ex) {
            mostrarError(ex.getMessage());
            return;
        }

        setConectando(true);

        Task<List<String>> tarea = new Task<>() {
            @Override
            protected List<String> call() throws Exception {
                updateMessage("Abriendo tunel SSH...");
                int puertoLocal = conexionSSH.abrirTunel(configuracion);

                updateMessage("Conectando con MariaDB / its5a...");
                modeloMariaDB.conectar(puertoLocal, configuracion);

                updateMessage("Consultando tablas del esquema...");
                return modeloMariaDB.listarTablas();
            }
        };

        labelEstado.textProperty().bind(tarea.messageProperty());

        tarea.setOnSucceeded(event -> {
            labelEstado.textProperty().unbind();

            try {
                Main.mostrarPrincipal(
                        modeloMariaDB,
                        modeloSQLite,
                        conexionSSH,
                        tarea.getValue()
                );
            } catch (Exception ex) {
                cerrarConexiones();
                setConectando(false);
                mostrarError("No fue posible abrir la ventana principal.\n" + ex.getMessage());
            }
        });

        tarea.setOnFailed(event -> {
            labelEstado.textProperty().unbind();
            cerrarConexiones();
            setConectando(false);

            Throwable error = tarea.getException();
            mostrarError("No fue posible establecer la conexion.\n\n"
                    + (error == null ? "Error desconocido." : error.getMessage()));
        });

        Thread hilo = new Thread(tarea, "crudcodile-login");
        hilo.setDaemon(true);
        hilo.start();
    }

    private ConfiguracionConexion obtenerConfiguracion() {
        String sshHost = fieldSshHost.getText().trim();
        String sshUsuario = fieldSshUsuario.getText().trim();
        String sshPassword = fieldSshPassword.getText();

        String dbHost = fieldDbHost.getText().trim();
        String dbUsuario = fieldDbUsuario.getText().trim();
        String dbPassword = fieldDbPassword.getText();

        if (sshHost.isBlank() || sshUsuario.isBlank() || sshPassword.isBlank()
                || dbHost.isBlank() || dbUsuario.isBlank() || dbPassword.isBlank()) {
            throw new IllegalArgumentException("Completa todos los datos SSH y MariaDB.");
        }

        return new ConfiguracionConexion(
                sshHost,
                convertirPuerto(fieldSshPort.getText(), "SSH"),
                sshUsuario,
                sshPassword,
                dbHost,
                convertirPuerto(fieldDbPort.getText(), "MariaDB"),
                dbUsuario,
                dbPassword
        );
    }

    private int convertirPuerto(String texto, String nombre) {
        try {
            int puerto = Integer.parseInt(texto.trim());

            if (puerto < 1 || puerto > 65535) {
                throw new NumberFormatException();
            }

            return puerto;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("El puerto de " + nombre + " no es valido.");
        }
    }

    private void setConectando(boolean conectando) {
        botonConectar.setDisable(conectando);
        botonAccesoLocal.setDisable(conectando);

        if (!conectando) {
            labelEstado.setText("Esquema destino: its5a");
        }
    }

    private void cerrarConexiones() {
        if (modeloMariaDB != null) {
            modeloMariaDB.cerrar();
        }

        if (conexionSSH != null) {
            conexionSSH.cerrar();
        }
    }

    private void mostrarError(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.ERROR);
        alerta.setTitle("CRUDCODILE");
        alerta.setHeaderText("Ocurrio un problema");
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}
