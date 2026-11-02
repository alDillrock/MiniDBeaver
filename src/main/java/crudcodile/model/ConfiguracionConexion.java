package crudcodile.model;

public class ConfiguracionConexion {

    private final String sshHost;
    private final int sshPort;
    private final String sshUsuario;
    private final String sshPassword;

    private final String dbHost;
    private final int dbPort;
    private final String dbUsuario;
    private final String dbPassword;
    private final String dbNombre = "its5a";

    public ConfiguracionConexion(
            String sshHost,
            int sshPort,
            String sshUsuario,
            String sshPassword,
            String dbHost,
            int dbPort,
            String dbUsuario,
            String dbPassword
    ) {
        this.sshHost = sshHost;
        this.sshPort = sshPort;
        this.sshUsuario = sshUsuario;
        this.sshPassword = sshPassword;
        this.dbHost = dbHost;
        this.dbPort = dbPort;
        this.dbUsuario = dbUsuario;
        this.dbPassword = dbPassword;
    }

    public String getSshHost() {
        return sshHost;
    }

    public int getSshPort() {
        return sshPort;
    }

    public String getSshUsuario() {
        return sshUsuario;
    }

    public String getSshPassword() {
        return sshPassword;
    }

    public String getDbHost() {
        return dbHost;
    }

    public int getDbPort() {
        return dbPort;
    }

    public String getDbUsuario() {
        return dbUsuario;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public String getDbNombre() {
        return dbNombre;
    }
}
