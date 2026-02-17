pipeline {
    agent any

    environment {
        DOCKER_REGISTRY = "docker.io"
        DOCKER_IMAGE = "nandraina/challenge-springboot"
        DOCKER_TAG = "latest"
        TESTCONTAINERS_RYUK_DISABLED = "true"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        stage('Build') {
            steps {
                withCredentials([string(credentialsId: 'nvd-api-key-id', variable: 'NVD_API_KEY')]) {
                    sh(script: "mvn clean verify -Dnvd.api.key=\$NVD_API_KEY", returnStdout: true)
                }
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

        // Push to registry
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