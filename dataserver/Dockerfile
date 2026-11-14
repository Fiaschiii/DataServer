# Imagem da API Java (usada pelo Render; também roda com "docker build" local).
# Etapa 1: compila o .jar com o Maven Wrapper do projeto.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Dependências primeiro: enquanto o pom.xml não muda, o Docker reaproveita esta camada
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

# Os testes não rodam aqui: os de integração precisam do banco (rode ./mvnw test antes do push)
COPY src src
RUN ./mvnw -B -q package -DskipTests

# Etapa 2: só o JRE e o .jar (imagem final bem menor, sem Maven nem código-fonte)
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --no-create-home dataserver
COPY --from=build /app/target/*.jar app.jar
USER dataserver

# O plano grátis do Render tem 512 MB: o heap usa até 75% da memória do contêiner
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC"
# O Render informa a porta na variável PORT (application.properties: server.port=${PORT:8080})
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
