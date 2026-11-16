# Actividad-EDD

Demian 
Manuela
Maria

https://manuelaromero-dev.github.io/Actividad-BDD/registro.html

## Login online (Java)

**https://actividad-bdd.onrender.com**

El servidor Java está publicado gratis en Render a partir de este repositorio. Si la página tarda en abrir, espera 1 minuto: el servidor gratuito se duerme cuando nadie lo usa.

## Cómo ejecutar el login (Java)

GitHub Pages solo muestra el HTML. Para que el registro y el inicio de sesión funcionen hay que ejecutar el servidor Java.

Hace falta Java 11 o superior (JDK). Se descarga gratis en https://adoptium.net

1. Pulsa **Code → Download ZIP** y descomprime la carpeta.
2. Inicia el servidor:
   - **Windows:** doble clic en `iniciar.bat`.
   - **Mac:** doble clic en `iniciar.command`. Si el Mac no deja abrirlo: Ajustes del Sistema → Privacidad y seguridad → "Abrir igualmente".
   - **Terminal (cualquier sistema):** dentro de la carpeta, `java Servidor.java`.
3. Se abre el navegador con la página de inicio de sesión (http://localhost:8080).
4. Para parar el servidor, cierra la ventana de la consola.

Los usuarios se guardan en `usuarios.txt`, en la misma carpeta. La contraseña se guarda como hash, no en texto plano.

### Sin instalar nada: GitHub Codespaces

[![Open in GitHub Codespaces](https://github.com/codespaces/badge.svg)](https://codespaces.new/manuelaromero-dev/Actividad-BDD)

Pulsa el botón, después "Create codespace" y espera 1-2 minutos: el servidor arranca solo.
