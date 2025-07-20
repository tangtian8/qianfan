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

import org.springframework.ai.chat.observation.ChatModelObservationConvention;
import org.springframework.ai.model.SpringAIModelProperties;
import org.springaicommunity.qianfan.QianFanChatModel;
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

import static org.springaicommunity.qianfan.api.QianFanConstants.PROVIDER_NAME;

/**
 * Chat {@link AutoConfiguration Auto-configuration} for QianFan Chat Model.
 *
 * @author Geng Rong
 * @author Ilayaperumal Gopinathan
 */
@AutoConfiguration(after = { RestClientAutoConfiguration.class, SpringAiRetryAutoConfiguration.class })
@ConditionalOnClass(QianFanApi.class)
@ConditionalOnProperty(name = SpringAIModelProperties.CHAT_MODEL, havingValue = PROVIDER_NAME, matchIfMissing = true)
@EnableConfigurationProperties({ QianFanConnectionProperties.class, QianFanChatProperties.class })
public class QianFanChatAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(name = "spring.ai.qianfan.api-version", havingValue = "V1", matchIfMissing = true)
	public QianFanChatModel qianFanChatModel(QianFanConnectionProperties commonProperties,
			QianFanChatProperties chatProperties, ObjectProvider<RestClient.Builder> restClientBuilderProvider,
			RetryTemplate retryTemplate, ResponseErrorHandler responseErrorHandler,
			ObjectProvider<ObservationRegistry> observationRegistry,
			ObjectProvider<ChatModelObservationConvention> observationConvention) {

		var qianFanApi = qianFanApi(chatProperties.getBaseUrl(), commonProperties.getBaseUrl(),
				chatProperties.getApiKey(), commonProperties.getApiKey(), chatProperties.getSecretKey(),
				commonProperties.getSecretKey(), restClientBuilderProvider.getIfAvailable(RestClient::builder),
				responseErrorHandler);

		var chatModel = new QianFanChatModel(qianFanApi, chatProperties.getOptions(), retryTemplate,
				observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP));

		observationConvention.ifAvailable(chatModel::setObservationConvention);

		return chatModel;
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(name = "spring.ai.qianfan.api-version", havingValue = "V2", matchIfMissing = true)
	public org.springaicommunity.qianfanv2.QianFanChatModel qianFanChatModelV2(
			QianFanConnectionProperties commonProperties, QianFanChatProperties chatProperties,
			ObjectProvider<RestClient.Builder> restClientBuilderProvider, RetryTemplate retryTemplate,
			ResponseErrorHandler responseErrorHandler, ObjectProvider<ObservationRegistry> observationRegistry,
			ObjectProvider<ChatModelObservationConvention> observationConvention) {

		var qianFanApi = qianFanApiv2(chatProperties.getBaseUrl(), commonProperties.getBaseUrl(),
				chatProperties.getApiKey(), commonProperties.getApiKey(), chatProperties.getSecretKey(),
				commonProperties.getSecretKey(), restClientBuilderProvider.getIfAvailable(RestClient::builder),
				responseErrorHandler);

		var chatModel = new org.springaicommunity.qianfanv2.QianFanChatModel(qianFanApi, chatProperties.getOptionsV2(),
				retryTemplate, observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP));

		observationConvention.ifAvailable(chatModel::setObservationConvention);

		return chatModel;
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

	private org.springaicommunity.qianfanv2.api.QianFanApi qianFanApiv2(String baseUrl, String commonBaseUrl,
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
