export JAVA_TOOL_OPTIONS="-javaagent:../newrelic.jar"

export NEW_RELIC_APP_NAME="azure-service-bus-java-service"
export NEW_RELIC_LICENSE_KEY={NEW_RELIC_LICENSE_KEY}

./mvnw spring-boot:run