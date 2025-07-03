export JAVA_TOOL_OPTIONS="-javaagent:../newrelic.jar"

# Azure Service Bus Configuration
export AZURE_SERVICEBUS_NAMESPACE_CONNECTION_STRING="Endpoint=sb://..."
export AZURE_SERVICEBUS_SAMPLE_TOPIC_NAME="orders"

export NEW_RELIC_APP_NAME="azure-service-bus-java-consumer"

./mvnw spring-boot:run