#!/bin/sh
cd "$(dirname "$0")" || exit 1
java Servidor.java || echo "No se pudo iniciar el servidor. Hace falta Java 11 o superior: https://adoptium.net"
