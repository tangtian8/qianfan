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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.qianfanv2.api.QianFanApi.*;
import org.springframework.ai.util.ResourceUtils;
import org.springframework.http.ResponseEntity;
import org.stringtemplate.v4.ST;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Geng Rong
 */
@EnabledIfEnvironmentVariables({ @EnabledIfEnvironmentVariable(named = "QIANFAN_API_KEY", matches = ".+") })
public class QianFanApiIT {

	private static final Logger log = LoggerFactory.getLogger(QianFanApiIT.class);

	org.springaicommunity.qianfanv2.api.QianFanApi qianFanApi = new org.springaicommunity.qianfanv2.api.QianFanApi(
			System.getenv("QIANFAN_API_KEY"));

	@Test
	void chatCompletionEntity() {
		ChatCompletionMessage chatCompletionMessage = new ChatCompletionMessage("Hello world",
				ChatCompletionMessage.Role.USER);
		ResponseEntity<ChatCompletion> response = this.qianFanApi.chatCompletionEntity(new ChatCompletionRequest(
				List.of(chatCompletionMessage), buildSystemMessage(), "ernie-4.0-8k", 0.7, false));
		log.info("response:{}", response);
		assertThat(response).isNotNull();
		assertThat(response.getBody()).isNotNull();
	}

	@Test
	void chatCompletionStream() {
		ChatCompletionMessage chatCompletionMessage = new ChatCompletionMessage("Hello world",
				ChatCompletionMessage.Role.USER);
		Flux<ChatCompletionChunk> response = this.qianFanApi.chatCompletionStream(new ChatCompletionRequest(
				List.of(chatCompletionMessage), buildSystemMessage(), "ernie-4.0-8k", 0.7, true));
		// log.info("response:{}",response.collectList().block());
		assertThat(response).isNotNull();
		response.doOnNext(chunk -> {
			// 或者使用日志
			log.info("ChatCompletionChunk: {}", chunk.choices().get(0).message().content());
		})
			.doOnComplete(() -> System.out.println("流完成"))
			.doOnError(error -> System.err.println("发生错误: " + error.getMessage()))
			.subscribe();
		assertThat(response.collectList().block()).isNotNull();

	}

	@Test
	void embeddings() {
		ResponseEntity<EmbeddingList> response = this.qianFanApi
			.embeddings(new QianFanApi.EmbeddingRequest("Hello world"));

		assertThat(response).isNotNull();
		assertThat(Objects.requireNonNull(response.getBody()).data()).hasSize(1);
		assertThat(response.getBody().data().get(0).embedding()).hasSize(1024);
		log.info("ChatCompletionChunk: {}", response.getBody().data());

	}

	private String buildSystemMessage() {
		String systemMessageTemplate = ResourceUtils.getText("classpath:/prompts/system-message.st");
		ST st = new ST(systemMessageTemplate, '{', '}');
		return st.add("name", "QianFan").add("voice", "pirate").render();
	}

}
