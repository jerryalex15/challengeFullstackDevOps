pipeline {
    agent any

    environment {
        DOCKER_REGISTRY = "docker.io"
        DOCKER_IMAGE = "nandraina/challenge-springboot"
        DOCKER_TAG = "latest"
    }

    stages {
        stage('Checkout') {
            steps {
                deleteDir() // supprime le workspace
                git branch: 'develop',
                    url: 'https://github.com/jerryalex15/challengeFullstackDevOps.git',
                    credentialsId: 'github-token-id'
            }
        }

        stage('Build') {
            steps {
                withCredentials([string(credentialsId: 'nvd-api-key-id', variable: 'NVD_API_KEY')]) {
                    sh '''
                        echo "Building project..."
                        mvn clean verify -Dnvd.api.key="$NVD_API_KEY"
                    '''
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