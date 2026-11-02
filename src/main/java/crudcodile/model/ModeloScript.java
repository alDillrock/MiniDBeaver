package crudcodile.model;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ModeloScript {

    private final Path carpetaScripts;

    public ModeloScript() throws IOException {
        carpetaScripts = Paths.get("scripts").toAbsolutePath();
        Files.createDirectories(carpetaScripts);
    }

    public Path getCarpetaScripts() {
        return carpetaScripts;
    }

    public String leer(Path archivo) throws IOException {
        validarArchivo(archivo);
        return Files.readString(archivo, StandardCharsets.UTF_8);
    }

    public Path guardar(Path archivo, String contenido) throws IOException {
        if (archivo == null) {
            throw new IOException("No se selecciono un archivo para guardar.");
        }

        Path destino = agregarExtensionSql(archivo.toAbsolutePath());
        Path carpeta = destino.getParent();

        if (carpeta != null) {
            Files.createDirectories(carpeta);
        }

        Files.writeString(destino, contenido, StandardCharsets.UTF_8);
        return destino;
    }

    private void validarArchivo(Path archivo) throws IOException {
        if (archivo == null || !Files.isRegularFile(archivo)) {
            throw new IOException("El script seleccionado no existe.");
        }
    }

    private Path agregarExtensionSql(Path archivo) {
        String nombre = archivo.getFileName().toString();

        if (nombre.toLowerCase().endsWith(".sql")) {
            return archivo;
        }

        return archivo.resolveSibling(nombre + ".sql");
    }
}
