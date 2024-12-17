########################################################################################################################
#
# Define Global Build Arguments
#
########################################################################################################################
ARG UBUNTU_TAG="focal-20230605"

########################################################################################################################
#
# Setup Builder Image
#
########################################################################################################################
FROM ubuntu:${UBUNTU_TAG} AS openjdk-builder

# Define Standard Environment Variables
ENV LC_ALL=C.UTF-8
ENV DEBIAN_FRONTEND=noninteractive

# Install basic OS utilities for building
RUN apt-get update && \
	apt-get install --yes tar gzip gnupg2 curl

##########################
####    Java Setup    ####
##########################

RUN set -eux; \
        ARCH="$(dpkg --print-architecture)"; \
        case "${ARCH}" in \
           aarch64|arm64) \
             ESUM='e184dc29a6712c1f78754ab36fb48866583665fa345324f1a79e569c064f95e9'; \
             BINARY_URL='https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.1%2B12/OpenJDK21U-jdk_aarch64_linux_hotspot_21.0.1_12.tar.gz'; \
             ;; \
           amd64|i386:x86-64) \
             ESUM='1a6fa8abda4c5caed915cfbeeb176e7fbd12eb6b222f26e290ee45808b529aa1'; \
             BINARY_URL='https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.1%2B12/OpenJDK21U-jdk_x64_linux_hotspot_21.0.1_12.tar.gz'; \
             ;; \
           ppc64el|powerpc:common64) \
             ESUM='9574828ef3d735a25404ced82e09bf20e1614f7d6403956002de9cfbfcb8638f'; \
             BINARY_URL='https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.1%2B12/OpenJDK21U-jdk_ppc64le_linux_hotspot_21.0.1_12.tar.gz'; \
             ;; \
           *) \
             echo "Unsupported arch: ${ARCH}"; \
             exit 1; \
             ;; \
        esac; \
    curl -LfsSo /tmp/openjdk.tar.gz ${BINARY_URL}; \
    echo "${ESUM} */tmp/openjdk.tar.gz" | sha256sum -c -; \
    mkdir -p /usr/local/java; \
    tar --extract \
    	      --file /tmp/openjdk.tar.gz \
    	      --directory "/usr/local/java" \
    	      --strip-components 1 \
    	      --no-same-owner \
    	  ; \
    rm -f /tmp/openjdk.tar.gz /usr/local/java/lib/src.zip;

########################################################################################################################
#
# Build Final Image
#
########################################################################################################################
FROM ubuntu:${UBUNTU_TAG} AS openjdk-base

# Define Standard Environment Variables
ENV LC_ALL=C.UTF-8
ENV DEBIAN_FRONTEND=noninteractive
ENV JAVA_VERSION="jdk-21.0.1+12"
ENV JAVA_HOME="/usr/local/java/"

# Fetch Validated Java Binaries
COPY --from=openjdk-builder /usr/local/java/ /usr/local/java/

# Install Basic OS Requirements
RUN apt-get update && \
	apt-get install --yes --no-install-recommends tar gzip openssl curl && \
	apt-get autoremove --yes && \
	apt-get autoclean --yes && \
	apt-get clean all --yes && \
    rm -rf /var/lib/{apt,dpkg,cache,log}/


# Expose the port that the application will run on
EXPOSE 8080

# Define version
ARG VERSION

# Create a non-root user and group
ARG UNAME=hedera
ARG UID=2000
ARG GID=2000

# Configure the standard user account
RUN groupadd --gid ${GID} ${UNAME} && \
    useradd --no-user-group --create-home --uid ${UID} --gid ${GID} --shell /bin/bash ${UNAME}

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
HEALTHCHECK --interval=30s --timeout=10s --start-period=3s --retries=3 \
  CMD curl -f http://localhost:8080/healthz/livez || exit 1 && \
      curl -f http://localhost:8080/healthz/readyz || exit 1

# RUN the bin script for starting the server
ENTRYPOINT ["/bin/bash", "-c", "/app/server-${VERSION}/bin/server"]
