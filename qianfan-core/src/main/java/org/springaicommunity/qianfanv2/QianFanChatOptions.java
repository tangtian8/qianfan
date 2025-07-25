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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springaicommunity.qianfanv2.api.QianFanApi;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.*;

/**
 * QianFanChatOptions represents the options for performing chat completion using the
 * QianFan API. It provides methods to set and retrieve various options like model,
 * frequency penalty, max tokens, etc.
 *
 * @author Geng Rong
 * @author Ilayaperumal Gopinathan
 * @since 1.0
 * @see ChatOptions
 */
@JsonInclude(Include.NON_NULL)
public class QianFanChatOptions implements ToolCallingChatOptions {

	// @formatter:off
	/**
	 * ID of the model to use.
	 */
	private @JsonProperty("model") String model;
	/**
	 * Number between -2.0 and 2.0. Positive values penalize new tokens based on their existing
	 * frequency in the text so far, decreasing the model's likelihood to repeat the same line verbatim.
	 */
	private @JsonProperty("frequency_penalty") Double frequencyPenalty;
	/**
	 * The maximum number of tokens to generate in the chat completion. The total length of input
	 * tokens and generated tokens is limited by the model's context length.
	 */
	private @JsonProperty("max_output_tokens") Integer maxTokens;
	/**
	 * Number between -2.0 and 2.0. Positive values penalize new tokens based on whether they
	 * appear in the text so far, increasing the model's likelihood to talk about new topics.
	 */
	private @JsonProperty("presence_penalty") Double presencePenalty;
	/**
	 * An object specifying the format that the model must output. Setting to { "type":
	 * "json_object" } enables JSON mode, which guarantees the message the model generates is valid JSON.
	 */
	private @JsonProperty("response_format") QianFanApi.ChatCompletionRequest.ResponseFormat responseFormat;
	/**
	 * Up to 4 sequences where the API will stop generating further tokens.
	 */
	private @JsonProperty("stop") List<String> stop;
	/**
	 * What sampling temperature to use, between 0 and 1. Higher values like 0.8 will make the output
	 * more random, while lower values like 0.2 will make it more focused and deterministic. We generally recommend
	 * altering this or top_p but not both.
	 */
	private @JsonProperty("temperature") Double temperature;
	/**
	 * An alternative to sampling with temperature, called nucleus sampling, where the model considers the
	 * results of the tokens with top_p probability mass. So 0.1 means only the tokens comprising the top 10%
	 * probability mass are considered. We generally recommend altering this or temperature but not both.
	 */
	private @JsonProperty("top_p") Double topP;
	// @formatter:on

	@JsonProperty("tools")
	private List<QianFanApi.FunctionTool> tools;

	@JsonProperty("tool_choice")
	private QianFanApi.ChatCompletionRequest.ToolChoice toolChoice;

	@JsonIgnore
	private List<ToolCallback> toolCallbacks = new ArrayList();

	@JsonIgnore
	private Set<String> toolNames = new HashSet();

	@JsonIgnore
	private Boolean internalToolExecutionEnabled;

	@JsonIgnore
	private Map<String, Object> toolContext = new HashMap();

	@Override
	public List<ToolCallback> getToolCallbacks() {
		return this.toolCallbacks;
	}

	@Override
	public void setToolCallbacks(List<ToolCallback> toolCallbacks) {
		Assert.notNull(toolCallbacks, "toolCallbacks cannot be null");
		Assert.noNullElements(toolCallbacks, "toolCallbacks cannot contain null elements");
		this.toolCallbacks = toolCallbacks;
	}

	@Override
	public Set<String> getToolNames() {
		return this.toolNames;
	}

	@Override
	public void setToolNames(Set<String> toolNames) {
		Assert.notNull(toolNames, "toolNames cannot be null");
		Assert.noNullElements(toolNames, "toolNames cannot contain null elements");
		toolNames.forEach((tool) -> Assert.hasText(tool, "toolNames cannot contain empty elements"));
		this.toolNames = toolNames;
	}

	@Override
	public Boolean getInternalToolExecutionEnabled() {
		return this.internalToolExecutionEnabled;
	}

	@Override
	public void setInternalToolExecutionEnabled(Boolean internalToolExecutionEnabled) {
		this.internalToolExecutionEnabled = internalToolExecutionEnabled;
	}

	@Override
	public Map<String, Object> getToolContext() {
		return this.toolContext;
	}

	@Override
	public void setToolContext(Map<String, Object> toolContext) {
		this.toolContext = toolContext;
	}

	public static Builder builder() {
		return new Builder();
	}

	public List<QianFanApi.FunctionTool> getTools() {
		return tools;
	}

	public void setTools(List<QianFanApi.FunctionTool> tools) {
		this.tools = tools;
	}

	public QianFanApi.ChatCompletionRequest.ToolChoice getToolChoice() {
		return toolChoice;
	}

	public void setToolChoice(QianFanApi.ChatCompletionRequest.ToolChoice toolChoice) {
		this.toolChoice = toolChoice;
	}

	public static QianFanChatOptions fromOptions(QianFanChatOptions fromOptions) {
		return QianFanChatOptions.builder()
			.model(fromOptions.getModel())
			.frequencyPenalty(fromOptions.getFrequencyPenalty())
			.maxTokens(fromOptions.getMaxTokens())
			.presencePenalty(fromOptions.getPresencePenalty())
			.responseFormat(fromOptions.getResponseFormat())
			.stop(fromOptions.getStop())
			.temperature(fromOptions.getTemperature())
			.topP(fromOptions.getTopP())
			.tools(fromOptions.getTools())
			.toolChoice(fromOptions.getToolChoice())
			.toolCallbacks(fromOptions.getToolCallbacks())
			.toolNames(fromOptions.getToolNames())
			.internalToolExecutionEnabled(fromOptions.getInternalToolExecutionEnabled())
			.toolContext(fromOptions.getToolContext())
			.build();
	}

	@Override
	public String getModel() {
		return this.model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	@Override
	public Double getFrequencyPenalty() {
		return this.frequencyPenalty;
	}

	public void setFrequencyPenalty(Double frequencyPenalty) {
		this.frequencyPenalty = frequencyPenalty;
	}

	@Override
	public Integer getMaxTokens() {
		return this.maxTokens;
	}

	public void setMaxTokens(Integer maxTokens) {
		this.maxTokens = maxTokens;
	}

	@Override
	public Double getPresencePenalty() {
		return this.presencePenalty;
	}

	public void setPresencePenalty(Double presencePenalty) {
		this.presencePenalty = presencePenalty;
	}

	public QianFanApi.ChatCompletionRequest.ResponseFormat getResponseFormat() {
		return this.responseFormat;
	}

	public void setResponseFormat(QianFanApi.ChatCompletionRequest.ResponseFormat responseFormat) {
		this.responseFormat = responseFormat;
	}

	@Override
	@JsonIgnore
	public List<String> getStopSequences() {
		return getStop();
	}

	@JsonIgnore
	public void setStopSequences(List<String> stopSequences) {
		setStop(stopSequences);
	}

	public List<String> getStop() {
		return this.stop;
	}

	public void setStop(List<String> stop) {
		this.stop = stop;
	}

	@Override
	public Double getTemperature() {
		return this.temperature;
	}

	public void setTemperature(Double temperature) {
		this.temperature = temperature;
	}

	@Override
	public Double getTopP() {
		return this.topP;
	}

	public void setTopP(Double topP) {
		this.topP = topP;
	}

	@Override
	@JsonIgnore
	public Integer getTopK() {
		return null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.model == null) ? 0 : this.model.hashCode());
		result = prime * result + ((this.frequencyPenalty == null) ? 0 : this.frequencyPenalty.hashCode());
		result = prime * result + ((this.maxTokens == null) ? 0 : this.maxTokens.hashCode());
		result = prime * result + ((this.presencePenalty == null) ? 0 : this.presencePenalty.hashCode());
		result = prime * result + ((this.responseFormat == null) ? 0 : this.responseFormat.hashCode());
		result = prime * result + ((this.stop == null) ? 0 : this.stop.hashCode());
		result = prime * result + ((this.temperature == null) ? 0 : this.temperature.hashCode());
		result = prime * result + ((this.topP == null) ? 0 : this.topP.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		QianFanChatOptions other = (QianFanChatOptions) obj;
		if (this.model == null) {
			if (other.model != null) {
				return false;
			}
		}
		else if (!this.model.equals(other.model)) {
			return false;
		}
		if (this.frequencyPenalty == null) {
			if (other.frequencyPenalty != null) {
				return false;
			}
		}
		else if (!this.frequencyPenalty.equals(other.frequencyPenalty)) {
			return false;
		}
		if (this.maxTokens == null) {
			if (other.maxTokens != null) {
				return false;
			}
		}
		else if (!this.maxTokens.equals(other.maxTokens)) {
			return false;
		}
		if (this.presencePenalty == null) {
			if (other.presencePenalty != null) {
				return false;
			}
		}
		else if (!this.presencePenalty.equals(other.presencePenalty)) {
			return false;
		}
		if (this.responseFormat == null) {
			if (other.responseFormat != null) {
				return false;
			}
		}
		else if (!this.responseFormat.equals(other.responseFormat)) {
			return false;
		}
		if (this.stop == null) {
			if (other.stop != null) {
				return false;
			}
		}
		else if (!this.stop.equals(other.stop)) {
			return false;
		}
		if (this.temperature == null) {
			if (other.temperature != null) {
				return false;
			}
		}
		else if (!this.temperature.equals(other.temperature)) {
			return false;
		}
		if (this.topP == null) {
			if (other.topP != null) {
				return false;
			}
		}
		else if (!this.topP.equals(other.topP)) {
			return false;
		}
		return true;
	}

	@Override
	public QianFanChatOptions copy() {
		return fromOptions(this);
	}

	public static class Builder {

		protected QianFanChatOptions options;

		public Builder() {
			this.options = new QianFanChatOptions();
		}

		public Builder(QianFanChatOptions options) {
			this.options = options;
		}

		public Builder model(String model) {
			this.options.model = model;
			return this;
		}

		public Builder frequencyPenalty(Double frequencyPenalty) {
			this.options.frequencyPenalty = frequencyPenalty;
			return this;
		}

		public Builder maxTokens(Integer maxTokens) {
			this.options.maxTokens = maxTokens;
			return this;
		}

		public Builder presencePenalty(Double presencePenalty) {
			this.options.presencePenalty = presencePenalty;
			return this;
		}

		public Builder responseFormat(QianFanApi.ChatCompletionRequest.ResponseFormat responseFormat) {
			this.options.responseFormat = responseFormat;
			return this;
		}

		public Builder stop(List<String> stop) {
			this.options.stop = stop;
			return this;
		}

		public Builder temperature(Double temperature) {
			this.options.temperature = temperature;
			return this;
		}

		public Builder topP(Double topP) {
			this.options.topP = topP;
			return this;
		}

		public Builder tools(List<QianFanApi.FunctionTool> tools) {
			this.options.tools = tools;
			return this;
		}

		public Builder toolChoice(QianFanApi.ChatCompletionRequest.ToolChoice toolChoice) {
			this.options.toolChoice = toolChoice;
			return this;
		}

		public Builder toolNames(Set<String> toolNames) {
			Assert.notNull(toolNames, "toolNames cannot be null");
			this.options.setToolNames(toolNames);
			return this;
		}

		public Builder internalToolExecutionEnabled(@Nullable Boolean internalToolExecutionEnabled) {
			this.options.setInternalToolExecutionEnabled(internalToolExecutionEnabled);
			return this;
		}

		public Builder toolCallbacks(List<ToolCallback> toolCallbacks) {
			this.options.setToolCallbacks(toolCallbacks);
			return this;
		}

		public Builder toolContext(Map<String, Object> toolContext) {
			if (this.options.toolContext == null) {
				this.options.toolContext = toolContext;
			}
			else {
				this.options.toolContext.putAll(toolContext);
			}

			return this;
		}

		public QianFanChatOptions build() {
			return this.options;
		}

	}

}
