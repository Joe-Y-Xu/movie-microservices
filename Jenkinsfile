pipeline {
    agent any

    tools {
        jdk 'JDK17'
        maven 'Maven3'
    }

    environment {
        DOCKER_REGISTRY = 'docker.io'
        DOCKER_USER     = 'ggjoey'
        K8S_NAMESPACE   = 'default'

        CATALOG_SERVICE = 'artifact-catalog-service'
        MOVIE_SERVICE   = 'movieinfo-service'
        RATING_SERVICE  = 'ratingdata-service'

        IMAGE_TAG = '' // 占位，后续动态赋值
    }

    stages {
        stage('Init Generate Tag') {
            steps {
                script {
                    def GIT_SHORT = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                    env.IMAGE_TAG = "${env.BUILD_NUMBER}-${GIT_SHORT}"
                    echo "✅ Target Image Tag: ${IMAGE_TAG}"
                }
            }
        }

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
                    // Build images
                    sh "/usr/local/bin/docker build -t ${DOCKER_USER}/${CATALOG_SERVICE}:${IMAGE_TAG} ./${CATALOG_SERVICE}"
                    sh "/usr/local/bin/docker build -t ${DOCKER_USER}/${MOVIE_SERVICE}:${IMAGE_TAG} ./${MOVIE_SERVICE}"
                    sh "/usr/local/bin/docker build -t ${DOCKER_USER}/${RATING_SERVICE}:${IMAGE_TAG} ./${RATING_SERVICE}"

                    // Login & push
                    withCredentials([
                        usernamePassword(
                            credentialsId: 'dockerhub-credentials',
                            usernameVariable: 'DOCKER_USER_NAME',
                            passwordVariable: 'DOCKER_TOKEN'
                        )
                    ]) {
                        sh """
                            set -e
                            /usr/local/bin/docker login -u '${DOCKER_USER_NAME}' -p '${DOCKER_TOKEN}'
                            /usr/local/bin/docker push ${DOCKER_USER}/${CATALOG_SERVICE}:${IMAGE_TAG}
                            /usr/local/bin/docker push ${DOCKER_USER}/${MOVIE_SERVICE}:${IMAGE_TAG}
                            /usr/local/bin/docker push ${DOCKER_USER}/${RATING_SERVICE}:${IMAGE_TAG}
                        """
                    }
                }
            }
            post {
                always {
                    sh "/usr/local/bin/docker logout || true"
                }
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                script {
                    withKubeConfig(credentialsId: 'kubeconfig') {
                        sh """
                            set -e
                            kubectl set image deployment/artifact-catalog-deploy artifact-catalog=${DOCKER_USER}/${CATALOG_SERVICE}:${IMAGE_TAG} -n ${K8S_NAMESPACE}
                            kubectl set image deployment/movieinfo-deploy movieinfo=${DOCKER_USER}/${MOVIE_SERVICE}:${IMAGE_TAG} -n ${K8S_NAMESPACE}
                            kubectl set image deployment/ratingdata-deploy ratingdata=${DOCKER_USER}/${RATING_SERVICE}:${IMAGE_TAG} -n ${K8S_NAMESPACE}

                            kubectl rollout status deployment/artifact-catalog-deploy -n ${K8S_NAMESPACE} --timeout=300s
                            kubectl rollout status deployment/movieinfo-deploy -n ${K8S_NAMESPACE} --timeout=300s
                            kubectl rollout status deployment/ratingdata-deploy -n ${K8S_NAMESPACE} --timeout=300s

                            kubectl get pods -n ${K8S_NAMESPACE}
                        """
                    }
                }
            }
        }

        stage('Smoke Test') {
            steps {
                script {
                    withKubeConfig(credentialsId: 'kubeconfig') {
                        sh """
                            set -e
                            CATALOG_POD=\$(kubectl get pods -n ${K8S_NAMESPACE} -l app=artifact-catalog -o jsonpath='{.items[0].metadata.name}')
                            echo "Testing catalog pod: \$CATALOG_POD"
                            kubectl exec "\$CATALOG_POD" -- curl -s --fail http://localhost:8080/catalog/1
                        """
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
