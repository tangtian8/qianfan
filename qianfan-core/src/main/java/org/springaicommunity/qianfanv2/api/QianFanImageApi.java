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
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springaicommunity.qianfan.api.auth.AuthApi;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * QianFan Image API.
 *
 * @author Geng Rong
 * @since 1.0
 */
public class QianFanImageApi {

	public static final String DEFAULT_IMAGE_MODEL = ImageModel.ERNIE_iRAG_1.getValue();

	private final RestClient restClient;

	/**
	 * Create a new QianFan Image api with default base URL.
	 * @param apiKey QianFan api key.
	 */
	public QianFanImageApi(String apiKey) {
		this(QianFanConstants.DEFAULT_BASE_URL, apiKey, RestClient.builder());
	}

	/**
	 * Create a new QianFan Image API with the provided base URL.
	 * @param baseUrl the base URL for the QianFan API.
	 * @param apiKey QianFan api key.
	 * @param restClientBuilder the rest client builder to use.
	 */
	public QianFanImageApi(String baseUrl, String apiKey, RestClient.Builder restClientBuilder) {
		this(baseUrl, apiKey, restClientBuilder, RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER);
	}

	/**
	 * Create a new QianFan Image API with the provided base URL.
	 * @param baseUrl the base URL for the QianFan API.
	 * @param apiKey QianFan api key.
	 * @param restClientBuilder the rest client builder to use.
	 * @param responseErrorHandler the response error handler to use.
	 */
	public QianFanImageApi(String baseUrl, String apiKey, RestClient.Builder restClientBuilder,
			ResponseErrorHandler responseErrorHandler) {

		this.restClient = restClientBuilder.baseUrl(baseUrl)
			.defaultHeaders(QianFanUtils.defaultHeaders(apiKey))
			.defaultStatusHandler(responseErrorHandler)
			.build();
	}

	public ResponseEntity<QianFanImageResponse> createImage(QianFanImageRequest qianFanImageRequest) {
		Assert.notNull(qianFanImageRequest, "Image request cannot be null.");
		Assert.hasLength(qianFanImageRequest.prompt(), "Prompt cannot be empty.");

		return this.restClient.post()
			.uri("/images/generations")
			.body(qianFanImageRequest)
			.retrieve()
			.toEntity(QianFanImageResponse.class);
	}

	/**
	 * QianFan Image API model.
	 */
	public enum ImageModel {

		/**
		 * Stable Diffusion XL (SDXL) is a powerful text-to-image generation model.
		 */
		ERNIE_iRAG_1("irag-1.0");

		private final String value;

		ImageModel(String model) {
			this.value = model;
		}

		public String getValue() {
			return this.value;
		}

	}

	// @formatter:off
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record QianFanImageRequest(
		@JsonProperty("model") String model,
		@JsonProperty("prompt") String prompt,
		@JsonProperty("negative_prompt") String negativePrompt,
		@JsonProperty("size") String size,
		@JsonProperty("n") Integer n,
		@JsonProperty("steps") Integer steps,
		@JsonProperty("seed") Integer seed,
		@JsonProperty("style") String style,
		@JsonProperty("user_id") String user) {

		public QianFanImageRequest(String prompt, String model) {
			this(model, prompt, null, null, null, null, null, null, null);
		}
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record QianFanImageResponse(
		@JsonProperty("id") String id,
		@JsonProperty("created") Long created,
		@JsonProperty("data") List<Data> data) {
	}
	// @formatter:onn

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record Data(@JsonProperty("index") Integer index, @JsonProperty("url") String url) {

	}

}
