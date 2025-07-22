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

import org.springaicommunity.qianfanv2.QianFanChatModel;
import org.springaicommunity.qianfanv2.QianFanEmbeddingModel;
import org.springaicommunity.qianfanv2.QianFanImageModel;
import org.springaicommunity.qianfanv2.api.QianFanApi;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.image.ImageModel;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

/**
 * @author Geng Rong
 */
@SpringBootConfiguration
public class QianFanTestConfiguration {

	@Bean
	public org.springaicommunity.qianfanv2.api.QianFanApi qianFanApi() {
		return new QianFanApi(getApiKey());
	}

	@Bean
	public org.springaicommunity.qianfanv2.api.QianFanImageApi qianFanImageApi() {
		return new org.springaicommunity.qianfanv2.api.QianFanImageApi(getApiKey());
	}

	private String getApiKey() {
		String apiKey = System.getenv("QIANFAN_API_KEY");
		if (!StringUtils.hasText(apiKey)) {
			throw new IllegalArgumentException(
					"You must provide an API key. Put it in an environment variable under the name QIANFAN_API_KEY");
		}
		return apiKey;
	}

	@Bean
	public org.springaicommunity.qianfanv2.QianFanChatModel qianFanChatModel(QianFanApi api) {
		return new org.springaicommunity.qianfanv2.QianFanChatModel(api);
	}

	@Bean
	public EmbeddingModel qianFanEmbeddingModel(org.springaicommunity.qianfanv2.api.QianFanApi api) {
		return new org.springaicommunity.qianfanv2.QianFanEmbeddingModel(api);
	}

	@Bean
	public ImageModel qianFanImageModel(org.springaicommunity.qianfanv2.api.QianFanImageApi api) {
		return new org.springaicommunity.qianfanv2.QianFanImageModel(api);
	}

}
