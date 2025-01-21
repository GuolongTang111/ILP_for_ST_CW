FROM openjdk:23

EXPOSE 8080

WORKDIR /app

COPY ./target/restservice-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT [ "java", "-jar", "app.jar" ]