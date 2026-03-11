pipeline {
    agent { label 'local-machine-agent' }

    environment {
        DOCKER_REGISTRY = "docker.io"
        DOCKER_IMAGE    = "nandraina/challenge-springboot"
        DOCKER_TAG      = "latest"
        DEPLOY_DIR      = "/home/opc/challengeFullstackDevOps"
        SECRETS_DIR     = "/home/opc/challengeFullstackDevOps/secrets"
        REMOTE_USER     = "opc"
        SSH_OPTS        = "-o StrictHostKeyChecking=no"
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
                    sh """
                        # ── 0. Définition des variables
                        chmod 600 \$PRIVATE_KEY_FILE
                        SSH_OPTS="-i \$PRIVATE_KEY_FILE -o StrictHostKeyChecking=no"

                        # ── 1. Copie atomique des clés (.new d'abord)
                        scp \$SSH_OPTS \$PRIVATE_KEY_FILE \
                            \$REMOTE_USER@\$VM_IP:\$SECRETS_DIR/private_key.pem.new

                        scp \$SSH_OPTS \$PUBLIC_KEY_FILE \
                            \$REMOTE_USER@\$VM_IP:\$SECRETS_DIR/public_key.pem.new

                        # ── 2. Remplacement + permissions
                        ssh \$SSH_OPTS \$REMOTE_USER@\$VM_IP '
                            cd \$SECRETS_DIR
                            mv private_key.pem.new private_key.pem
                            mv public_key.pem.new public_key.pem
                            chmod 644 \$SECRETS_DIR/*.pem
                        '

                        # ── 3. Envoi du docker-compose
                        scp \$SSH_OPTS docker-compose.yml \
                            \$REMOTE_USER@\$VM_IP:\$DEPLOY_DIR/docker-compose.yml

                        # ── 4. Pull + restart + health check ──────────────────────────
                        ssh \$SSH_OPTS \$REMOTE_USER@\$VM_IP '
                            cd \$DEPLOY_DIR
                            docker pull \$DOCKER_IMAGE:\$DOCKER_TAG
                            docker compose down
                            docker compose up -d
                            sleep 10
                            docker compose ps | grep -q "Up" || exit 1
                        '
                    """
                }
            }
        }
    }

    post {
        always {
            sh 'rm -f secrets/*.pem || true'
        }
    }
}