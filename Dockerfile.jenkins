FROM jenkins/jenkins:lts-jdk21

USER root

RUN apt-get update && \
    apt-get install -y git maven docker.io && \
    rm -rf /var/lib/apt/lists/*

USER jenkins

ENV JENKINS_HOME=/var/jenkins_home
VOLUME /var/jenkins_home

EXPOSE 8080 50000