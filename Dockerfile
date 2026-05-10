# Build stage
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app
COPY . .

RUN mvn clean package -DskipTests

# Runtime stage — use full Debian JRE (not Alpine/musl) to avoid SSL issues with MongoDB Atlas
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

CMD ["sh", "-c", "MONGO_FILE=/etc/secrets/mongo_connection; if [ -f \"$MONGO_FILE\" ]; then RAW=$(tr -d '\\r\\n' < \"$MONGO_FILE\"); case \"$RAW\" in MONGO_URI=*|MONGO_CONNECTION=*|spring.data.mongodb.uri=*|spring.mongodb.uri=*) MONGO_URI=\"${RAW#*=}\" ;; *) MONGO_URI=\"$RAW\" ;; esac; fi; exec java -Djdk.tls.client.protocols=TLSv1.2,TLSv1.3 -Djsse.enableSNIExtension=true -jar app.jar --server.port=${PORT:-8080} ${MONGO_URI:+--spring.data.mongodb.uri=$MONGO_URI --spring.mongodb.uri=$MONGO_URI}"]