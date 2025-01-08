########################################################################################################################
#
# Define Global Build Arguments
#
########################################################################################################################
ARG UBUNTU_TAG="mantic-20240216"
ARG SOURCE_DATE_EPOCH="0"

########################################################################################################################
#
# Setup Builder Image
#
########################################################################################################################
FROM ubuntu:${UBUNTU_TAG} AS java-builder-interim
# Define Build Arguments
ARG SOURCE_DATE_EPOCH

# Define Standard Environment Variables
ENV DEBIAN_FRONTEND=noninteractive
ENV LANG=C.UTF-8
ENV LC_ALL=C.UTF-8

# Install basic OS utilities for building
RUN --mount=type=bind,source=./repro-sources-list.sh,target=/usr/local/bin/repro-sources-list.sh \
    repro-sources-list.sh && \
    apt-get update && \
	apt-get install --yes --no-install-recommends tar gzip curl ca-certificates && \
    apt-get autoclean --yes && \
    apt-get clean all --yes && \
    rm -rf /var/log/ && \
    rm -rf /var/cache/

##########################
####    Java Setup    ####
##########################

RUN set -eux; \
        ARCH="$(dpkg --print-architecture)"; \
        case "${ARCH}" in \
           aarch64|arm64) \
            ESUM='6482639ed9fd22aa2e704cc366848b1b3e1586d2bf1213869c43e80bca58fe5c'; \
            BINARY_URL='https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.5%2B11/OpenJDK21U-jdk_aarch64_linux_hotspot_21.0.5_11.tar.gz' \
            ;; \
          amd64|i386:x86-64) \
            ESUM='3c654d98404c073b8a7e66bffb27f4ae3e7ede47d13284c132d40a83144bfd8c'; \
            BINARY_URL='https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.5%2B11/OpenJDK21U-jdk_x64_linux_hotspot_21.0.5_11.tar.gz' \
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

########################################
####    Deterministic Build Hack    ####
########################################

RUN groupadd --gid 2000 hedera && \
    useradd --no-user-group --create-home --uid 2000 --gid 2000 --shell /bin/bash hedera

# Define version
ARG VERSION

#USER hedera

# Set the working directory inside the container
WORKDIR /app

# Copy Distribution TAR file
COPY --from=distributions server-${VERSION}.tar .

# Extract the TAR file
RUN tar -xvf server-${VERSION}.tar

# Copy the logging properties file
COPY logging.properties logging.properties

WORKDIR /

# === Workarounds below will not be needed when https://github.com/moby/buildkit/pull/4057 is merged ===
# NOTE: PR #4057 has been merged but will not be available until the v0.13.x series of releases.
# Limit the timestamp upper bound to SOURCE_DATE_EPOCH.
# Workaround for https://github.com/moby/buildkit/issues/3180
RUN find $( ls / | grep -E -v "^(dev|mnt|proc|sys)$" ) \
  -newermt "@${SOURCE_DATE_EPOCH}" -writable -xdev \
  | xargs touch --date="@${SOURCE_DATE_EPOCH}" --no-dereference

##########################

FROM scratch AS java-builder
COPY --from=java-builder-interim / /


########################################################################################################################
#
# Build Final Image
#
########################################################################################################################
FROM java-builder AS production-image

# Define Build Arguments
ARG SOURCE_DATE_EPOCH

# Define Standard Environment Variables
ENV LANG=C.UTF-8
ENV LC_ALL=C.UTF-8
ENV DEBIAN_FRONTEND=noninteractive

ENV JAVA_VERSION="jdk-21.0.5+11"
ENV JAVA_HOME=/usr/local/java
ENV PATH=${JAVA_HOME}/bin:${PATH}

# Install Java
COPY --from=java-builder ${JAVA_HOME}/ ${JAVA_HOME}/

# Expose the port that the application will run on
EXPOSE 8080/tcp

USER hedera
WORKDIR /app

# HEALTHCHECK for liveness and readiness
HEALTHCHECK --interval=30s --timeout=10s --start-period=3s --retries=3 \
  CMD curl -f http://localhost:8080/healthz/livez || exit 1 && \
      curl -f http://localhost:8080/healthz/readyz || exit 1

# RUN the bin script for starting the server
ENTRYPOINT ["/bin/bash", "-c", "/app/server-${VERSION}/bin/server"]
