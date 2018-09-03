FROM debian:jessie

RUN echo 'deb http://deb.debian.org/debian jessie-backports main' > /etc/apt/sources.list.d/jessie-backports.list

ENV DEBIAN_FRONTEND noninteractive

RUN apt update -y

RUN apt install --target-release jessie-backports \
          openjdk-8-jdk \
          ca-certificates-java \
          --assume-yes

RUN apt install -qq --force-yes mesa-utils libgl1-mesa-glx libxcursor1 libxrandr2 libxxf86vm1 x11-xserver-utils xfonts-base xserver-common

ADD . /code

RUN dpkg -i /code/xvfb_1.16.4-1_amd64.deb

WORKDIR /code

RUN ./gradlew assemble