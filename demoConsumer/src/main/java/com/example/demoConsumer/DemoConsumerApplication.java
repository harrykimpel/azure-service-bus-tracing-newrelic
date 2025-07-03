package com.example.demoConsumer;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
// import org.springframework.jms.annotation.EnableJms;
// import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

import org.apache.qpid.jms.message.JmsBytesMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.demoProducer.Order;
import com.newrelic.api.agent.ConcurrentHashMapHeaders;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TransportType;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Headers;

import jakarta.jms.BytesMessage;
import com.azure.messaging.servicebus.*;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
public class DemoConsumerApplication {

	private static final Logger LOGGER = LoggerFactory.getLogger(DemoConsumerApplication.class);
	private static final String SUBSCRIPTION_NAME = "sample-subscription";
	private static String connectionString = System.getenv("AZURE_SERVICEBUS_NAMESPACE_CONNECTION_STRING");
	private static String topicName = System.getenv("AZURE_SERVICEBUS_SAMPLE_TOPIC_NAME");

	@Trace(dispatcher = true)
	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(DemoConsumerApplication.class);
		app.setBannerMode(Banner.Mode.OFF);
		app.run(args);

		// Create an instance of the processor through the ServiceBusClientBuilder
		ServiceBusProcessorClient processorClient = new ServiceBusClientBuilder()
				.connectionString(connectionString)
				.processor()
				.topicName(topicName)
				.subscriptionName(SUBSCRIPTION_NAME)
				.processMessage(
						DemoConsumerApplication::processMessage)
				.processError(context -> processError(context))
				.buildProcessorClient();

		System.out.println("Starting the processor");
		processorClient.start();

		try {
			TimeUnit.SECONDS.sleep(1000); // Keep the application running for 1000 seconds
		} catch (InterruptedException e) {
			System.err.println("Interrupted while waiting for messages");
		}
		System.out.println("Stopping and closing the processor");
		processorClient.close();
	}

	@Trace(dispatcher = true)
	private static void processMessage(ServiceBusReceivedMessageContext context) {
		ServiceBusReceivedMessage message = context.getMessage();

		LOGGER.info("Processing message. Session: {}, Sequence #: {}. Contents: {}", message.getMessageId(),
				message.getSequenceNumber(), message.getBody());

		SecureRandom secureRandom = new SecureRandom();
		int randomTimeToSleep = secureRandom.nextInt(5);
		Integer secondsToSleep = 3 + randomTimeToSleep;
		try {
			Thread.sleep(secondsToSleep * 1000);
		} catch (Exception ex) {

		}

		Headers distributedTraceHeaders = ConcurrentHashMapHeaders.build(HeaderType.MESSAGE);
		for (Object key : message.getApplicationProperties().values()) {
			Object value = message.getApplicationProperties().get(key);
			if (value != null) {
				// distributedTraceHeaders.addHeader(key, value.toString());
				LOGGER.info("key header: {}", value);
			}
		}
		Object traceParent = message.getApplicationProperties().get("traceparent");
		LOGGER.info("traceparent header: {}", traceParent);
		if (traceParent != null) {
			distributedTraceHeaders.addHeader("traceparent", traceParent.toString());
		}
		Object traceState = message.getApplicationProperties().get("tracestate");
		LOGGER.info("tracestate header: {}", traceState);
		if (traceState != null) {
			distributedTraceHeaders.addHeader("tracestate", traceState.toString());
		}

		// Accept distributed tracing headers to link this request to the originating
		// request
		NewRelic.getAgent().getTransaction().acceptDistributedTraceHeaders(TransportType.AMQP,
				distributedTraceHeaders);

		LOGGER.info("Processing time for message {}: {} seconds", message.getMessageId(),
				secondsToSleep);
	}

	@Trace(dispatcher = true)
	private static void processError(ServiceBusErrorContext context) {
		LOGGER.error("Error when receiving messages from namespace: '{}'. Entity: '{}'",
				context.getFullyQualifiedNamespace(), context.getEntityPath());

		if (!(context.getException() instanceof ServiceBusException)) {
			LOGGER.error("Non-ServiceBusException occurred: {}", context.getException());
			return;
		}

		ServiceBusException exception = (ServiceBusException) context.getException();
		ServiceBusFailureReason reason = exception.getReason();

		if (reason == ServiceBusFailureReason.MESSAGING_ENTITY_DISABLED
				|| reason == ServiceBusFailureReason.MESSAGING_ENTITY_NOT_FOUND
				|| reason == ServiceBusFailureReason.UNAUTHORIZED) {
			LOGGER.error("An unrecoverable error occurred. Stopping processing with reason {}: {}",
					reason, exception.getMessage());
		} else if (reason == ServiceBusFailureReason.MESSAGE_LOCK_LOST) {
			LOGGER.error("Message lock lost for message: {}", context.getException());
		} else if (reason == ServiceBusFailureReason.SERVICE_BUSY) {
			try {
				// Choosing an arbitrary amount of time to wait until trying again.
				TimeUnit.SECONDS.sleep(1);
			} catch (InterruptedException e) {
				LOGGER.error("Unable to sleep for period of time");
			}
		} else {
			LOGGER.error("Error source {}, reason {}, message: {}", context.getErrorSource(),
					reason, context.getException());
		}
	}

	// @Trace(dispatcher = true)
	// @JmsListener(destination = TOPIC_NAME, containerFactory =
	// "topicJmsListenerContainerFactory", subscription = SUBSCRIPTION_NAME)
	// public void receiveMessage(Message<Object> message) {
	// Object payload = message.getPayload();

	// // Get headers from JMS message
	// MessageHeaders headers = message.getHeaders();
	// Object headerBar = headers.get("newrelic");
	// LOGGER.info("headerBar: {}", headerBar);

	// // output the type of the message
	// LOGGER.info("Message type: {}", payload.getClass().getName());
	// LOGGER.info("Message received: {}", payload);
	// if (payload instanceof JmsBytesMessage) {
	// try {
	// JmsBytesMessage byteMessage = (JmsBytesMessage) message;
	// byte[] byteData = null;
	// byteData = new byte[(int) byteMessage.getBodyLength()];
	// byteMessage.readBytes(byteData);
	// String stringMessage = new String(byteData);
	// LOGGER.info("Message received: {}", stringMessage);
	// } catch (Exception e) {
	// LOGGER.error("Error reading bytes from message", e);
	// }
	// }
	// }
}
