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
            ESUM='d768eecddd7a515711659e02caef8516b7b7177fa34880a56398fd9822593a79'; \
            BINARY_URL='https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.4%2B7/OpenJDK21U-jdk_aarch64_linux_hotspot_21.0.4_7.tar.gz'; \
            ;; \
          amd64|i386:x86-64) \
            ESUM='51fb4d03a4429c39d397d3a03a779077159317616550e4e71624c9843083e7b9'; \
            BINARY_URL='https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.4%2B7/OpenJDK21U-jdk_x64_linux_hotspot_21.0.4_7.tar.gz'; \
            ;; \
           ppc64el|powerpc:common64) \
            ESUM='c208cd0fb90560644a90f928667d2f53bfe408c957a5e36206585ad874427761'; \
            BINARY_URL='https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.4%2B7/OpenJDK21U-jdk_ppc64le_linux_hotspot_21.0.4_7.tar.gz'; \
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

ENV JAVA_VERSION="jdk-21.0.4+7"
ENV JAVA_HOME=/usr/local/java
ENV PATH=${JAVA_HOME}/bin:${PATH}

# Install Java
COPY --from=java-builder ${JAVA_HOME}/ ${JAVA_HOME}/

# Expose the port that the application will run on
EXPOSE 8080/tcp

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
