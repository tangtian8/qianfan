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

package org.springaicommunity.qianfanv2.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

// @formatter:off
/**
 * Single class implementation of the QianFan Chat Completion API and Embedding API.
 * <a href="https://cloud.baidu.com/doc/WENXINWORKSHOP/index.html">QianFan Docs</a>
 *
 * @author Geng Rong
 * @author Thomas Vitale
 * @since 1.0
 */
public class QianFanApi {

	public static final String DEFAULT_CHAT_MODEL = ChatModel.ERNIE_4_5_Turbo.getValue();
	public static final String DEFAULT_EMBEDDING_MODEL = EmbeddingModel.EmbeddingV1.getValue();
//	private static final Predicate<ChatCompletionChunk> SSE_DONE_PREDICATE = ChatCompletionChunk::end;


	private final RestClient restClient;

	private final WebClient webClient;

	/**
	 * Create a new chat completion api with default base URL.
	 *
	 * @param apiKey QianFan api key.
	 */
	public QianFanApi(String apiKey) {
		this(QianFanConstants.DEFAULT_BASE_URL, apiKey);
	}

	/**
	 * Create a new chat completion api.
	 *
	 * @param baseUrl api base URL.
	 * @param apiKey QianFan api key.
	 */
	public QianFanApi(String baseUrl, String apiKey) {
		this(baseUrl, apiKey, RestClient.builder());
	}

	/**
	 * Create a new chat completion api.
	 *
	 * @param baseUrl api base URL.
	 * @param apiKey QianFan api key.
	 * @param restClientBuilder RestClient builder.
	 */
	public QianFanApi(String baseUrl, String apiKey, RestClient.Builder restClientBuilder) {
		this(baseUrl, apiKey, restClientBuilder, RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER);
	}

	/**
	 * Create a new chat completion api.
	 *
	 * @param baseUrl api base URL.
	 * @param apiKey QianFan api key.
	 * @param restClientBuilder RestClient builder.
	 * @param responseErrorHandler Response error handler.
	 */
	public QianFanApi(String baseUrl, String apiKey, RestClient.Builder restClientBuilder, ResponseErrorHandler responseErrorHandler) {
		this(baseUrl, apiKey, restClientBuilder, WebClient.builder(), responseErrorHandler);
	}

	/**
	 * Create a new chat completion api.
	 *
	 * @param baseUrl api base URL.
	 * @param apiKey QianFan api key.
	 * @param restClientBuilder RestClient builder.
	 * @param webClientBuilder     WebClient builder.
	 * @param responseErrorHandler Response error handler.
	 */
	public QianFanApi(String baseUrl, String apiKey, RestClient.Builder restClientBuilder,
					WebClient.Builder webClientBuilder, ResponseErrorHandler responseErrorHandler) {

		this.restClient = restClientBuilder
				.baseUrl(baseUrl)
				.defaultHeaders(QianFanUtils.defaultHeaders(apiKey))
				.defaultStatusHandler(responseErrorHandler)
				.build();

		this.webClient = webClientBuilder
				.baseUrl(baseUrl)
				.defaultHeaders(QianFanUtils.defaultHeaders(apiKey))
				.build();
	}

	/**
	 * Creates a model response for the given chat conversation.
	 *
	 * @param chatRequest The chat completion request.
	 * @return Entity response with {@link ChatCompletion} as a body and HTTP status code and headers.
	 */
	public ResponseEntity<ChatCompletion> chatCompletionEntity(ChatCompletionRequest chatRequest) {

		Assert.notNull(chatRequest, "The request body can not be null.");
		Assert.isTrue(!chatRequest.stream(), "Request must set the stream property to false.");

		return this.restClient.post()
				.uri("/chat/completions")
				.body(chatRequest)
				.retrieve()
				.toEntity(ChatCompletion.class);
	}

	/**
	 * Creates a streaming chat response for the given chat conversation.
	 * @param chatRequest The chat completion request. Must have the stream property set
	 * to true.
	 * @return Returns a {@link Flux} stream from chat completion chunks.
	 */
	public Flux<ChatCompletionChunk> chatCompletionStream(ChatCompletionRequest chatRequest) {
		Assert.notNull(chatRequest, "The request body can not be null.");
		Assert.isTrue(chatRequest.stream(), "Request must set the stream property to true.");

		return this.webClient.post()
				.uri("/chat/completions")
				.body(Mono.just(chatRequest), ChatCompletionRequest.class)
				.retrieve()
				.bodyToFlux(ChatCompletionChunk.class)
				.takeUntil(chunk -> {
					if (chunk == null) return false;
					// 根据你的实际业务逻辑调整这里
					if("stop".equals(chunk.choices().get(0).finishReason)){
						return true;
					}
					return false;
				});
	}

	/**
	 * Creates an embedding vector representing the input text or token array.
	 * @param embeddingRequest The embedding request.
	 * @return Returns list of {@link Embedding} wrapped in {@link EmbeddingList}.
	 */
	public ResponseEntity<EmbeddingList> embeddings(EmbeddingRequest embeddingRequest) {

		Assert.notNull(embeddingRequest, "The request body can not be null.");

		// Input text to embed, encoded as a string or array of tokens. To embed multiple
		// inputs in a single
		// request, pass an array of strings or array of token arrays.
		Assert.notNull(embeddingRequest.texts(), "The input can not be null.");

		// The input must not an empty string, and any array must be 16 dimensions or
		// less.
		Assert.isTrue(!CollectionUtils.isEmpty(embeddingRequest.texts()), "The input list can not be empty.");
		Assert.isTrue(embeddingRequest.texts().size() <= 16, "The list must be 16 dimensions or less");

		return this.restClient.post()
				.uri("/embeddings")
				.body(embeddingRequest)
				.retrieve()
				.toEntity(new ParameterizedTypeReference<>() {

				});
	}

	/**
	 * QianFan Chat Completion Models:
	 * <a href="https://cloud.baidu.com/doc/WENXINWORKSHOP/s/Nlks5zkzu#%E5%AF%B9%E8%AF%9Dchat">QianFan Model</a>.
	 */
	public enum ChatModel {
		ERNIE_4_5_Turbo	("ernie-4.5-turbo-128k"),
		;

		public final String  value;

		ChatModel(String value) {
			this.value = value;
		}

		public String getValue() {
			return this.value;
		}
	}

	/**
	 * QianFan Embeddings Models:
	 * <a href="https://cloud.baidu.com/doc/WENXINWORKSHOP/s/Nlks5zkzu#%E5%90%91%E9%87%8Fembeddings">Embeddings</a>.
	 */
	public enum EmbeddingModel {

		/**
		 * DIMENSION: 384
		 */
		EmbeddingV1("embedding-v1"),

		/**
		 * DIMENSION: 1024
		 */
		tao8k("tao-8k"),

		/**
		 * DIMENSION: 1024
		 */
		bge_large_zh	("bge-large-zh"),

		/**
		 * DIMENSION: 1024
		 */
		Qwen3_Embedding_0_6B("Qwen3-Embedding-0.6B");

		public final String  value;

		EmbeddingModel(String value) {
			this.value = value;
		}

		public String getValue() {
			return this.value;
		}
	}

	/**
	 * Creates a model response for the given chat conversation.
	 *
	 * @param messages A list of messages comprising the conversation so far.
	 * @param model ID of the model to use.
	 * @param frequencyPenalty Number between -2.0 and 2.0. Positive values penalize new tokens based on their existing
	 * frequency in the text so far, decreasing the model's likelihood to repeat the same line verbatim.
	 * @param maxTokens The maximum number of tokens to generate in the chat completion. The total length of input
	 * tokens and generated tokens is limited by the model's context length.
	 * appear in the text so far, increasing the model's likelihood to talk about new topics.
	 * @param presencePenalty Number between -2.0 and 2.0. Positive values penalize new tokens based on their existing
	 * frequency in the text so far, decreasing the model's likelihood to repeat the same line verbatim.
	 * @param responseFormat An object specifying the format that the model must output. Setting to { "type":
	 * "json_object" } enables JSON mode, which guarantees the message the model generates is valid JSON.
	 * @param stop Up to 4 sequences where the API will stop generating further tokens.
	 * @param stream If set, partial message deltas will be sent.Tokens will be sent as data-only server-sent events as
	 * they become available, with the stream terminated by a data: [DONE] message.
	 * @param temperature What sampling temperature to use, between 0 and 1. Higher values like 0.8 will make the output
	 * more random, while lower values like 0.2 will make it more focused and deterministic. We generally recommend
	 * altering this or top_p but not both.
	 * @param topP An alternative to sampling with temperature, called nucleus sampling, where the model considers the
	 * results of the tokens with top_p probability mass. So 0.1 means only the tokens comprising the top 10%
	 * probability mass are considered. We generally recommend altering this or temperature but not both.
	 */
	@JsonInclude(Include.NON_NULL)
	public record ChatCompletionRequest(
			@JsonProperty("messages") List<ChatCompletionMessage> messages,
			@JsonProperty("model") String model,
			@JsonProperty("frequency_penalty") Double frequencyPenalty,
			@JsonProperty("max_output_tokens") Integer maxTokens,
			@JsonProperty("presence_penalty") Double presencePenalty,
			@JsonProperty("response_format") ResponseFormat responseFormat,
			@JsonProperty("stop") List<String> stop,
			@JsonProperty("stream") Boolean stream,
			@JsonProperty("temperature") Double temperature,
			@JsonProperty("top_p") Double topP,
			@JsonProperty("tools") List<FunctionTool> tools,
			@JsonProperty("tool_choice") ToolChoice toolChoice
			) {

		public ChatCompletionRequest(List<ChatCompletionMessage> messages, String model, Double temperature,List<FunctionTool> tools,ToolChoice toolChoice) {
			this(messages, model, null, null,
					null, null, null, false, temperature, null,tools,toolChoice);
		}

		/**
		 * Shortcut constructor for a chat completion request with the given messages and model.
		 *
		 * @param messages A list of messages comprising the conversation so far.
		 * @param model ID of the model to use.
		 * @param temperature What sampling temperature to use, between 0 and 1.
		 */
		public ChatCompletionRequest(List<ChatCompletionMessage> messages, String model, Double temperature) {
			this(messages, model, null, null,
					null, null, null, false, temperature, null,null,null);
		}

		/**
		 * Shortcut constructor for a chat completion request with the given messages, model and control for streaming.
		 *
		 * @param messages A list of messages comprising the conversation so far.
		 * @param model ID of the model to use.
		 * @param temperature What sampling temperature to use, between 0 and 1.
		 * @param stream If set, partial message deltas will be sent.Tokens will be sent as data-only server-sent events
		 * as they become available, with the stream terminated by a data: [DONE] message.
		 */
		public ChatCompletionRequest(List<ChatCompletionMessage> messages, String model, Double temperature, boolean stream) {
			this(messages, model, null, null,
					null, null, null, stream, temperature, null,null,null);
		}


		/**
		 * Shortcut constructor for a chat completion request with the given messages, model, tools and tool choice.
		 * Streaming is set to false, temperature to 0.8 and all other parameters are null.
		 *
		 * @param messages A list of messages comprising the conversation so far.
		 * @param stream If set, partial message deltas will be sent.Tokens will be sent as data-only server-sent events
		 * as they become available, with the stream terminated by a data: [DONE] message.
		 */
		public ChatCompletionRequest(List<ChatCompletionMessage> messages, Boolean stream) {
			this(messages, DEFAULT_CHAT_MODEL, null, null,
					null, null, null, stream, 0.8, null,null,null);
		}

		/**
		 * An object specifying the format that the model must output.
		 * @param type Must be one of 'text' or 'json_object'.
		 */
		@JsonInclude(Include.NON_NULL)
		public record ResponseFormat(
				@JsonProperty("type") String type) {
		}

		@JsonInclude(Include.NON_NULL)
		public static enum ToolChoice {
			@JsonProperty("auto")
			AUTO,
			@JsonProperty("any")
			ANY,
			@JsonProperty("none")
			NONE;

			private ToolChoice() {
			}
		}
	}

	/**
	 * Message comprising the conversation.
	 *
	 * @param rawContent The contents of the message. Can be a {@link String}.
	 * The response message content is always a {@link String}.
	 * @param role The role of the messages author. Could be one of the {@link Role} types.
	 */
	@JsonInclude(Include.NON_NULL)
	public record ChatCompletionMessage(
			@JsonProperty("content") Object rawContent,
			@JsonProperty("role") Role role) {

		/**
		 * Get message content as String.
		 */
		public String content() {
			if (this.rawContent == null) {
				return null;
			}
			if (this.rawContent instanceof String text) {
				return text;
			}
			throw new IllegalStateException("The content is not a string!");
		}

		/**
		 * The role of the author of this message.
		 * https://cloud.baidu.com/doc/WENXINWORKSHOP/s/om1k634fe#%E5%A4%9A%E8%A7%92%E8%89%B2%E7%B1%BB%E5%9E%8B
		 * system	否	代表系统角色设定，其content字段为角色设定信息
		 * user	是	代表用户输入，其content字段为对话文本内容
		 * assistant	无标注样例不包含，有标注样例包含	代表模型回答，其content字段为对话文本内容
		 */
		public enum Role {
			/**
			 * System message.
			 */
			@JsonProperty("system")
			system,
			/**
			 * User message.
			 */
			@JsonProperty("user")
			user,
			/**
			 * Assistant message.
			 */
			@JsonProperty("assistant")
			assistant,
			/**
			 * Assistant message.
			 */
			@JsonProperty("tool")
			tool
		}
	}

	/**
	 * Represents a chat completion response returned by model, based on the provided input.
	 *
	 * @param id A unique identifier for the chat completion.
	 * @param result Result of chat completion message.
	 * @param created The Unix timestamp (in seconds) of when the chat completion was created.
	 * used in conjunction with the seed request parameter to understand when backend changes have been made that might
	 * impact determinism.
	 * @param object The object type, which is always chat.completion.
	 * @param finishReason The reason the chat completion finished.
	 * @param usage Usage statistics for the completion request.
	 */
	@JsonInclude(Include.NON_NULL)
	public record ChatCompletion(
			@JsonProperty("id") String id,
			@JsonProperty("object") String object,
			@JsonProperty("created") Long created,
			@JsonProperty("result") String result,
			@JsonProperty("finish_reason") String finishReason,
			@JsonProperty("choices") List<Choices> choices,
			@JsonProperty("usage") Usage usage) {
	}//

	/**
	 * Usage statistics for the completion request.
	 *
	 * @param completionTokens Number of tokens in the completion.
	 * @param promptTokens Number of tokens in the prompt.
	 * @param totalTokens Total number of tokens used in the request (prompt + completion).
	 */
	@JsonInclude(Include.NON_NULL)
	public record Usage(
			@JsonProperty("completion_tokens") Integer completionTokens,
			@JsonProperty("prompt_tokens") Integer promptTokens,
			@JsonProperty("total_tokens") Integer totalTokens) {

	}

	/**
	 *
	 * @param index
	 * @param finishReason
	 * @param message
	 * @param flag
	 */
	@JsonInclude(Include.NON_NULL)
	public record Choices(
			@JsonProperty("index") Integer index,
			@JsonProperty("finish_reason") String finishReason,
			@JsonProperty("message") Message message,
			@JsonProperty("flag") Integer flag) {
	}

	/**
	 *
	 * @param content
	 * @param role
	 */
	@JsonInclude(Include.NON_NULL)
	public record Message(
			@JsonProperty("content") String content,
			@JsonProperty("role") String role,
			@JsonProperty("tool_calls") List<ToolCalls> toolCalls) {
	}


	@JsonInclude(Include.NON_NULL)
	public record ToolCalls(
		@JsonProperty("id") String id,
		@JsonProperty("type") String type,
		@JsonProperty("function") Map<String,String> function){
	}
	@JsonInclude(Include.NON_NULL)
	public record ChoicesChunk(
			@JsonProperty("index") Integer index,
			@JsonProperty("delta") Message message,
			@JsonProperty("finish_reason") String finishReason,
			@JsonProperty("flag") Integer flag) {
	}
	/**
	 * Represents a streamed chunk of a chat completion response returned by model, based on the provided input.
	 *
	 * @param id A unique identifier for the chat completion. Each chunk has the same ID.
	 * @param object The object type, which is always 'chat.completion.chunk'.
	 * @param created The Unix timestamp (in seconds) of when the chat completion was created. Each chunk has the same
	 * timestamp.
	 * @param result Result of chat completion message.
	 */
	@JsonInclude(Include.NON_NULL)
	public record ChatCompletionChunk(
			@JsonProperty("id") String id,
			@JsonProperty("object") String object,
			@JsonProperty("created") Long created,
			@JsonProperty("result") String result,
//			@JsonProperty("finish_reason") String finishReason,
//			@JsonProperty("is_end") Boolean end,
			@JsonProperty("choices") List<ChoicesChunk> choices
//			@JsonProperty("usage") Usage usage
			) {
	}

	/**
	 * Creates an embedding vector representing the input text.
	 *
	 * @param texts Input text to embed, encoded as a string or array of tokens.
	 * @param model ID of the model to use.
	 * @param user A unique identifier representing your end-user, which can help QianFan to
	 * 		monitor and detect abuse.
	 */
	@JsonInclude(Include.NON_NULL)
	public record EmbeddingRequest(
			@JsonProperty("input") List<String> texts,
			@JsonProperty("model") String model,
			@JsonProperty("user_id") String user
			) {


		/**
		 * Create an embedding request with the given input.
		 * Embedding model is set to 'bge_large_zh'.
		 * @param text Input text to embed.
		 */
		public EmbeddingRequest(String text) {
			this(List.of(text), DEFAULT_EMBEDDING_MODEL, null);
		}


		/**
		 * Create an embedding request with the given input.
		 * @param text Input text to embed.
		 * @param model ID of the model to use.
		 * @param userId A unique identifier representing your end-user, which can help QianFan to
		 * 		monitor and detect abuse.
		 */
		public EmbeddingRequest(String text, String model, String userId) {
			this(List.of(text), model, userId);
		}

		/**
		 * Create an embedding request with the given input.
		 * Embedding model is set to 'bge_large_zh'.
		 * @param texts Input text to embed.
		 */
		public EmbeddingRequest(List<String> texts) {
			this(texts, DEFAULT_EMBEDDING_MODEL, null);
		}

		/**
		 * Create an embedding request with the given input.
		 * @param texts Input text to embed.
		 * @param model ID of the model to use.
		 */
		public EmbeddingRequest(List<String> texts, String model) {
			this(texts, model, null);
		}
	}

	/**
	 * Represents an embedding vector returned by embedding endpoint.
	 *
	 * @param index The index of the embedding in the list of embeddings.
	 * @param embedding The embedding vector, which is a list of floats. The length of
	 * vector depends on the model.
	 * @param object The object type, which is always 'embedding'.
	 */
	@JsonInclude(Include.NON_NULL)
	public record Embedding(
			// @formatter:off
			@JsonProperty("index") Integer index,
			@JsonProperty("embedding") float[] embedding,
			@JsonProperty("object") String object) {
		// @formatter:on

		/**
		 * Create an embedding with the given index, embedding and object type set to
		 * 'embedding'.
		 * @param index The index of the embedding in the list of embeddings.
		 * @param embedding The embedding vector, which is a list of floats. The length of
		 * vector depends on the model.
		 */
		public Embedding(Integer index, float[] embedding) {
			this(index, embedding, "embedding");
		}

	}

	/**
	 * List of multiple embedding responses.
	 *
	 * @param object Must have value "embedding_list".
	 * @param data List of entities.
	 * @param model ID of the model to use.
	 * @param errorCode Error code if any.
	 * @param errorNsg Error message if any.
	 * @param usage Usage statistics for the completion request.
	 */
	@JsonInclude(Include.NON_NULL)
	public record EmbeddingList(
	// @formatter:off
			@JsonProperty("object") String object,
			@JsonProperty("data") List<Embedding> data,
			@JsonProperty("model") String model,
			@JsonProperty("error_code") String errorCode,
			@JsonProperty("error_msg") String errorNsg,
			@JsonProperty("usage") Usage usage) {
		// @formatter:on
	}

	@JsonInclude(Include.NON_NULL)
	public record FunctionTool(@JsonProperty("type") String type, @JsonProperty("function") Function function) {

		public FunctionTool(Function function) {
			this("function", function);
		}
	}

	@JsonInclude(Include.NON_NULL)
	public record Function(@JsonProperty("description") String description, @JsonProperty("name") String name,
			@JsonProperty("parameters") Map<String, Object> parameters) {
	}

}
// @formatter:on
