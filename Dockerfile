FROM debian:stretch

ENV DEBIAN_FRONTEND noninteractive

RUN apt update -y

RUN apt install \
          openjdk-8-jdk \
          --assume-yes

COPY . /code

WORKDIR /code

RUN ./gradlew build
