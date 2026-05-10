# Build stage
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app
COPY . .

RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

CMD ["sh", "-c", "MONGO_FILE=/etc/secrets/mongo_connection; if [ -f \"$MONGO_FILE\" ]; then MONGO_URI=$(tr -d '\\r\\n' < \"$MONGO_FILE\"); fi; exec java -jar app.jar --server.port=${PORT:-8080} ${MONGO_URI:+--spring.data.mongodb.uri=$MONGO_URI}"]