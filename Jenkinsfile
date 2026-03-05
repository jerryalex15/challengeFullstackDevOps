pipeline {
    agent any

    environment {
        DOCKER_REGISTRY = "docker.io"
        DOCKER_IMAGE = "nandraina/challenge-springboot"
        DOCKER_TAG = "latest"
        PRIVATE_KEY_PATH = credentials('jenkins-private-key-file')
        PUBLIC_KEY_PATH  = credentials('jenkins-public-key-file')
        TESTCONTAINERS_RYUK_DISABLED=true
        // IMPORTANT pour macOS + Testcontainers
        TESTCONTAINERS_HOST_OVERRIDE = "host.docker.internal"
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

        stage('Build + Tests') {
            steps {
                withCredentials([string(credentialsId: 'nvd-api-key-id', variable: 'NVD_API_KEY')]) {
                    withEnv(["MAVEN_OPTS=-DnvdApiKey=$NVD_API_KEY"]) {
                        sh """
                        mvn clean verify \
                          -Ddependency-check.forceUpdate=true
                        """
                    }
                }
            }
        }

        stage('Sonar Analysis') {
            steps {
                withSonarQubeEnv('SonarCloud') {
                    withCredentials([string(credentialsId: 'sonar-token', variable: 'SONAR_AUTH_TOKEN')]) {
                        sh '''
                        mvn verify sonar:sonar \
                          -Dsonar.projectKey=jerryalex15_challengeFullstackDevOps \
                          -Dsonar.organization=jerryalex15 \
                          -Dsonar.host.url=https://sonarcloud.io \
                          -Dsonar.token=$SONAR_AUTH_TOKEN \
                          -Dsonar.branch.name=develop
                        '''
                    }
                }
            }
        }

        stage("Quality Gate") {
            steps {
                timeout(time: 15, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
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
                withCredentials([
                    usernamePassword(
                        credentialsId: 'docker-hub-cred',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS'
                    )
                ]) {
                    sh '''
                    echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
                    docker push $DOCKER_IMAGE:$DOCKER_TAG
                    '''
                }
            }
        }
        stage('Deploy to Oracle VM') {
            steps {
                // 'variable' est le nom interne Jenkins, on l'utilise directement dans le SH
                withCredentials([string(credentialsId: 'Oracle-vm-ip', variable: 'VM_IP')]) {
                    sshagent(credentials: ['oracle-vm-ssh']) {
                        sh '''
                            # Ici on utilise des guillemets simples pour le bloc SH
                            # pour que ce soit le shell qui lise la variable d'environnement

                            ssh -o StrictHostKeyChecking=no opc@${VM_IP} << 'EOF'
                                mkdir -p /home/opc/challengeFullstackDevOps
                                cd /home/opc/challengeFullstackDevOps

                                docker pull nandraina/challenge-springboot:latest

                                # Le "|| true" évite que le pipeline plante si c'est le premier déploiement
                                docker compose down || true
                                docker compose up -d
                            EOF
                        '''
                    }
                }
            }
        }
    }
}