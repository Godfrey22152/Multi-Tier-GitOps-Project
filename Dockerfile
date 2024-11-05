# Use the official Maven image to build the app
FROM maven:3.9.1-eclipse-temurin-17 AS build
WORKDIR /app

# Download dependencies without the full build context to speed up caching
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy the source code
COPY src ./src
COPY .mvn/ .mvn
COPY mvnw .

# Grant executable permissions to mvnw
RUN chmod +x mvnw

# Package the Spring Boot app
RUN ./mvnw clean package -DskipTests

# Use the official OpenJDK image to run the app
FROM eclipse-temurin:17-jre-jammy 
WORKDIR /app

# Copy the packaged app from the build stage
COPY --from=build /app/target/bankapp-0.0.1-SNAPSHOT.jar app.jar

# Expose the port on which the app runs
EXPOSE 8080

# Run the Spring Boot app
ENTRYPOINT ["java", "-jar", "app.jar"]
