pipeline {
    agent any

    tools {
        jdk 'Default'
        maven 'Maven-3.9'
    }

    environment {
        DOCKER_REGISTRY = "docker.io"
        DOCKER_IMAGE = "nandraina/challenge-springboot"
        DOCKER_TAG = "latest"
    }

    stages {
        stage('Checkout') {
            steps {
                git url: ' git@github.com:jerryalex15/challengeFullstackDevOps.git',
                    credentialsId: 'GitHub-cred',
                    branch: 'develop'
            }
        }

        stage('Build') {
            steps {
                sh 'mvn clean verify'
            }
        }

        stage('Sonar') {
            steps {
                sh 'mvn sonar:sonar'
            }
        }

        stage('Build Docker Image') {
            steps {
                sh """
                docker build -t $DOCKER_IMAGE:$DOCKER_TAG .
                """
            }
        }

        stage('Push Docker Image') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'docker-hub-cred', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                    sh """
                    echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin
                    docker push $DOCKER_IMAGE:$DOCKER_TAG
                    """
                }
            }
        }
    }
}