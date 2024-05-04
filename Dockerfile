FROM maven:3.8.5-openjdk-17 AS build
COPY . .
RUN mvn clean package -DskipTests

FROM openjdk:21-jdk
COPY --from=build /target/telegramBot-0.0.1-SNAPSHOT.jar telegramBot.jar
EXPOSE 4000
ENTRYPOINT ["java","-jar","telegramBot.jar"]