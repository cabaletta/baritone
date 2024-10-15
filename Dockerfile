FROM ubuntu:focal

ENV DEBIAN_FRONTEND noninteractive

RUN apt update -y

RUN apt install \
          openjdk-21-jdk \
	  git \
          --assume-yes

COPY . /code

WORKDIR /code

RUN ./gradlew build
