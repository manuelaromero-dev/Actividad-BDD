async function hash(texto) {
    const datos = new TextEncoder().encode(texto);
    const resultado = await crypto.subtle.digest("SHA-256", datos);
    return Array.from(new Uint8Array(resultado))
        .map(b => b.toString(16).padStart(2, "0"))
        .join("");
}

function leerUsuarios() {
    return JSON.parse(localStorage.getItem("usuarios")) || [];
}

function mostrarMensaje(texto) {
    document.getElementById("mensaje").textContent = texto;
}

async function registrar(evento) {
    evento.preventDefault();

    const nombre = document.getElementById("nombre").value.trim();
    const email = document.getElementById("Correo_electrónico").value.trim().toLowerCase();
    const edad = document.getElementById("Edad").value;
    const password = document.getElementById("Contraseña").value;
    const confirmar = document.getElementById("Confirmar_Contraseña").value;
    const genero = document.querySelector('input[name="género"]:checked').value;

    if (password !== confirmar) {
        mostrarMensaje("Las contraseñas no coinciden");
        return;
    }

    const usuarios = leerUsuarios();

    if (usuarios.some(u => u.email === email)) {
        mostrarMensaje("Ese correo ya está registrado");
        return;
    }

    usuarios.push({ nombre, email, edad, genero, password: await hash(password) });
    localStorage.setItem("usuarios", JSON.stringify(usuarios));

    evento.target.reset();
    mostrarMensaje("Usuario registrado. Ya puedes iniciar sesión");
}

async function iniciarSesion(evento) {
    evento.preventDefault();

    const email = document.getElementById("email").value.trim().toLowerCase();
    const password = await hash(document.getElementById("password").value);
    const usuario = leerUsuarios().find(u => u.email === email && u.password === password);

    if (!usuario) {
        mostrarMensaje("Correo o contraseña incorrectos");
        return;
    }

    localStorage.setItem("sesion", usuario.email);
    window.location.href = "inicio.html";
}

function mostrarInicio() {
    const email = localStorage.getItem("sesion");
    const usuario = leerUsuarios().find(u => u.email === email);

    if (!usuario) {
        window.location.href = "login.html";
        return;
    }

    document.getElementById("saludo").textContent = "Hola, " + usuario.nombre;
}

function cerrarSesion() {
    localStorage.removeItem("sesion");
    window.location.href = "login.html";
}

const formRegistro = document.getElementById("formRegistro");
const formLogin = document.getElementById("formLogin");
const botonSalir = document.getElementById("salir");

if (formRegistro) formRegistro.addEventListener("submit", registrar);
if (formLogin) formLogin.addEventListener("submit", iniciarSesion);
if (botonSalir) {
    mostrarInicio();
    botonSalir.addEventListener("click", cerrarSesion);
}
