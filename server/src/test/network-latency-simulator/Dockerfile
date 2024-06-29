# Use an official Ubuntu base image
FROM ubuntu:latest

# Set environment variables
ENV DEBIAN_FRONTEND=noninteractive

# Install required packages
RUN apt-get update && \
    apt-get install -y iproute2 iputils-ping curl net-tools iperf3 iptables kmod && \
    curl -L https://github.com/fullstorydev/grpcurl/releases/download/v1.8.7/grpcurl_1.8.7_linux_x86_64.tar.gz -o grpcurl.tar.gz && \
    tar -xvf grpcurl.tar.gz && mv grpcurl /usr/local/bin/grpcurl && rm grpcurl.tar.gz

# Copy scripts and protos folder into the container
COPY configure_latency.sh consumer.sh start.sh /usr/local/bin/
COPY protos /usr/local/protos

# Make the scripts executable
RUN chmod +x /usr/local/bin/configure_latency.sh /usr/local/bin/consumer.sh /usr/local/bin/start.sh

# Default command to run when starting the container
CMD ["bash", "-c", "start.sh"]
