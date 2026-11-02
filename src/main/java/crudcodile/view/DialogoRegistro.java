package crudcodile.view;

import crudcodile.Main;
import crudcodile.model.ColumnaTabla;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DialogoRegistro {

    public Optional<Map<String, String>> mostrar(
            Window propietario,
            String titulo,
            List<ColumnaTabla> columnas,
            Map<String, String> valoresIniciales
    ) {
        Dialog<Map<String, String>> dialogo = new Dialog<>();
        dialogo.initOwner(propietario);
        dialogo.setTitle(titulo);
        dialogo.setHeaderText("Datos del registro local");

        ButtonType guardar = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().addAll(guardar, ButtonType.CANCEL);

        GridPane formulario = new GridPane();
        formulario.setHgap(14);
        formulario.setVgap(10);
        formulario.setPadding(new Insets(8));

        Map<String, TextField> campos = new LinkedHashMap<>();

        for (int i = 0; i < columnas.size(); i++) {
            ColumnaTabla columna = columnas.get(i);
            String obligatorio = columna.isPermiteNulo() ? "" : " *";

            Label label = new Label(
                    columna.getNombre() + " (" + columna.getTipoSQLite() + ")" + obligatorio
            );
            TextField campo = new TextField(valoresIniciales.getOrDefault(columna.getNombre(), ""));
            campo.setPrefColumnCount(28);

            campos.put(columna.getNombre(), campo);
            formulario.add(label, 0, i);
            formulario.add(campo, 1, i);
        }

        ScrollPane scroll = new ScrollPane(formulario);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(Math.min(430, 80 + columnas.size() * 48));
        dialogo.getDialogPane().setContent(scroll);
        dialogo.getDialogPane().setPrefWidth(620);

        String css = Main.class.getResource("/crudcodile/css/crudcodile.css").toExternalForm();
        dialogo.getDialogPane().getStylesheets().add(css);
        dialogo.getDialogPane().getStyleClass().add("dialog-dark");

        Node botonGuardar = dialogo.getDialogPane().lookupButton(guardar);
        botonGuardar.getStyleClass().add("button-primary");

        dialogo.setResultConverter(boton -> {
            if (boton != guardar) {
                return null;
            }

            Map<String, String> resultado = new LinkedHashMap<>();

            for (ColumnaTabla columna : columnas) {
                resultado.put(columna.getNombre(), campos.get(columna.getNombre()).getText());
            }

            return resultado;
        });

        return dialogo.showAndWait();
    }
}
