# First Start consumer without any network latency so it connects without issues.
consumer.sh 1 1000 &
# Then start the network latency configuration script.
configure_latency.sh 500 500 64
