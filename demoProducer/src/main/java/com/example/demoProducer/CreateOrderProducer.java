package com.example.demoProducer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

import com.newrelic.api.agent.ConcurrentHashMapHeaders;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TransportType;
import com.azure.core.util.BinaryData;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusMessageBatch;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.NewRelic;
import com.example.demoProducer.Order;

@Service
public class CreateOrderProducer {

        private static final Logger log = LoggerFactory.getLogger(CreateOrderProducer.class);

        String connectionString = System.getenv("AZURE_SERVICEBUS_NAMESPACE_CONNECTION_STRING");
        String topicName = System.getenv("AZURE_SERVICEBUS_SAMPLE_TOPIC_NAME");

        public CreateOrderProducer() {
        }

        @Trace(dispatcher = true)
        public boolean sendCreateOrderEvent(Order order) throws ExecutionException, InterruptedException {

                // NewRelic.getAgent().getTransaction().startSegment("CreateOrderProducer.sendCreateOrderEvent");

                AtomicBoolean sampleSuccessful = new AtomicBoolean(false);
                CountDownLatch countdownLatch = new CountDownLatch(1);

                // The connection string value can be obtained by:
                // 1. Going to your Service Bus namespace in Azure Portal.
                // 2. Go to "Shared access policies"
                // 3. Copy the connection string for the "RootManageSharedAccessKey" policy.
                // The 'connectionString' format is shown below.
                // 1.
                // "Endpoint={fully-qualified-namespace};SharedAccessKeyName={policy-name};SharedAccessKey={key}"
                // 2. "<<fully-qualified-namespace>>" will look similar to
                // "{your-namespace}.servicebus.windows.net"
                // 3. "queueName" will be the name of the Service Bus queue instance you created
                // inside the Service Bus namespace.

                // Instantiate a client that will be used to call the service.
                ServiceBusSenderClient sender = new ServiceBusClientBuilder()
                                .connectionString(connectionString)
                                .sender()
                                .topicName(topicName) // Use topicName for topics, queueName for queues
                                .buildClient();

                // ConcurrentHashMapHeaders provides a concrete implementation of
                // com.newrelic.api.agent.Headers
                com.newrelic.api.agent.Headers distributedTraceHeaders = ConcurrentHashMapHeaders
                                .build(HeaderType.MESSAGE);
                NewRelic.getAgent().getTransaction().acceptDistributedTraceHeaders(TransportType.AMQP,
                                distributedTraceHeaders);

                com.newrelic.api.agent.Headers dtHeaders = ConcurrentHashMapHeaders.build(HeaderType.MESSAGE);
                NewRelic.getAgent().getTransaction().insertDistributedTraceHeaders(dtHeaders);

                log.info("getHeaderNames: {}",
                                dtHeaders.getHeaderNames());
                // }
                // if (distributedTraceHeaders.containsHeader("traceparent")) {
                // log.info("Distributed trace headers: {}",
                // dtHeaders.getHeader("traceparent").getBytes(StandardCharsets.UTF_8));
                // }

                // Create a message to send.
                // final ServiceBusMessageBatch messageBatch = sender.createMessageBatch();
                // IntStream.range(0, 1)
                // .mapToObj(index -> new
                // ServiceBusMessage(BinaryData.fromString(order.toString())))
                // .forEach(message -> messageBatch.tryAddMessage(message));
                ServiceBusMessage message = new ServiceBusMessage(BinaryData.fromString(order.toString()));
                // message.addContext("newrelic", dtHeaders.getHeaders("newrelic"));

                Object traceparent = dtHeaders.getHeader("traceparent");// .getBytes(StandardCharsets.UTF_8);
                log.info("traceparent: {}", traceparent);
                if (traceparent != null) {
                        message.getApplicationProperties().put("traceparent", traceparent);
                }
                Object tracestate = dtHeaders.getHeader("tracestate");// .getBytes(StandardCharsets.UTF_8);
                log.info("tracestate: {}", tracestate);
                if (tracestate != null) {
                        message.getApplicationProperties().put("tracestate", tracestate);
                }
                // messageBatch.tryAddMessage(message);

                // Send that message. It completes successfully when the event has been
                // delivered to the Service queue or topic.
                // It completes with an error if an exception occurred while sending the
                // message.
                sender.sendMessage(message);

                // Close the sender.
                sender.close();

                log.info("Create order {} event sent via Azure Service Bus", order);
                // log.info(sendResult.toString());
                return true;
        }
}
