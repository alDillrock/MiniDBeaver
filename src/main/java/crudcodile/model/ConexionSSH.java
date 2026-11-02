package crudcodile.model;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.util.Properties;

public class ConexionSSH {

    private Session sesion;
    private int puertoLocal;

    public int abrirTunel(ConfiguracionConexion configuracion) throws JSchException {
        cerrar();

        JSch jsch = new JSch();
        sesion = jsch.getSession(
                configuracion.getSshUsuario(),
                configuracion.getSshHost(),
                configuracion.getSshPort()
        );

        sesion.setPassword(configuracion.getSshPassword());

        Properties propiedades = new Properties();
        propiedades.put("StrictHostKeyChecking", "no");
        sesion.setConfig(propiedades);

        sesion.connect(10000);

        puertoLocal = sesion.setPortForwardingL(
                0,
                configuracion.getDbHost(),
                configuracion.getDbPort()
        );

        return puertoLocal;
    }

    public boolean estaConectado() {
        return sesion != null && sesion.isConnected();
    }

    public int getPuertoLocal() {
        return puertoLocal;
    }

    public void cerrar() {
        if (sesion != null && sesion.isConnected()) {
            sesion.disconnect();
        }

        sesion = null;
        puertoLocal = 0;
    }
}
