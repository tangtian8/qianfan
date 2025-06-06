# Spring AI QianFan [![Maven Central Version](https://img.shields.io/maven-central/v/org.springaicommunity/qianfan-core)](https://central.sonatype.com/artifact/org.springaicommunity/qianfan-core/)

This module provides integration with Baidu's QianFan AI platform, offering a comprehensive suite of AI capabilities including chat models, embedding models, and image generation.

## Features

- **Chat Models**: Access to QianFan's conversational AI models for building intelligent chatbots and assistants.
- **Embedding Models**: Generate text embeddings for semantic search and text similarity analysis.
- **Image Generation**: Create AI-generated images using QianFan's CogView model.

## Requirements

- Java 17 or later
- Maven 3.6+
- QianFan API credentials (API Key and Secret Key)

## Getting Started

### Build the Project

You can build the project using either Maven or the Maven wrapper:

```bash
# Using Maven
mvn clean install

# Using Maven wrapper
./mvnw clean install
```

### Building the Documentation

To build the documentation site:

```bash
./mvnw -pl docs antora
```

This will generate the documentation site in `/docs/target/antora/site`. You can open the generated HTML files in your browser to view the documentation.

### Usage

Add the dependency to your project:

```xml
<dependency>
    <groupId>org.springaicommunity</groupId>
    <artifactId>qianfan-core</artifactId>
    <version>last-version</version>
</dependency>
```

For Gradle projects:

```groovy
dependencies {
    implementation 'org.springaicommunity:qianfan-core:last-version'
}
```

### Configuration

Set the following environment variables with your QianFan credentials:

```bash
export SPRING_AI_QIANFAN_API_KEY=<INSERT API KEY HERE>
export SPRING_AI_QIANFAN_SECRET_KEY=<INSERT SECRET KEY HERE>
```

Alternatively, you can configure these properties in your `application.properties` file:

```properties
spring.ai.qianfan.api-key=YOUR_API_KEY
spring.ai.qianfan.secret-key=YOUR_SECRET_KEY
```

## Documentation

For more detailed documentation, please build the documentation locally using the command above, or refer to the GitHub repository's documentation section.

## License

This project is licensed under the Apache License 2.0 - see the LICENSE file for details.