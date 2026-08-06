pipeline {
    agent any

    tools {
        jdk 'JDK17'
        maven 'Maven3'
        kubernetescli 'kubectl-latest'
    }

    environment {
        DOCKER_REGISTRY = 'docker.io'
        DOCKER_USER     = 'ggjoey'
        K8S_NAMESPACE   = 'default'

        CATALOG_SERVICE = 'artifact-catalog-service'
        MOVIE_SERVICE   = 'movieinfo-service'
        RATING_SERVICE  = 'ratingdata-service'
    }

    stages {
        stage('Init Generate Tag') {
            steps {
                script {
                    def GIT_SHORT = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                    env.IMAGE_TAG = "${env.BUILD_NUMBER}-${GIT_SHORT}"
                    echo "✅ Target Image Tag: ${env.IMAGE_TAG}"
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
                    sh "/usr/local/bin/docker build -t ${DOCKER_USER}/${CATALOG_SERVICE}:${env.IMAGE_TAG} ./${CATALOG_SERVICE}"
                    sh "/usr/local/bin/docker build -t ${DOCKER_USER}/${MOVIE_SERVICE}:${env.IMAGE_TAG} ./${MOVIE_SERVICE}"
                    sh "/usr/local/bin/docker build -t ${DOCKER_USER}/${RATING_SERVICE}:${env.IMAGE_TAG} ./${RATING_SERVICE}"

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
                            /usr/local/bin/docker push ${DOCKER_USER}/${CATALOG_SERVICE}:${env.IMAGE_TAG}
                            /usr/local/bin/docker push ${DOCKER_USER}/${MOVIE_SERVICE}:${env.IMAGE_TAG}
                            /usr/local/bin/docker push ${DOCKER_USER}/${RATING_SERVICE}:${env.IMAGE_TAG}
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
                    // 调试：打印PATH确认kubectl是否加载成功，运行正常后可删除这段
                    sh """
                        echo "==== Current PATH ===="
                        echo \$PATH
                        which kubectl || echo "!!! kubectl NOT FOUND in PATH !!!"
                    """
                    withKubeConfig(credentialsId: 'kubeconfig') {
                        sh """
                            set -e
                            kubectl set image deployment/artifact-catalog-deploy artifact-catalog=${DOCKER_USER}/${CATALOG_SERVICE}:${env.IMAGE_TAG} -n ${K8S_NAMESPACE}
                            kubectl set image deployment/movieinfo-deploy movieinfo=${DOCKER_USER}/${MOVIE_SERVICE}:${env.IMAGE_TAG} -n ${K8S_NAMESPACE}
                            kubectl set image deployment/ratingdata-deploy ratingdata=${DOCKER_USER}/${RATING_SERVICE}:${env.IMAGE_TAG} -n ${K8S_NAMESPACE}

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
