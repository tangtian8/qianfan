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

import org.springaicommunity.qianfan.QianFanImageOptions;
import org.springaicommunity.qianfan.api.QianFanImageApi;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * QianFan Image autoconfiguration properties.
 *
 * @author Geng Rong
 */
@ConfigurationProperties(QianFanImageProperties.CONFIG_PREFIX)
public class QianFanImageProperties extends QianFanParentProperties {

	public static final String CONFIG_PREFIX = "spring.ai.qianfan.image";

	public static final String DEFAULT_IMAGE_MODEL = QianFanImageApi.ImageModel.Stable_Diffusion_XL.getValue();

	public static final String DEFAULT_IMAGE_MODEL_V2 = org.springaicommunity.qianfanv2.api.QianFanImageApi.ImageModel.ERNIE_iRAG_1
		.getValue();

	/**
	 * Options for QianFan Image API.
	 */
	@NestedConfigurationProperty
	private QianFanImageOptions options = QianFanImageOptions.builder().model(DEFAULT_IMAGE_MODEL).build();

	/**
	 * Options for QianFan Image API.
	 */
	@NestedConfigurationProperty
	private org.springaicommunity.qianfanv2.QianFanImageOptions optionsV2 = org.springaicommunity.qianfanv2.QianFanImageOptions
		.builder()
		.model(DEFAULT_IMAGE_MODEL_V2)
		.build();

	public QianFanImageOptions getOptions() {
		return this.options;
	}

	public void setOptions(QianFanImageOptions options) {
		this.options = options;
	}

	public org.springaicommunity.qianfanv2.QianFanImageOptions getOptionsV2() {
		return this.optionsV2;
	}

}
