import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.awt.Desktop;
import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class Servidor {

    static final Path USUARIOS = Path.of("usuarios.txt");
    static final Map<String, String> sesiones = new HashMap<>();
    static final SecureRandom random = new SecureRandom();

    public static void main(String[] args) throws IOException {
        if (!Files.exists(Path.of("login.html"))) {
            System.out.println("Ejecuta el servidor desde la carpeta del proyecto (donde esta login.html)");
            return;
        }

        HttpServer server;
        String puertoHosting = System.getenv("PORT");
        if (puertoHosting != null) {
            server = HttpServer.create(new InetSocketAddress(Integer.parseInt(puertoHosting)), 0);
        } else {
            try {
                server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 8080), 0);
            } catch (BindException e) {
                server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
            }
        }
        server.createContext("/", Servidor::manejar);
        server.start();

        String url = "http://localhost:" + server.getAddress().getPort();
        System.out.println("Servidor en " + url);
        System.out.println("Para pararlo, cierra esta ventana o pulsa Ctrl+C");
        abrirNavegador(url);
    }

    static void abrirNavegador(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
            }
        } catch (Exception e) {
            System.out.println("Abre " + url + " en el navegador");
        }
    }

    static void manejar(HttpExchange ex) throws IOException {
        String ruta = ex.getRequestURI().getPath();
        boolean post = ex.getRequestMethod().equals("POST");

        if (post && ruta.equals("/registro")) registrar(ex);
        else if (post && ruta.equals("/login")) entrar(ex);
        else if (ruta.equals("/privado")) privado(ex);
        else if (ruta.equals("/logout")) salir(ex);
        else if (ruta.equals("/registro.html")) archivo(ex, "registro.html");
        else if (ruta.equals("/") || ruta.equals("/login.html")) archivo(ex, "login.html");
        else responder(ex, 404, mensaje("Página no encontrada", "/"));
    }

    static void registrar(HttpExchange ex) throws IOException {
        Map<String, String> datos = leerFormulario(ex);
        String nombre = datos.getOrDefault("fullname", "").trim();
        String email = datos.getOrDefault("email", "").trim().toLowerCase();
        String pass = datos.getOrDefault("Contraseña", "");
        String pass2 = datos.getOrDefault("Confirmar Contraseña", "");

        if (nombre.isEmpty() || email.isEmpty() || email.contains(";") || pass.length() < 8) {
            responder(ex, 400, mensaje("Faltan datos o la contraseña es demasiado corta", "/registro.html"));
            return;
        }
        if (!pass.equals(pass2)) {
            responder(ex, 400, mensaje("Las contraseñas no coinciden", "/registro.html"));
            return;
        }
        if (buscar(email) != null) {
            responder(ex, 409, mensaje("Ese correo ya está registrado", "/registro.html"));
            return;
        }

        byte[] sal = new byte[16];
        random.nextBytes(sal);
        String linea = email + ";" + nombre.replace(";", " ") + ";"
                + Base64.getEncoder().encodeToString(sal) + ";" + hash(pass, sal) + "\n";
        Files.writeString(USUARIOS, linea, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);

        responder(ex, 200, mensaje("Usuario registrado. Ya puedes iniciar sesión", "/"));
    }

    static void entrar(HttpExchange ex) throws IOException {
        Map<String, String> datos = leerFormulario(ex);
        String email = datos.getOrDefault("email", "").trim().toLowerCase();
        String pass = datos.getOrDefault("password", "");
        String[] usuario = buscar(email);

        if (usuario == null || !hash(pass, Base64.getDecoder().decode(usuario[2])).equals(usuario[3])) {
            responder(ex, 401, mensaje("Correo o contraseña incorrectos", "/"));
            return;
        }

        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sesiones.put(token, usuario[1]);

        ex.getResponseHeaders().add("Set-Cookie", "sesion=" + token + "; HttpOnly; SameSite=Lax; Path=/");
        redirigir(ex, "/privado");
    }

    static void privado(HttpExchange ex) throws IOException {
        String nombre = sesiones.get(leerCookie(ex));
        if (nombre == null) {
            redirigir(ex, "/");
            return;
        }
        responder(ex, 200, pagina("Zona privada",
                "<h1>Hola, " + escapar(nombre) + "</h1>"
                + "<p>Has iniciado sesión correctamente.</p>"
                + "<a href=\"/logout\">Cerrar sesión</a>"));
    }

    static void salir(HttpExchange ex) throws IOException {
        sesiones.remove(leerCookie(ex));
        ex.getResponseHeaders().add("Set-Cookie", "sesion=; Max-Age=0; Path=/");
        redirigir(ex, "/");
    }

    static String[] buscar(String email) throws IOException {
        if (!Files.exists(USUARIOS)) return null;
        for (String linea : Files.readAllLines(USUARIOS, StandardCharsets.UTF_8)) {
            String[] campos = linea.split(";");
            if (campos[0].equals(email)) return campos;
        }
        return null;
    }

    static String hash(String pass, byte[] sal) {
        try {
            PBEKeySpec spec = new PBEKeySpec(pass.toCharArray(), sal, 100000, 256);
            byte[] resultado = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(resultado);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static Map<String, String> leerFormulario(HttpExchange ex) throws IOException {
        String cuerpo = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> datos = new HashMap<>();
        for (String par : cuerpo.split("&")) {
            String[] kv = par.split("=", 2);
            if (kv.length == 2) {
                datos.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                        URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
            }
        }
        return datos;
    }

    static String leerCookie(HttpExchange ex) {
        String cookies = ex.getRequestHeaders().getFirst("Cookie");
        if (cookies == null) return null;
        for (String c : cookies.split(";")) {
            String[] kv = c.trim().split("=", 2);
            if (kv.length == 2 && kv[0].equals("sesion")) return kv[1];
        }
        return null;
    }

    static void archivo(HttpExchange ex, String nombre) throws IOException {
        responder(ex, 200, Files.readString(Path.of(nombre), StandardCharsets.UTF_8));
    }

    static void responder(HttpExchange ex, int codigo, String html) throws IOException {
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        ex.sendResponseHeaders(codigo, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.close();
    }

    static void redirigir(HttpExchange ex, String destino) throws IOException {
        ex.getResponseHeaders().set("Location", destino);
        ex.sendResponseHeaders(303, -1);
        ex.close();
    }

    static String pagina(String titulo, String cuerpo) {
        return "<!DOCTYPE html><html lang=\"es\"><head><meta charset=\"UTF-8\"><title>"
                + titulo + "</title></head><body>" + cuerpo + "</body></html>";
    }

    static String mensaje(String texto, String enlace) {
        return pagina(texto, "<p>" + texto + "</p><a href=\"" + enlace + "\">Volver</a>");
    }

    static String escapar(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
