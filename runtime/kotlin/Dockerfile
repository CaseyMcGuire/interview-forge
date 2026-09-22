FROM eclipse-temurin:21-jdk-jammy

ARG KOTLIN_VERSION=2.4.20
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl unzip \
    && curl --fail --location --output /tmp/kotlin.zip \
      "https://github.com/JetBrains/kotlin/releases/download/v${KOTLIN_VERSION}/kotlin-compiler-${KOTLIN_VERSION}.zip" \
    && unzip -q /tmp/kotlin.zip -d /opt \
    && rm /tmp/kotlin.zip \
    && rm -rf /var/lib/apt/lists/*

ENV PATH="/opt/kotlinc/bin:${PATH}"
ENV JAVA_TOOL_OPTIONS="-XX:ActiveProcessorCount=1 -XX:MaxRAMPercentage=70 -XX:+ExitOnOutOfMemoryError"
ENV JAVA_OPTS="-Xms32m -Xmx512m"

# Built with ./gradlew :kotlin-runtime:installDist before building this image.
COPY build/install/kotlin-runtime/lib/ /opt/interview-forge/lib/

# kotlinc needs explicit JAR paths; use the same libraries as the execution JVM.
RUN printf '%s\n' '-classpath' \
      "$(find /opt/interview-forge/lib -name '*.jar' | sort | paste -sd:)" \
      > /opt/interview-forge/compiler.args

WORKDIR /workspace
USER 65534:65534
