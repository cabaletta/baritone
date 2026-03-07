FROM docker.io/eclipse-temurin:8-jdk

RUN apt-get update \
    && apt-get install -y --no-install-recommends wget unzip ca-certificates \
    && rm -rf /var/lib/apt/lists/*

ARG GRADLE_VERSION=1.12
RUN wget -q "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -O /tmp/gradle.zip \
    && unzip -q /tmp/gradle.zip -d /opt \
    && rm /tmp/gradle.zip \
    && ln -s "/opt/gradle-${GRADLE_VERSION}" /opt/gradle

ENV PATH="/opt/gradle/bin:${PATH}"

WORKDIR /workspace/baritone-1.7.10
CMD ["gradle", "build"]
