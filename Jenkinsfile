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
                        mvn sonar:sonar \
                          -Dsonar.projectKey=jerryalex15_challengeFullstackDevOps \
                          -Dsonar.organization=jerryalex15 \
                          -Dsonar.host.url=https://sonarcloud.io \
                          -Dsonar.token=$SONAR_AUTH_TOKEN \
                          -Dsonar.branch.name=develop \
                          -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
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
                withCredentials([
                    usernamePassword(
                        credentialsId: 'docker-hub-cred',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS'
                    )
                ]) {
                    sh '''
                        echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
                        docker buildx build \
                            --platform linux/amd64 \
                            -t $DOCKER_IMAGE:$DOCKER_TAG \
                            --push .
                    '''
                }
            }
        }
        stage('Deploy to Oracle VM') {
            steps {
                withCredentials([
                    string(credentialsId: 'Oracle-vm-ip', variable: 'VM_IP'),
                    file(credentialsId: 'jenkins-private-key-file', variable: 'PRIVATE_KEY_FILE'),
                    file(credentialsId: 'jenkins-public-key-file', variable: 'PUBLIC_KEY_FILE')
                ]) {
                    sshagent(credentials: ['oracle-vm-ssh']) {
                        sh """
                            # Prépare le dossier
                            ssh -o StrictHostKeyChecking=no opc@\$VM_IP 'sudo rm -f /home/opc/challengeFullstackDevOps/secrets/*.pem'
                            ssh -o StrictHostKeyChecking=no opc@\$VM_IP 'chmod 755 /home/opc/challengeFullstackDevOps/secrets'

                            # Copie les clés
                            scp -o StrictHostKeyChecking=no \$PRIVATE_KEY_FILE \
                                opc@\$VM_IP:/home/opc/challengeFullstackDevOps/secrets/private_key.pem
                            scp -o StrictHostKeyChecking=no \$PUBLIC_KEY_FILE \
                                opc@\$VM_IP:/home/opc/challengeFullstackDevOps/secrets/public_key.pem

                            # Sécurise les fichiers, dossier lisible par Docker
                            ssh -o StrictHostKeyChecking=no opc@\$VM_IP 'chmod 644 /home/opc/challengeFullstackDevOps/secrets/*.pem'

                            # Copie docker-compose
                            scp -o StrictHostKeyChecking=no docker-compose.yml \
                                opc@\$VM_IP:/home/opc/challengeFullstackDevOps/docker-compose.yml

                            # Lance
                            ssh -o StrictHostKeyChecking=no opc@\$VM_IP '
                                cd /home/opc/challengeFullstackDevOps
                                docker pull nandraina/challenge-springboot:latest
                                docker compose down || true
                                docker compose up -d
                            '
                        """
                    }
                }
            }
        }
    }
}