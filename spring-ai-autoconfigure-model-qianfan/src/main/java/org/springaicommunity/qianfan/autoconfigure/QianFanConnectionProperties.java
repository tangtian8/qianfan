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

import org.springaicommunity.qianfan.api.QianFanConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(QianFanConnectionProperties.CONFIG_PREFIX)
public class QianFanConnectionProperties extends QianFanParentProperties {

	public static final String CONFIG_PREFIX = "spring.ai.qianfan";

	public static final String DEFAULT_BASE_URL = QianFanConstants.DEFAULT_BASE_URL;

	public static final String DEFAULT_BASE_URL_V2 = org.springaicommunity.qianfanv2.api.QianFanConstants.DEFAULT_BASE_URL;

	@Value("spring.ai.qianfan.api-version")
	private String version;

	public QianFanConnectionProperties() {

		if ("V2".equals(version)) {
			super.setBaseUrl(DEFAULT_BASE_URL_V2);
		}
		else {
			super.setBaseUrl(DEFAULT_BASE_URL);

		}
	}

}
