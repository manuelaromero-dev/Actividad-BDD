FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY Servidor.java login.html registro.html ./
RUN javac Servidor.java
CMD ["java", "Servidor"]
