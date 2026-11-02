# CRUDCODILE

## CRUD remoto, copia local y scripts SQL en JavaFX

**Proyecto de escritorio desarrollado en Java con JavaFX, FXML, MariaDB, SQLite y SSH.**

### Integrantes

- Ricardo Castillo
- Alan Campos
- Emmanuel Mora
- Fernanda Suarez
- Lucia Jaramillo

---

## Introduccion

CRUDCODILE es una aplicacion de escritorio que permite conectarse de forma segura a un servidor remoto mediante un tunel SSH, acceder a la base de datos MariaDB `its5a`, consultar sus tablas y descargar una copia de la tabla seleccionada a una base SQLite local.

Una vez descargada la informacion, el usuario puede trabajar directamente sobre la copia local mediante operaciones CRUD: crear registros, consultar datos, modificar registros y eliminarlos. De esta manera, el trabajo cotidiano se realiza sobre SQLite y la conexion remota se utiliza principalmente para obtener o actualizar la informacion proveniente de MariaDB.

La aplicacion fue organizada siguiendo el patron MVC. Las vistas se construyen con JavaFX y FXML, los controladores coordinan las acciones de la interfaz y los modelos concentran el acceso SSH, MariaDB, SQLite y archivos de scripts.

## Funciones principales

- Inicio de sesion con datos SSH y MariaDB.
- Creacion de un tunel SSH hacia el servidor remoto.
- Conexion al esquema MariaDB `its5a`.
- Listado de tablas disponibles en el servidor.
- Descarga de una tabla remota a `data/crudcodile.db`.
- CRUD completo sobre las tablas almacenadas localmente.
- Acceso a la base SQLite local sin iniciar sesion.
- Confirmacion antes de reemplazar una copia local al volver a descargarla.

## Funciones agregadas al proyecto

Ademas de los requisitos principales, CRUDCODILE incluye las siguientes funciones:

- **Modo local:** permite abrir `crudcodile.db` y trabajar sin conectarse por SSH.
- **Editor de scripts SQL:** permite crear, abrir, editar y guardar archivos `.sql`.
- **Consola SQL limitada a CRUD:** solo permite `SELECT`, `INSERT`, `UPDATE` y `DELETE` sobre SQLite local.
- **Proteccion de operaciones masivas:** solicita confirmacion antes de ejecutar `UPDATE` o `DELETE` sin `WHERE`.
- **Bloqueo de comandos fuera del alcance CRUD:** rechaza instrucciones como `CREATE`, `DROP`, `ALTER`, `PRAGMA`, `VACUUM` y `ATTACH`.
- **Persistencia de scripts:** los archivos SQL se guardan por defecto en la carpeta `scripts/`.
- **Ventana de integrantes:** muestra los nombres del equipo desde la pantalla inicial.

## Tecnologias utilizadas

- Java 17+
- JavaFX 21
- FXML
- CSS
- Maven
- JSch
- MariaDB Connector/J
- SQLite JDBC
- Scene Builder para edicion visual de FXML

## Estructura general

```text
src/main/java/crudcodile/
├── Main.java
├── controller/
│   ├── LoginController.java
│   ├── PrincipalController.java
│   └── ScriptController.java
├── model/
│   ├── ColumnaTabla.java
│   ├── ConfiguracionConexion.java
│   ├── ConexionSSH.java
│   ├── DatosTabla.java
│   ├── ModeloMariaDB.java
│   ├── ModeloSQLite.java
│   ├── ModeloScript.java
│   └── ResultadoSQL.java
└── view/
    └── DialogoRegistro.java

src/main/resources/crudcodile/
├── css/
│   └── crudcodile.css
└── view/
    ├── login.fxml
    ├── principal.fxml
    └── script.fxml
```


## Uso responsable de la IA

- Los prompts usados para generar este proyecto han sido

``` Hola chat quiero que analizes mi codigo actual el cual es una herramienta
que me permite descargar una base de datos mediante ssh y hacer ediciones basica de
CRUD en ella ```

``` Quiro que primero me ayudes dandole una mejor estetica quiero que uses primordialmente
colores como el verde, negro y detalles en blanco ```

``` Quiero que me ayudes creando una herramienta que conecte una terminal a mi sistema
el cual me permita ejecutar scripts de comando sql ```

``` Podrias agregar un boton en el menu inicial que diga integrantes donde esten los nombres
de los integrantes del eqipo Ricardo Castillo, Alan Campos, Emmanuel Mora, Fernanda Suarez, Lucia Jaramillo ```

``` Podrias ayudarme haciendo un .md el cual funcione como protada e introduccion al proyecto
asi como las mejoras que fueron agregadas ```

