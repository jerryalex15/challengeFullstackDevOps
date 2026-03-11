pipeline {
    agent { label 'local-machine-agent' }

    environment {
        DOCKER_REGISTRY = "docker.io"
        DOCKER_IMAGE = "nandraina/challenge-springboot"
        DOCKER_TAG = "latest"
    }
    stages {

        stage('Checkout') {
            steps {
                git branch: 'develop',
                    url: 'https://github.com/jerryalex15/challengeFullstackDevOps.git',
                    credentialsId: 'github-credential-ci'
            }
        }

        stage('Build + Tests') {
            steps {
                withCredentials([
                    file(credentialsId: 'jenkins-private-key-file', variable: 'PRIVATE_KEY_PATH'),
                    file(credentialsId: 'jenkins-public-key-file',  variable: 'PUBLIC_KEY_PATH'),
                    string(credentialsId: 'nvd-api-key', variable: 'NVD_API_KEY')
                ]) {
                    withEnv(["MAVEN_OPTS=-DnvdApiKey=$NVD_API_KEY"]) {
                    sh """
                        export PRIVATE_KEY_PATH=\$PRIVATE_KEY_PATH
                        export PUBLIC_KEY_PATH=\$PUBLIC_KEY_PATH
                        mvn clean verify \
                          -Ddependency-check.forceUpdate=true \
                          -DnvdApiKey=\$NVD_API_KEY
                    """
                    }
                }
            }
        }

        stage('Sonar Analysis') {
            steps {
                withSonarQubeEnv('SonarCloud') {
                    withCredentials([string(credentialsId: 'jenkins-sonar-angular-token', variable: 'SONAR_AUTH_TOKEN')]) {
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
                timeout(time: 10, unit: 'MINUTES') {
                    retry(2) { waitForQualityGate abortPipeline: true }
                }
            }
        }
        stage('Build Docker Image') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'Jenkins-ci-docker-hub-credential',
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
                    string(credentialsId: 'oracle-vm-ip', variable: 'VM_IP'),
                    file(credentialsId: 'jenkins-private-key-file', variable: 'PRIVATE_KEY_FILE'),
                    file(credentialsId: 'jenkins-public-key-file', variable: 'PUBLIC_KEY_FILE')
                ]) {
                    sshagent(credentials: ['oracle-vm-ssh']) {
                        sh """

                            ssh -o StrictHostKeyChecking=no opc@\$VM_IP 'sudo rm -f /home/opc/challengeFullstackDevOps/secrets/*.pem'
                            ssh -o StrictHostKeyChecking=no opc@\$VM_IP 'chmod 755 /home/opc/challengeFullstackDevOps/secrets'

                            scp -o StrictHostKeyChecking=no \$PRIVATE_KEY_FILE \
                                opc@\$VM_IP:/home/opc/challengeFullstackDevOps/secrets/private_key.pem
                            scp -o StrictHostKeyChecking=no \$PUBLIC_KEY_FILE \
                                opc@\$VM_IP:/home/opc/challengeFullstackDevOps/secrets/public_key.pem

                            ssh -o StrictHostKeyChecking=no opc@\$VM_IP 'chmod 644 /home/opc/challengeFullstackDevOps/secrets/*.pem'

                            scp -o StrictHostKeyChecking=no docker-compose.yml \
                                opc@\$VM_IP:/home/opc/challengeFullstackDevOps/docker-compose.yml
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

    post {
        always {
            node('local-machine-agent') {
                sh 'rm -f secrets/*.pem || true'
            }
        }
    }
}