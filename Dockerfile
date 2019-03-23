FROM debian:stretch

RUN echo 'deb http://deb.debian.org/debian stretch-backports main' > /etc/apt/sources.list.d/stretch-backports.list

ENV DEBIAN_FRONTEND noninteractive

RUN apt update -y

RUN apt install \
          openjdk-8-jdk \
          --assume-yes

RUN apt install -qq --assume-yes mesa-utils libgl1-mesa-glx libxcursor1 libxrandr2 libxxf86vm1 x11-xserver-utils xfonts-base xserver-common

COPY . /code

WORKDIR /code

# this .deb is specially patched to support lwjgl
# source: https://github.com/tectonicus/tectonicus/issues/60#issuecomment-154239173
RUN dpkg -i scripts/xvfb_1.16.4-1_amd64.deb

RUN ./gradlew build
