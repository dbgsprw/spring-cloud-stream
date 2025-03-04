/*
 * Copyright 2023-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.stream.function;

import java.util.function.Function;

import org.junit.jupiter.api.Test;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.function.context.PostProcessingFunction;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * @author Oleg Zhurakousky
 *
 */
public class FunctionPostProcessingTests {

	@Test
	void testNothingIsBroken() {
		System.clearProperty("spring.cloud.function.definition");
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
			TestChannelBinderConfiguration.getCompleteConfiguration(PostProcessingTestConfiguration.class))
			.web(WebApplicationType.NONE).run("--spring.jmx.enabled=false", "--spring.cloud.function.definition=echo")) {

			InputDestination inputDestination = context.getBean(InputDestination.class);
			Message<byte[]> inputMessage = MessageBuilder.withPayload("hello".getBytes()).build();
			inputDestination.send(inputMessage);

			OutputDestination outputDestination = context.getBean(OutputDestination.class);

			assertThat(outputDestination.receive().getPayload()).isEqualTo("hello".getBytes());
		}
	}

	@Test
	void testSuccessfulPostProcessingOfSingleFunction() {
		System.clearProperty("spring.cloud.function.definition");
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
			TestChannelBinderConfiguration.getCompleteConfiguration(PostProcessingTestConfiguration.class))
			.web(WebApplicationType.NONE).run("--spring.jmx.enabled=false", "--spring.cloud.function.definition=uppercase")) {

			InputDestination inputDestination = context.getBean(InputDestination.class);
			Message<byte[]> inputMessage = MessageBuilder.withPayload("hello".getBytes()).build();
			inputDestination.send(inputMessage);

			OutputDestination outputDestination = context.getBean(OutputDestination.class);

			assertThat(outputDestination.receive().getPayload()).isEqualTo("HELLO".getBytes());
			assertThat(context.getBean(SingleFunctionPostProcessingFunction.class).success).isTrue();
		}
	}

	@Test
	void testNoPostProcessingOnError() {
		System.clearProperty("spring.cloud.function.definition");
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
			TestChannelBinderConfiguration.getCompleteConfiguration(PostProcessingTestConfiguration.class))
			.web(WebApplicationType.NONE).run("--spring.jmx.enabled=false", "--spring.cloud.function.definition=uppercase")) {

			InputDestination inputDestination = context.getBean(InputDestination.class);
			Message<byte[]> inputMessage = MessageBuilder.withPayload("error".getBytes()).build();
			inputDestination.send(inputMessage);

			OutputDestination outputDestination = context.getBean(OutputDestination.class);

			assertThat(outputDestination.receive()).isNull();
			assertThat(context.getBean(SingleFunctionPostProcessingFunction.class).success).isFalse();
		}
	}

	@Test
	void testNoFailureOnPostProcessingError() {
		System.clearProperty("spring.cloud.function.definition");
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
			TestChannelBinderConfiguration.getCompleteConfiguration(PostProcessingTestConfiguration.class))
			.web(WebApplicationType.NONE).run("--spring.jmx.enabled=false", "--spring.cloud.function.definition=uppercase")) {

			InputDestination inputDestination = context.getBean(InputDestination.class);
			Message<byte[]> inputMessage = MessageBuilder.withPayload("post_processing_error".getBytes()).build();
			inputDestination.send(inputMessage);

			OutputDestination outputDestination = context.getBean(OutputDestination.class);

			assertThat(outputDestination.receive().getPayload()).isEqualTo("POST_PROCESSING_ERROR".getBytes());
			assertThat(context.getBean(SingleFunctionPostProcessingFunction.class).success).isFalse();
		}
	}


	@Test
	void testWithCompositionLastFunctionIsPostProcessing() {
		System.clearProperty("spring.cloud.function.definition");
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
			TestChannelBinderConfiguration.getCompleteConfiguration(PostProcessingTestConfiguration.class))
			.web(WebApplicationType.NONE).run("--spring.jmx.enabled=false", "--spring.cloud.function.definition=echo|uppercase")) {

			InputDestination inputDestination = context.getBean(InputDestination.class);
			Message<byte[]> inputMessage = MessageBuilder.withPayload("hello".getBytes()).build();
			inputDestination.send(inputMessage);

			OutputDestination outputDestination = context.getBean(OutputDestination.class);

			assertThat(outputDestination.receive().getPayload()).isEqualTo("HELLO".getBytes());
			assertThat(context.getBean(SingleFunctionPostProcessingFunction.class).success).isTrue();
		}
	}

	@Test
	void testWithCompositionFirstFunctionIsPostProcessing() {
		System.clearProperty("spring.cloud.function.definition");
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
			TestChannelBinderConfiguration.getCompleteConfiguration(PostProcessingTestConfiguration.class))
			.web(WebApplicationType.NONE).run("--spring.jmx.enabled=false", "--spring.cloud.function.definition=uppercase|echo")) {

			InputDestination inputDestination = context.getBean(InputDestination.class);
			Message<byte[]> inputMessage = MessageBuilder.withPayload("hello".getBytes()).build();
			inputDestination.send(inputMessage);

			OutputDestination outputDestination = context.getBean(OutputDestination.class);

			assertThat(outputDestination.receive().getPayload()).isEqualTo("HELLO".getBytes());
			assertThat(context.getBean(SingleFunctionPostProcessingFunction.class).success).isFalse();
		}
	}

	@Test
	void testOlnyLastPostProcessorInvoked() {
		System.clearProperty("spring.cloud.function.definition");
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
			TestChannelBinderConfiguration.getCompleteConfiguration(PostProcessingTestConfiguration.class))
			.web(WebApplicationType.NONE).run("--spring.jmx.enabled=false", "--spring.cloud.function.definition=echo|uppercase|reverse")) {

			InputDestination inputDestination = context.getBean(InputDestination.class);
			Message<byte[]> inputMessage = MessageBuilder.withPayload("hello".getBytes()).build();
			inputDestination.send(inputMessage);

			OutputDestination outputDestination = context.getBean(OutputDestination.class);

			assertThat(outputDestination.receive().getPayload()).isEqualTo("OLLEH".getBytes());
			assertThat(context.getBean(SingleFunctionPostProcessingFunction.class).success).isFalse();
			assertThat(context.getBean(SingleFunctionPostProcessingFunction2.class).success).isTrue();
		}
	}

	@Test
	void testOlnyLastPostProcessorInvoked2() {
		System.clearProperty("spring.cloud.function.definition");
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
			TestChannelBinderConfiguration.getCompleteConfiguration(PostProcessingTestConfiguration.class))
			.web(WebApplicationType.NONE).run("--spring.jmx.enabled=false", "--spring.cloud.function.definition=uppercase|echo|reverse")) {

			InputDestination inputDestination = context.getBean(InputDestination.class);
			Message<byte[]> inputMessage = MessageBuilder.withPayload("hello".getBytes()).build();
			inputDestination.send(inputMessage);

			OutputDestination outputDestination = context.getBean(OutputDestination.class);

			assertThat(outputDestination.receive().getPayload()).isEqualTo("OLLEH".getBytes());
			assertThat(context.getBean(SingleFunctionPostProcessingFunction.class).success).isFalse();
			assertThat(context.getBean(SingleFunctionPostProcessingFunction2.class).success).isTrue();
		}
	}


	@EnableAutoConfiguration
	public static class PostProcessingTestConfiguration {

		@Bean
		public Function<String, String> echo() {
			return x -> x;
		}

		@Bean
		public Function<String, String> uppercase() {
			return new SingleFunctionPostProcessingFunction();
		}

		@Bean
		public Function<String, String> reverse() {
			return new SingleFunctionPostProcessingFunction2();
		}
	}

	private static class SingleFunctionPostProcessingFunction implements PostProcessingFunction<String, String> {

		private boolean success;

		@Override
		public String apply(String input) {
			if (input.equals("error")) {
				throw new RuntimeException("intentional");
			}
			return input.toUpperCase();
		}

		@Override
		public void postProcess(Message<String> result) {
			if (result.getPayload().equals("POST_PROCESSING_ERROR")) {
				throw new RuntimeException("intentional");
			}
			success = true;
		}
	}

	private static class SingleFunctionPostProcessingFunction2 implements PostProcessingFunction<String, String> {

		private boolean success;

		@Override
		public String apply(String input) {
			return new StringBuilder(input).reverse().toString();
		}

		@Override
		public void postProcess(Message<String> result) {
			success = true;
		}
	}
}
