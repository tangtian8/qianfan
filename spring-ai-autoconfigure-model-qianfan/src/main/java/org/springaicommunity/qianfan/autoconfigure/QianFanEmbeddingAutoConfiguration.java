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

package org.springaicommunity.qianfan.autoconfigure;

import io.micrometer.observation.ObservationRegistry;

import org.springframework.ai.embedding.observation.EmbeddingModelObservationConvention;
import org.springframework.ai.model.SpringAIModelProperties;
import org.springaicommunity.qianfan.QianFanEmbeddingModel;
import org.springaicommunity.qianfan.api.QianFanApi;
import org.springframework.ai.retry.autoconfigure.SpringAiRetryAutoConfiguration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;

import static org.springaicommunity.qianfanv2.api.QianFanConstants.PROVIDER_NAME;

/**
 * Embedding {@link AutoConfiguration Auto-configuration} for QianFan Embedding Model.
 *
 * @author Geng Rong
 * @author Ilayaperumal Gopinathan
 */
@AutoConfiguration(after = { RestClientAutoConfiguration.class, SpringAiRetryAutoConfiguration.class })
@ConditionalOnClass(QianFanApi.class)
@ConditionalOnProperty(name = SpringAIModelProperties.EMBEDDING_MODEL, havingValue = PROVIDER_NAME,
		matchIfMissing = true)
@EnableConfigurationProperties({ QianFanConnectionProperties.class, QianFanEmbeddingProperties.class })
public class QianFanEmbeddingAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(name = "spring.ai.qianfan.api-version", havingValue = "V1", matchIfMissing = true)
	public QianFanEmbeddingModel qianFanEmbeddingModel(QianFanConnectionProperties commonProperties,
			QianFanEmbeddingProperties embeddingProperties,
			ObjectProvider<RestClient.Builder> restClientBuilderProvider, RetryTemplate retryTemplate,
			ResponseErrorHandler responseErrorHandler, ObjectProvider<ObservationRegistry> observationRegistry,
			ObjectProvider<EmbeddingModelObservationConvention> observationConvention) {

		var qianFanApi = qianFanApi(embeddingProperties.getBaseUrl(), commonProperties.getBaseUrl(),
				embeddingProperties.getApiKey(), commonProperties.getApiKey(), embeddingProperties.getSecretKey(),
				commonProperties.getSecretKey(), restClientBuilderProvider.getIfAvailable(RestClient::builder),
				responseErrorHandler);

		var embeddingModel = new QianFanEmbeddingModel(qianFanApi, embeddingProperties.getMetadataMode(),
				embeddingProperties.getOptions(), retryTemplate,
				observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP));

		observationConvention.ifAvailable(embeddingModel::setObservationConvention);

		return embeddingModel;
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(name = "spring.ai.qianfan.api-version", havingValue = "V2", matchIfMissing = true)
	public org.springaicommunity.qianfanv2.QianFanEmbeddingModel qianFanEmbeddingModelV2(
			QianFanConnectionProperties commonProperties, QianFanEmbeddingProperties embeddingProperties,
			ObjectProvider<RestClient.Builder> restClientBuilderProvider, RetryTemplate retryTemplate,
			ResponseErrorHandler responseErrorHandler, ObjectProvider<ObservationRegistry> observationRegistry,
			ObjectProvider<EmbeddingModelObservationConvention> observationConvention) {

		var qianFanApi = qianFanApiV2(embeddingProperties.getBaseUrl(), commonProperties.getBaseUrl(),
				embeddingProperties.getApiKey(), commonProperties.getApiKey(), embeddingProperties.getSecretKey(),
				commonProperties.getSecretKey(), restClientBuilderProvider.getIfAvailable(RestClient::builder),
				responseErrorHandler);

		var embeddingModel = new org.springaicommunity.qianfanv2.QianFanEmbeddingModel(qianFanApi,
				embeddingProperties.getMetadataMode(), embeddingProperties.getOptionsV2(), retryTemplate,
				observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP));

		observationConvention.ifAvailable(embeddingModel::setObservationConvention);

		return embeddingModel;
	}

	private QianFanApi qianFanApi(String baseUrl, String commonBaseUrl, String apiKey, String commonApiKey,
			String secretKey, String commonSecretKey, RestClient.Builder restClientBuilder,
			ResponseErrorHandler responseErrorHandler) {

		String resolvedBaseUrl = StringUtils.hasText(baseUrl) ? baseUrl : commonBaseUrl;
		Assert.hasText(resolvedBaseUrl, "QianFan base URL must be set");

		String resolvedApiKey = StringUtils.hasText(apiKey) ? apiKey : commonApiKey;
		Assert.hasText(resolvedApiKey, "QianFan API key must be set");

		String resolvedSecretKey = StringUtils.hasText(secretKey) ? secretKey : commonSecretKey;
		Assert.hasText(resolvedSecretKey, "QianFan Secret key must be set");

		return new QianFanApi(resolvedBaseUrl, resolvedApiKey, resolvedSecretKey, restClientBuilder,
				responseErrorHandler);
	}

	private org.springaicommunity.qianfanv2.api.QianFanApi qianFanApiV2(String baseUrl, String commonBaseUrl,
			String apiKey, String commonApiKey, String secretKey, String commonSecretKey,
			RestClient.Builder restClientBuilder, ResponseErrorHandler responseErrorHandler) {

		String resolvedBaseUrl = StringUtils.hasText(baseUrl) ? baseUrl : commonBaseUrl;
		Assert.hasText(resolvedBaseUrl, "QianFan base URL must be set");

		String resolvedApiKey = StringUtils.hasText(apiKey) ? apiKey : commonApiKey;
		Assert.hasText(resolvedApiKey, "QianFan API key must be set");

		return new org.springaicommunity.qianfanv2.api.QianFanApi(resolvedBaseUrl, resolvedApiKey, restClientBuilder,
				responseErrorHandler);
	}

}
