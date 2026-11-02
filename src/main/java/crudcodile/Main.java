package crudcodile;

import crudcodile.controller.PrincipalController;
import crudcodile.controller.ScriptController;
import crudcodile.model.ConexionSSH;
import crudcodile.model.ModeloMariaDB;
import crudcodile.model.ModeloSQLite;
import crudcodile.model.ModeloScript;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class Main extends Application {

    private static Stage escenarioPrincipal;

    @Override
    public void start(Stage stage) throws IOException {
        escenarioPrincipal = stage;
        mostrarLogin();
    }

    public static void mostrarLogin() throws IOException {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/crudcodile/view/login.fxml"));
        Parent root = loader.load();

        Scene scene = crearEscena(root, 980, 680);
        escenarioPrincipal.setTitle("CRUDCODILE - Acceso");
        escenarioPrincipal.setMinWidth(900);
        escenarioPrincipal.setMinHeight(620);
        escenarioPrincipal.setOnCloseRequest(null);
        escenarioPrincipal.setScene(scene);
        escenarioPrincipal.centerOnScreen();
        escenarioPrincipal.show();
    }

    public static void mostrarPrincipal(
            ModeloMariaDB modeloMariaDB,
            ModeloSQLite modeloSQLite,
            ConexionSSH conexionSSH,
            List<String> tablas
    ) throws IOException {

        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/crudcodile/view/principal.fxml"));
        Parent root = loader.load();

        PrincipalController controller = loader.getController();
        controller.configurarRemoto(modeloMariaDB, modeloSQLite, conexionSSH, tablas);

        prepararVentanaPrincipal(root, controller, "CRUDCODILE - MariaDB -> SQLite");
    }

    public static void mostrarPrincipalLocal(
            ModeloSQLite modeloSQLite,
            List<String> tablasLocales
    ) throws IOException {

        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/crudcodile/view/principal.fxml"));
        Parent root = loader.load();

        PrincipalController controller = loader.getController();
        controller.configurarLocal(modeloSQLite, tablasLocales);

        prepararVentanaPrincipal(root, controller, "CRUDCODILE - SQLite local");
    }

    private static void prepararVentanaPrincipal(
            Parent root,
            PrincipalController controller,
            String titulo
    ) {
        Scene scene = crearEscena(root, 1180, 760);
        escenarioPrincipal.setTitle(titulo);
        escenarioPrincipal.setMinWidth(1000);
        escenarioPrincipal.setMinHeight(650);
        escenarioPrincipal.setScene(scene);
        escenarioPrincipal.setOnCloseRequest(event -> controller.cerrarConexiones());
        escenarioPrincipal.centerOnScreen();
        escenarioPrincipal.show();
    }

    public static void mostrarEditorScript(
            ModeloSQLite modeloSQLite,
            ModeloScript modeloScript,
            Path archivoInicial,
            Runnable alModificarBaseLocal,
            Window propietario
    ) throws IOException {

        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/crudcodile/view/script.fxml"));
        Parent root = loader.load();

        ScriptController controller = loader.getController();
        controller.configurar(modeloSQLite, modeloScript, archivoInicial, alModificarBaseLocal);

        Stage stage = new Stage();
        stage.setTitle("CRUDCODILE - Editor SQL local");
        stage.setScene(crearEscena(root, 1050, 720));
        stage.setMinWidth(850);
        stage.setMinHeight(600);

        if (propietario != null) {
            stage.initOwner(propietario);
        }

        stage.centerOnScreen();
        stage.show();
    }

    private static Scene crearEscena(Parent root, double ancho, double alto) {
        Scene scene = new Scene(root, ancho, alto);
        scene.getStylesheets().add(
                Main.class.getResource("/crudcodile/css/crudcodile.css").toExternalForm()
        );
        return scene;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
