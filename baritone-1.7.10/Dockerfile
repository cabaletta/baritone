FROM docker.io/eclipse-temurin:8-jdk

RUN apt-get update && apt-get install -y wget unzip && rm -rf /var/lib/apt/lists/*

RUN wget https://services.gradle.org/distributions/gradle-1.12-bin.zip && \
    unzip gradle-1.12-bin.zip && \
    rm gradle-1.12-bin.zip && \
    mv gradle-1.12 /opt/gradle

ENV PATH /opt/gradle/bin:${PATH}

WORKDIR /project

CMD ["gradle", "build"]