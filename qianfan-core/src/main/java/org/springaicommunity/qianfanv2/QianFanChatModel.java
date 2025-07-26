/*
 * Copyright 2023-2024 the original author or authors.
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

package org.springaicommunity.qianfanv2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.qianfanv2.api.QianFanApi;
import org.springaicommunity.qianfanv2.api.QianFanApi.ChatCompletion;
import org.springaicommunity.qianfanv2.api.QianFanApi.ChatCompletionChunk;
import org.springaicommunity.qianfanv2.api.QianFanApi.ChatCompletionMessage;
import org.springaicommunity.qianfanv2.api.QianFanApi.ChatCompletionMessage.Role;
import org.springaicommunity.qianfanv2.api.QianFanApi.ChatCompletionRequest;
import org.springaicommunity.qianfanv2.api.QianFanConstants;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.metadata.EmptyUsage;
import org.springframework.ai.chat.model.*;
import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.ai.chat.observation.ChatModelObservationConvention;
import org.springframework.ai.chat.observation.ChatModelObservationDocumentation;
import org.springframework.ai.chat.observation.DefaultChatModelObservationConvention;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.model.tool.*;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;

/**
 * {@link ChatModel} and {@link StreamingChatModel} implementation for {@literal QianFan}
 * backed by {@link QianFanApi}.
 *
 * @author Geng Rong
 * @see ChatModel
 * @see StreamingChatModel
 * @see QianFanApi
 * @author Alexandros Pappas
 * @since 1.0
 */
public class QianFanChatModel implements ChatModel, StreamingChatModel {

	private static final Logger logger = LoggerFactory.getLogger(QianFanChatModel.class);

	private static final ChatModelObservationConvention DEFAULT_OBSERVATION_CONVENTION = new DefaultChatModelObservationConvention();

	private static final ToolCallingManager DEFAULT_TOOL_CALLING_MANAGER = ToolCallingManager.builder().build();

	/**
	 * The retry template used to retry the QianFan API calls.
	 */
	public final RetryTemplate retryTemplate;

	/**
	 * The default options used for the chat completion requests.
	 */
	private final QianFanChatOptions defaultOptions;

	/**
	 * Low-level access to the QianFan API.
	 */
	private final QianFanApi qianFanApi;

	/**
	 * Observation registry used for instrumentation.
	 */
	private final ObservationRegistry observationRegistry;

	/**
	 * Conventions to use for generating observations.
	 */
	private ChatModelObservationConvention observationConvention = DEFAULT_OBSERVATION_CONVENTION;

	private final ToolExecutionEligibilityPredicate toolExecutionEligibilityPredicate = new DefaultToolExecutionEligibilityPredicate();

	/**
	 * Creates an instance of the QianFanChatModel.
	 * @param qianFanApi The QianFanApi instance to be used for interacting with the
	 * QianFan Chat API.
	 * @throws IllegalArgumentException if QianFanApi is null
	 */
	public QianFanChatModel(QianFanApi qianFanApi) {
		this(qianFanApi, QianFanChatOptions.builder().model(QianFanApi.DEFAULT_CHAT_MODEL).temperature(0.7).build());
	}

	/**
	 * Initializes an instance of the QianFanChatModel.
	 * @param qianFanApi The QianFanApi instance to be used for interacting with the
	 * QianFan Chat API.
	 * @param options The QianFanChatOptions to configure the chat client.
	 */
	public QianFanChatModel(QianFanApi qianFanApi, QianFanChatOptions options) {
		this(qianFanApi, options, RetryUtils.DEFAULT_RETRY_TEMPLATE);
	}

	/**
	 * Initializes a new instance of the QianFanChatModel.
	 * @param qianFanApi The QianFanApi instance to be used for interacting with the
	 * QianFan Chat API.
	 * @param options The QianFanChatOptions to configure the chat client.
	 * @param retryTemplate The retry template.
	 */
	public QianFanChatModel(QianFanApi qianFanApi, QianFanChatOptions options, RetryTemplate retryTemplate) {
		this(qianFanApi, options, retryTemplate, ObservationRegistry.NOOP);
	}

	/**
	 * Initializes a new instance of the QianFanChatModel.
	 * @param qianFanApi The QianFanApi instance to be used for interacting with the
	 * QianFan Chat API.
	 * @param options The QianFanChatOptions to configure the chat client.
	 * @param retryTemplate The retry template.
	 * @param observationRegistry The ObservationRegistry used for instrumentation.
	 */
	public QianFanChatModel(QianFanApi qianFanApi, QianFanChatOptions options, RetryTemplate retryTemplate,
			ObservationRegistry observationRegistry) {
		Assert.notNull(qianFanApi, "QianFanApi must not be null");
		Assert.notNull(options, "Options must not be null");
		Assert.notNull(retryTemplate, "RetryTemplate must not be null");
		Assert.notNull(observationRegistry, "ObservationRegistry must not be null");
		this.qianFanApi = qianFanApi;
		this.defaultOptions = options;
		this.retryTemplate = retryTemplate;
		this.observationRegistry = observationRegistry;
	}

	@Override
	public ChatResponse call(Prompt prompt) {
		Prompt requestPrompt = this.buildRequestPrompt(prompt);
		ChatCompletionRequest request = createRequest(requestPrompt, false);
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			// 对象转JSON字符串
			String json = objectMapper.writeValueAsString(request);
			logger.info("request:{}", json);
		}
		catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		ChatModelObservationContext observationContext = ChatModelObservationContext.builder()
			.prompt(prompt)
			.provider(QianFanConstants.PROVIDER_NAME)
			.build();

		return ChatModelObservationDocumentation.CHAT_MODEL_OPERATION
			.observation(this.observationConvention, DEFAULT_OBSERVATION_CONVENTION, () -> observationContext,
					this.observationRegistry)
			.observe(() -> {
				ResponseEntity<ChatCompletion> completionEntity = this.retryTemplate
					.execute(ctx -> this.qianFanApi.chatCompletionEntity(request));

				var chatCompletion = completionEntity.getBody();
				if (chatCompletion == null) {
					logger.warn("No chat completion returned for prompt: {}", prompt);
					return new ChatResponse(List.of());
				}

			// @formatter:off
					Map<String, Object> metadata = Map.of(
						"id", chatCompletion.id(),
						"role", Role.assistant
					);

				try {
					// 对象转JSON字符串
					String json = objectMapper.writeValueAsString(chatCompletion);
					logger.info("response:{}", json);
				} catch (JsonProcessingException e) {
					e.printStackTrace();
				}
					// @formatter:on
				String content = chatCompletion.choices().get(0).message().content();
				List<QianFanApi.ToolCalls> toolCallsParam = chatCompletion.choices().get(0).message().toolCalls();
				List<AssistantMessage.ToolCall> toolCalls = new ArrayList<>();
				if (!CollectionUtils.isEmpty(toolCallsParam)) {
					for (QianFanApi.ToolCalls ele : toolCallsParam) {
						Map<String, String> function = ele.function();
						String name = function.get("name");
						String arguments = function.get("arguments");
						AssistantMessage.ToolCall toolCall = new AssistantMessage.ToolCall(name, ele.type(), name,
								arguments);
						toolCalls.add(toolCall);
					}
				}
				var assistantMessage = new AssistantMessage(content, metadata, toolCalls);
				List<Generation> generations = Collections.singletonList(new Generation(assistantMessage));

				// List<Generation> generations =
				// chatCompletion.choices().stream().map((choice) -> {
				// Map<String, Object> metadata = Map.of("id", chatCompletion.id() != null
				// ? chatCompletion.id() : "", "index", choice.index(), "role",
				// choice.message().role() != null ? choice.message().role().name() : "",
				// "finishReason", choice.finishReason() != null ?
				// choice.finishReason().name() : "");
				// return this.buildGeneration(choice, metadata);
				// }).toList();

				ChatResponse chatResponse = new ChatResponse(generations, from(chatCompletion, request.model()));
				observationContext.setResponse(chatResponse);

				if (this.toolExecutionEligibilityPredicate.isToolExecutionRequired(prompt.getOptions(), chatResponse)) {
					logger.info("执行tools");
					ToolExecutionResult toolExecutionResult = this.DEFAULT_TOOL_CALLING_MANAGER.executeToolCalls(prompt,
							chatResponse);
					return ChatResponse.builder()
						.from(chatResponse)
						.generations(ToolExecutionResult.buildGenerations(toolExecutionResult))
						.build();
					// return toolExecutionResult.returnDirect()
					// ? ChatResponse.builder()
					// .from(chatResponse)
					// .generations(ToolExecutionResult.buildGenerations(toolExecutionResult))
					// .build()
					// : this.call(new Prompt(toolExecutionResult.conversationHistory(),
					// prompt.getOptions()));
				}
				else {
					return chatResponse;
				}

			});
	}

	@Override
	public Flux<ChatResponse> stream(Prompt prompt) {

		return Flux.deferContextual(contextView -> {
			ChatCompletionRequest request = createRequest(prompt, true);

			var completionChunks = this.qianFanApi.chatCompletionStream(request);

			final ChatModelObservationContext observationContext = ChatModelObservationContext.builder()
				.prompt(prompt)
				.provider(QianFanConstants.PROVIDER_NAME)
				.build();

			Observation observation = ChatModelObservationDocumentation.CHAT_MODEL_OPERATION.observation(
					this.observationConvention, DEFAULT_OBSERVATION_CONVENTION, () -> observationContext,
					this.observationRegistry);

			observation.parentObservation(contextView.getOrDefault(ObservationThreadLocalAccessor.KEY, null)).start();

			Flux<ChatResponse> chatResponse = completionChunks.map(this::toChatCompletion)
				.switchMap(chatCompletion -> Mono.just(chatCompletion).map(chatCompletion2 -> {
				// @formatter:off
						Map<String, Object> metadata = Map.of(
								"id", chatCompletion.id(),
								"role", Role.assistant
						);
						// @formatter:on

					String content = chatCompletion.choices().get(0).message().content();
					var assistantMessage = new AssistantMessage(content, metadata);
					List<Generation> generations = Collections.singletonList(new Generation(assistantMessage));
					return new ChatResponse(generations, from(chatCompletion, request.model()));
				}))
				.doOnError(observation::error)
				.doFinally(s -> observation.stop())
				.contextWrite(ctx -> ctx.put(ObservationThreadLocalAccessor.KEY, observation));
			return new MessageAggregator().aggregate(chatResponse, observationContext::setResponse);

		});
	}

	/**
	 * Convert the ChatCompletionChunk into a ChatCompletion.
	 * @param chunk the ChatCompletionChunk to convert
	 * @return the ChatCompletion
	 */
	private ChatCompletion toChatCompletion(ChatCompletionChunk chunk) {
		List<QianFanApi.ChoicesChunk> choicesChunk = chunk.choices();
		List<QianFanApi.Choices> choices = new ArrayList<>();
		if (!CollectionUtils.isEmpty(choicesChunk)) {
			for (QianFanApi.ChoicesChunk ele : choicesChunk) {
				QianFanApi.Choices choice = new QianFanApi.Choices(ele.index(), null, ele.message(), ele.flag());
				choices.add(choice);
			}
		}
		return new ChatCompletion(chunk.id(), chunk.object(), chunk.created(), chunk.result(), null, choices, null);
	}

	/**
	 * Accessible for testing.
	 */
	public ChatCompletionRequest createRequest(Prompt prompt, boolean stream) {
		var chatCompletionMessages = prompt.getInstructions()
			.stream()
			.map(m -> new ChatCompletionMessage(m.getText(), Role.valueOf(m.getMessageType().getValue())))
			.toList();
		var systemMessageList = chatCompletionMessages.stream().filter(msg -> msg.role() == Role.system).toList();
		var userMessageList = chatCompletionMessages.stream().filter(msg -> msg.role() == Role.user).toList();

		if (systemMessageList.size() > 1) {
			throw new IllegalArgumentException("Only one system message is allowed in the prompt");
		}
		List<ChatCompletionMessage> messages = new ArrayList<>();
		messages.addAll(systemMessageList);
		messages.addAll(userMessageList);
		// var systemMessage = systemMessageList.isEmpty() ? null :
		// systemMessageList.get(0).content();
		var request = new ChatCompletionRequest(messages, stream);

		if (this.defaultOptions != null) {
			request = ModelOptionsUtils.merge(this.defaultOptions, request, ChatCompletionRequest.class);
		}

		if (prompt.getOptions() != null) {
			var updatedRuntimeOptions = ModelOptionsUtils.copyToTarget(prompt.getOptions(), ChatOptions.class,
					QianFanChatOptions.class);
			request = ModelOptionsUtils.merge(updatedRuntimeOptions, request, ChatCompletionRequest.class);

			List<ToolDefinition> toolDefinitions = DEFAULT_TOOL_CALLING_MANAGER
				.resolveToolDefinitions((QianFanChatOptions) prompt.getOptions());
			request = ModelOptionsUtils.merge(
					QianFanChatOptions.builder().tools(this.getFunctionTools(toolDefinitions)).build(), request,
					QianFanApi.ChatCompletionRequest.class);
		}
		return request;
	}

	private static ObjectMapper objectMapper = new ObjectMapper();

	private List<QianFanApi.FunctionTool> getFunctionTools(List<ToolDefinition> toolDefinitions) {
		return toolDefinitions.stream().map((toolDefinition) -> {
			try {
				Map<String, Object> schemaMap = objectMapper.readValue(toolDefinition.inputSchema(),
						new TypeReference<Map<String, Object>>() {
						});
				QianFanApi.Function function = new QianFanApi.Function(toolDefinition.description(),
						toolDefinition.name(), schemaMap);
				return new QianFanApi.FunctionTool(function);
			}
			catch (Exception e) {
				// 处理JSON解析异常
				throw new RuntimeException("Schema解析失败", e);
			}
		}).toList();
	}

	Prompt buildRequestPrompt(Prompt prompt) {
		QianFanChatOptions runtimeOptions = null;
		if (prompt.getOptions() != null) {
			ChatOptions var4 = prompt.getOptions();
			if (var4 instanceof ToolCallingChatOptions) {
				ToolCallingChatOptions toolCallingChatOptions = (ToolCallingChatOptions) var4;
				runtimeOptions = (QianFanChatOptions) ModelOptionsUtils.copyToTarget(toolCallingChatOptions,
						ToolCallingChatOptions.class, QianFanChatOptions.class);
			}
			else {
				runtimeOptions = (QianFanChatOptions) ModelOptionsUtils.copyToTarget(prompt.getOptions(),
						ChatOptions.class, QianFanChatOptions.class);
			}
		}

		QianFanChatOptions requestOptions = (QianFanChatOptions) ModelOptionsUtils.merge(runtimeOptions,
				this.defaultOptions, QianFanChatOptions.class);
		if (runtimeOptions != null) {
			requestOptions.setInternalToolExecutionEnabled(
					(Boolean) ModelOptionsUtils.mergeOption(runtimeOptions.getInternalToolExecutionEnabled(),
							this.defaultOptions.getInternalToolExecutionEnabled()));
			requestOptions.setToolNames(ToolCallingChatOptions.mergeToolNames(runtimeOptions.getToolNames(),
					this.defaultOptions.getToolNames()));
			requestOptions.setToolCallbacks(ToolCallingChatOptions.mergeToolCallbacks(runtimeOptions.getToolCallbacks(),
					this.defaultOptions.getToolCallbacks()));
			requestOptions.setToolContext(ToolCallingChatOptions.mergeToolContext(runtimeOptions.getToolContext(),
					this.defaultOptions.getToolContext()));
		}
		else {
			requestOptions.setInternalToolExecutionEnabled(this.defaultOptions.getInternalToolExecutionEnabled());
			requestOptions.setToolNames(this.defaultOptions.getToolNames());
			requestOptions.setToolCallbacks(this.defaultOptions.getToolCallbacks());
			requestOptions.setToolContext(this.defaultOptions.getToolContext());
		}

		ToolCallingChatOptions.validateToolCallbacks(requestOptions.getToolCallbacks());
		return new Prompt(prompt.getInstructions(), requestOptions);
	}

	@Override
	public ChatOptions getDefaultOptions() {
		return QianFanChatOptions.fromOptions(this.defaultOptions);
	}

	private ChatOptions buildRequestOptions(ChatCompletionRequest request) {
		return ChatOptions.builder()
			.model(request.model())
			.frequencyPenalty(request.frequencyPenalty())
			.maxTokens(request.maxTokens())
			.presencePenalty(request.presencePenalty())
			.stopSequences(request.stop())
			.temperature(request.temperature())
			.topP(request.topP())
			.build();
	}

	private ChatResponseMetadata from(ChatCompletion result, String model) {
		Assert.notNull(result, "QianFan ChatCompletionResult must not be null");
		return ChatResponseMetadata.builder()
			.id(result.id() != null ? result.id() : "")
			.usage(result.usage() != null ? getDefaultUsage(result.usage()) : new EmptyUsage())
			.model(model)
			.keyValue("created", result.created() != null ? result.created() : 0L)
			.build();
	}

	private DefaultUsage getDefaultUsage(QianFanApi.Usage usage) {
		return new DefaultUsage(usage.promptTokens(), usage.completionTokens(), usage.totalTokens(), usage);
	}

	public void setObservationConvention(ChatModelObservationConvention observationConvention) {
		this.observationConvention = observationConvention;
	}

}
