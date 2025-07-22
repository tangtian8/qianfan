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

import org.springframework.ai.image.observation.ImageModelObservationConvention;
import org.springframework.ai.model.SpringAIModelProperties;
import org.springaicommunity.qianfan.QianFanImageModel;
import org.springaicommunity.qianfan.api.QianFanApi;
import org.springaicommunity.qianfan.api.QianFanImageApi;
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
 * Image {@link AutoConfiguration Auto-configuration} for QianFan Image Model.
 *
 * @author Geng Rong
 * @author Ilayaperumal Gopinathan
 */
@AutoConfiguration(after = { RestClientAutoConfiguration.class, SpringAiRetryAutoConfiguration.class })
@ConditionalOnClass(QianFanApi.class)
@ConditionalOnProperty(name = SpringAIModelProperties.IMAGE_MODEL, havingValue = PROVIDER_NAME, matchIfMissing = true)
@EnableConfigurationProperties({ QianFanConnectionProperties.class, QianFanImageProperties.class })
public class QianFanImageAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(name = "spring.ai.qianfan.api-version", havingValue = "V1", matchIfMissing = true)
	public QianFanImageModel qianFanImageModel(QianFanConnectionProperties commonProperties,
			QianFanImageProperties imageProperties, ObjectProvider<RestClient.Builder> restClientBuilderProvider,
			RetryTemplate retryTemplate, ResponseErrorHandler responseErrorHandler,
			ObjectProvider<ObservationRegistry> observationRegistry,
			ObjectProvider<ImageModelObservationConvention> observationConvention) {

		String apiKey = StringUtils.hasText(imageProperties.getApiKey()) ? imageProperties.getApiKey()
				: commonProperties.getApiKey();

		String secretKey = StringUtils.hasText(imageProperties.getSecretKey()) ? imageProperties.getSecretKey()
				: commonProperties.getSecretKey();

		String baseUrl = StringUtils.hasText(imageProperties.getBaseUrl()) ? imageProperties.getBaseUrl()
				: commonProperties.getBaseUrl();

		Assert.hasText(apiKey, "QianFan API key must be set.  Use the property: spring.ai.qianfan.api-key");
		Assert.hasText(secretKey, "QianFan secret key must be set.  Use the property: spring.ai.qianfan.secret-key");
		Assert.hasText(baseUrl, "QianFan base URL must be set.  Use the property: spring.ai.qianfan.base-url");

		var qianFanImageApi = new QianFanImageApi(baseUrl, apiKey, secretKey,
				restClientBuilderProvider.getIfAvailable(RestClient::builder), responseErrorHandler);

		var imageModel = new QianFanImageModel(qianFanImageApi, imageProperties.getOptions(), retryTemplate,
				observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP));

		observationConvention.ifAvailable(imageModel::setObservationConvention);

		return imageModel;
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(name = "spring.ai.qianfan.api-version", havingValue = "V2", matchIfMissing = true)
	public org.springaicommunity.qianfanv2.QianFanImageModel qianFanImageModelV2(
			QianFanConnectionProperties commonProperties, QianFanImageProperties imageProperties,
			ObjectProvider<RestClient.Builder> restClientBuilderProvider, RetryTemplate retryTemplate,
			ResponseErrorHandler responseErrorHandler, ObjectProvider<ObservationRegistry> observationRegistry,
			ObjectProvider<ImageModelObservationConvention> observationConvention) {

		String apiKey = StringUtils.hasText(imageProperties.getApiKey()) ? imageProperties.getApiKey()
				: commonProperties.getApiKey();

		String secretKey = StringUtils.hasText(imageProperties.getSecretKey()) ? imageProperties.getSecretKey()
				: commonProperties.getSecretKey();

		String baseUrl = StringUtils.hasText(imageProperties.getBaseUrl()) ? imageProperties.getBaseUrl()
				: commonProperties.getBaseUrl();

		Assert.hasText(apiKey, "QianFan API key must be set.  Use the property: spring.ai.qianfan.api-key");
		Assert.hasText(secretKey, "QianFan secret key must be set.  Use the property: spring.ai.qianfan.secret-key");
		Assert.hasText(baseUrl, "QianFan base URL must be set.  Use the property: spring.ai.qianfan.base-url");

		var qianFanImageApi = new org.springaicommunity.qianfanv2.api.QianFanImageApi(baseUrl, apiKey,
				restClientBuilderProvider.getIfAvailable(RestClient::builder), responseErrorHandler);

		var imageModel = new org.springaicommunity.qianfanv2.QianFanImageModel(qianFanImageApi,
				imageProperties.getOptionsV2(), retryTemplate,
				observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP));

		observationConvention.ifAvailable(imageModel::setObservationConvention);

		return imageModel;
	}

}
