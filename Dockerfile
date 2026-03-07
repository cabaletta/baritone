FROM docker.io/eclipse-temurin:8-jdk

RUN apt-get update     && apt-get install -y --no-install-recommends wget unzip ca-certificates     && rm -rf /var/lib/apt/lists/*

ARG GRADLE_VERSION=1.12
RUN wget -q https://services.gradle.org/distributions/gradle--bin.zip -O /tmp/gradle.zip     && unzip -q /tmp/gradle.zip -d /opt     && rm /tmp/gradle.zip     && ln -s /opt/gradle- /opt/gradle

ENV PATH=/opt/gradle/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/usr/games:/usr/local/games:/usr/lib/wsl/lib:/mnt/c/Users/ehara/.codex/tmp/arg0/codex-arg0uIFxup:/mnt/c/Program
