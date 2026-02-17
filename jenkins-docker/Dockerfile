FROM jenkins/jenkins:lts-jdk21

USER root

# Installer dépendances
RUN apt-get update && \
    apt-get install git && \
    apt-get install -y maven docker.io && \
    rm -rf /var/lib/apt/lists/*

# Ajouter user jenkins au groupe docker
RUN usermod -aG docker jenkins

USER jenkins