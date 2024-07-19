# Use Eclipse Temurin with Java 21 as the base image
FROM eclipse-temurin:21

# Define version
ARG VERSION

# Set the working directory inside the container
WORKDIR /app

# Copy Distribution TAR file
COPY --from=distributions server-${VERSION}.tar .

# Extract the TAR file
RUN tar -xvf server-${VERSION}.tar

# Expose the port that the application will run on
EXPOSE 8080

# RUN the bin script for starting the server
ENTRYPOINT ["sh", "-c", "/app/server-${VERSION}/bin/server"]
