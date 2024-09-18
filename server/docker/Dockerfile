# Use Eclipse Temurin with Java 21 as the base image
FROM eclipse-temurin:21

# Expose the port that the application will run on
EXPOSE 8080

# Define version
ARG VERSION

# Create a non-root user and group
ARG UNAME=hedera
ARG UID=2000
ARG GID=2000
RUN groupadd -g $GID -o $UNAME
RUN useradd -m -u $UID -g $GID -o -s /bin/bash $UNAME
USER $UNAME

# Set the working directory inside the container
WORKDIR /app

# Copy Distribution TAR file
COPY --from=distributions server-${VERSION}.tar .

# Extract the TAR file
RUN tar -xvf server-${VERSION}.tar

# Copy the logging properties file
COPY logging.properties logging.properties

# HEALTHCHECK for liveness and readiness
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/healthz/livez || exit 1 && \
      curl -f http://localhost:8080/healthz/readyz || exit 1

# RUN the bin script for starting the server
ENTRYPOINT ["/bin/bash", "-c", "/app/server-${VERSION}/bin/server"]
