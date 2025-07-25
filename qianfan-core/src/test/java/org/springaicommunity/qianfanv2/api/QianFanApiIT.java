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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.*;

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
				ChatCompletionMessage.Role.user);

		ChatCompletionMessage chatCompletionMessageSystem = new ChatCompletionMessage(buildSystemMessage(),
				ChatCompletionMessage.Role.system);
		ResponseEntity<ChatCompletion> response = this.qianFanApi.chatCompletionEntity(new ChatCompletionRequest(
				List.of(chatCompletionMessage, chatCompletionMessageSystem), "ernie-4.5-turbo-128k", 0.7, false));
		log.info("response:{}", response);
		assertThat(response).isNotNull();
		assertThat(response.getBody()).isNotNull();
	}

	@Test
	void chatToolCompletionEntity() {
		ChatCompletionMessage chatCompletionMessage = new ChatCompletionMessage("查一下上海和北京现在的天气",
				ChatCompletionMessage.Role.user);

		List<FunctionTool> tools = new ArrayList<>();
		Map<String, Object> properties = new HashMap<>();

		Map<String, Object> location = new HashMap<>();
		location.put("description", "地理位置，精确到区县级别");
		location.put("type", "string");
		Map<String, Object> time = new HashMap<>();
		time.put("description", "时间，格式为YYYY-MM-DD");
		time.put("type", "string");

		properties.put("location", location);
		properties.put("time", time);

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("properties", properties);
		parameters.put("type", "object");

		Function function = new Function("get_current_weather", "天气查询工具", parameters);
		FunctionTool tool = new FunctionTool(function);
		tools.add(tool);

		ChatCompletionRequest chatCompletionRequest = new ChatCompletionRequest(List.of(chatCompletionMessage),
				"ernie-lite-pro-128k", 0.7, tools, null);
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			// 对象转JSON字符串
			String json = objectMapper.writeValueAsString(chatCompletionRequest);
			log.info("request:{}", json);
		}
		catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		ResponseEntity<ChatCompletion> response = this.qianFanApi.chatCompletionEntity(chatCompletionRequest);
		log.info("response:{}", response.getBody());
		assertThat(response).isNotNull();
		assertThat(response.getBody()).isNotNull();
	}

	@Test
	void chatCompletionStream() {
		ChatCompletionMessage chatCompletionMessage = new ChatCompletionMessage("Hello world",
				ChatCompletionMessage.Role.user);
		ChatCompletionMessage chatCompletionMessageSystem = new ChatCompletionMessage(buildSystemMessage(),
				ChatCompletionMessage.Role.system);
		Flux<ChatCompletionChunk> response = this.qianFanApi.chatCompletionStream(new ChatCompletionRequest(
				List.of(chatCompletionMessage, chatCompletionMessageSystem), "ernie-4.5-turbo-128k", 0.7, true));
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
		assertThat(response.getBody().data().get(0).embedding()).hasSize(384);
		log.info("ChatCompletionChunk: {}", response.getBody().data());

	}

	private String buildSystemMessage() {
		String systemMessageTemplate = ResourceUtils.getText("classpath:/prompts/system-message.st");
		ST st = new ST(systemMessageTemplate, '{', '}');
		return st.add("name", "QianFan").add("voice", "pirate").render();
	}

}
