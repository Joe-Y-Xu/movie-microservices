pipeline {
    agent any

    tools {
        jdk 'JDK17'
        maven 'Maven3'
    }

    environment {
        DOCKER_REGISTRY = 'docker.io'
        DOCKER_USER = 'ggjoey'
        K8S_NAMESPACE = 'default'

        CATALOG_SERVICE  = 'artifact-catalog-service'
        MOVIE_SERVICE   = 'movieinfo-service'
        RATING_SERVICE  = 'ratingdata-service'
    }

    stages {
        stage('Maven Build') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Archive JARs') {
            steps {
                archiveArtifacts artifacts: '**/target/*.jar',
                                 fingerprint: true,
                                 allowEmptyArchive: false
            }
        }

        stage('Build & Push Docker Images') {
            environment {
                PATH = "/usr/local/bin:${env.PATH}"
            }
            steps {
                script {
                    def gitShort = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                    def IMAGE_TAG = "${env.BUILD_NUMBER}-${gitShort}"
                    echo "✅ Using Image Tag: ${IMAGE_TAG}"

                    // Build images with absolute docker path
                    sh "/usr/local/bin/docker build -t ${DOCKER_USER}/${CATALOG_SERVICE}:${IMAGE_TAG} ./${CATALOG_SERVICE}"
                    sh "/usr/local/bin/docker build -t ${DOCKER_USER}/${MOVIE_SERVICE}:${IMAGE_TAG} ./${MOVIE_SERVICE}"
                    sh "/usr/local/bin/docker build -t ${DOCKER_USER}/${RATING_SERVICE}:${IMAGE_TAG} ./${RATING_SERVICE}"

                    // Docker login & push (fixed triple double quotes + escaping)
                    withCredentials([
                        usernamePassword(
                            credentialsId: 'dockerhub-credentials',
                            usernameVariable: 'DOCKER_USER_NAME',
                            passwordVariable: 'DOCKER_TOKEN'
                        )
                    ]) {
                        sh """
                            /usr/local/bin/docker login -u \${DOCKER_USER_NAME} -p \${DOCKER_TOKEN}
                            /usr/local/bin/docker push \${DOCKER_USER_NAME}/artifact-catalog-service:${IMAGE_TAG}
                            /usr/local/bin/docker push \${DOCKER_USER_NAME}/movieinfo-service:${IMAGE_TAG}
                            /usr/local/bin/docker push \${DOCKER_USER_NAME}/ratingdata-service:${IMAGE_TAG}
                            /usr/local/bin/docker logout
                        """
                    }
                }
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                script {
                    def gitShort = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                    def IMAGE_TAG = "${env.BUILD_NUMBER}-${gitShort}"

                    withKubeConfig([credentialsId: 'kubeconfig']) {
                        sh """
                            kubectl set image deployment/artifact-catalog-deploy artifact-catalog=${DOCKER_USER}/${CATALOG_SERVICE}:${IMAGE_TAG} -n ${K8S_NAMESPACE}
                            kubectl set image deployment/movieinfo-deploy movieinfo=${DOCKER_USER}/${MOVIE_SERVICE}:${IMAGE_TAG} -n ${K8S_NAMESPACE}
                            kubectl set image deployment/ratingdata-deploy ratingdata=${DOCKER_USER}/${RATING_SERVICE}:${IMAGE_TAG} -n ${K8S_NAMESPACE}

                            kubectl rollout status deployment/artifact-catalog-deploy -n ${K8S_NAMESPACE}
                            kubectl rollout status deployment/movieinfo-deploy -n ${K8S_NAMESPACE}
                            kubectl rollout status deployment/ratingdata-deploy -n ${K8S_NAMESPACE}

                            kubectl get pods -n ${K8S_NAMESPACE}
                        """
                    }
                }
            }
        }

        stage('Smoke Test') {
            steps {
                script {
                    withKubeConfig([credentialsId: 'kubeconfig']) {
                        sh '''
                            CATALOG_POD=$(kubectl get pods -n default -l app=artifact-catalog -o jsonpath='{.items[0].metadata.name}')
                            kubectl exec $CATALOG_POD -- curl -s http://localhost:8080/catalog/1
                        '''
                    }
                }
            }
        }
    }

    post {
        always {
            echo 'Pipeline finished'
            cleanWs()
        }

        success {
            echo '✅ Pipeline SUCCESS'
        }

        failure {
            echo '❌ Pipeline failed'
        }
    }
}
