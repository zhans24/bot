FROM maven:3.2.0-openjdk-21 AS build
COPY . .
RUN mvn clean package -DskipTests

FROM openjdk:21.0.1
COPY --from=build /target/telegramBot-0.0.1-SNAPSHOT.jar telegramBot.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","telegramBot.jar"]