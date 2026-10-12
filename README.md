# AgentScope Implementation

Simple Spring Boot application using **AgentScope Java** and OpenAI for chat and search-style prompts.

## Stack

- Java 25, Spring Boot 4.1.1
- AgentScope Java 2.0.3
- OpenAI model: `gpt-4o-mini`
- Maven; HTTP port `8080`

## Run locally

Prerequisites: Java 25, Maven, and an OpenAI API key.

```bash
git clone https://github.com/commitbyrajat/agentscope-impl.git
cd agentscope-impl
export OPENAI_API_KEY="your-api-key"
mvn spring-boot:run
```

## Test APIs

**Chat**

```bash
curl -G "http://localhost:8080/chat" \
  --data-urlencode "prompt=Why is the sky blue?"
```

**Search**

```bash
curl -G "http://localhost:8080/search" \
  --data-urlencode "prompt=What is the latest LTS release of Java and its key features?"
```

## Build JAR

```bash
mvn clean package
java -jar target/agent-scope-impl-1.0-SNAPSHOT.jar
```

## Docker

If a `Dockerfile` is present in the repository root:

```bash
docker build -t agentscope-impl .
docker run --rm -p 8080:8080 -e OPENAI_API_KEY="$OPENAI_API_KEY" agentscope-impl
```

## Configuration

Edit `src/main/resources/application.yml` to change the model, logging, or port. Keep API keys in environment variables, not in source control.

Repository: https://github.com/commitbyrajat/agentscope-impl
