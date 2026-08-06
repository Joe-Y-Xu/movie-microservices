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
        
        // Service names
        CATALOG_SERVICE = 'artifact-catalog-service'
        MOVIE_SERVICE   = 'movieinfo-service'
        RATING_SERVICE  = 'ratingdata-service'
        
        // Ensure kubectl is in PATH
        PATH = "/opt/homebrew/bin:/usr/local/bin:${env.PATH}"
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
            steps {
                script {
                    // Build Docker images
                    sh "docker build -t ${DOCKER_USER}/${CATALOG_SERVICE}:${env.IMAGE_TAG} ./${CATALOG_SERVICE}"
                    sh "docker build -t ${DOCKER_USER}/${MOVIE_SERVICE}:${env.IMAGE_TAG} ./${MOVIE_SERVICE}"
                    sh "docker build -t ${DOCKER_USER}/${RATING_SERVICE}:${env.IMAGE_TAG} ./${RATING_SERVICE}"

                    // Push Docker images with credentials
                    withCredentials([
                        usernamePassword(
                            credentialsId: 'dockerhub-credentials',
                            usernameVariable: 'DOCKER_USER_NAME',
                            passwordVariable: 'DOCKER_TOKEN'
                        )
                    ]) {
                        sh """
                            set -e
                            docker login -u '${DOCKER_USER_NAME}' -p '${DOCKER_TOKEN}'
                            docker push ${DOCKER_USER}/${CATALOG_SERVICE}:${env.IMAGE_TAG}
                            docker push ${DOCKER_USER}/${MOVIE_SERVICE}:${env.IMAGE_TAG}
                            docker push ${DOCKER_USER}/${RATING_SERVICE}:${env.IMAGE_TAG}
                        """
                    }
                }
            }
            post {
                always {
                    sh "docker logout || true"
                }
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                script {
                    // Debug: Verify kubectl is available
                    sh """
                        echo "==== Current PATH ===="
                        echo \$PATH
                        which kubectl || echo "!!! kubectl NOT FOUND in PATH !!!"
                        kubectl version --client
                    """
                    
                    // ✅ CORRECTED: Using 'kubeconfig' as the credential ID
                    withKubeConfig(credentialsId: 'kubeconfig') {
                        sh """
                            set -e
                            echo "=== Updating images in Kubernetes ==="
                            echo "Updating artifact-catalog-deploy with ${DOCKER_USER}/${CATALOG_SERVICE}:${env.IMAGE_TAG}"
                            kubectl set image deployment/artifact-catalog-deploy artifact-catalog=${DOCKER_USER}/${CATALOG_SERVICE}:${env.IMAGE_TAG} -n ${K8S_NAMESPACE}
                            
                            echo "Updating movieinfo-deploy with ${DOCKER_USER}/${MOVIE_SERVICE}:${env.IMAGE_TAG}"
                            kubectl set image deployment/movieinfo-deploy movieinfo=${DOCKER_USER}/${MOVIE_SERVICE}:${env.IMAGE_TAG} -n ${K8S_NAMESPACE}
                            
                            echo "Updating ratingdata-deploy with ${DOCKER_USER}/${RATING_SERVICE}:${env.IMAGE_TAG}"
                            kubectl set image deployment/ratingdata-deploy ratingdata=${DOCKER_USER}/${RATING_SERVICE}:${env.IMAGE_TAG} -n ${K8S_NAMESPACE}

                            echo "=== Waiting for rollout to complete ==="
                            kubectl rollout status deployment/artifact-catalog-deploy -n ${K8S_NAMESPACE} --timeout=300s
                            kubectl rollout status deployment/movieinfo-deploy -n ${K8S_NAMESPACE} --timeout=300s
                            kubectl rollout status deployment/ratingdata-deploy -n ${K8S_NAMESPACE} --timeout=300s

                            echo "=== Current pods in ${K8S_NAMESPACE} namespace ==="
                            kubectl get pods -n ${K8S_NAMESPACE}
                        """
                    }
                }
            }
        }

        stage('Smoke Test') {
            steps {
                script {
                    // ✅ CORRECTED: Using 'kubeconfig' as the credential ID
                    withKubeConfig(credentialsId: 'kubeconfig') {
                        sh """
                            set -e
                            echo "=== Running smoke tests ==="
                            
                            echo "Testing catalog service..."
                            CATALOG_POD=\$(kubectl get pods -n ${K8S_NAMESPACE} -l app=artifact-catalog -o jsonpath='{.items[0].metadata.name}')
                            if [ -z "\$CATALOG_POD" ]; then
                                echo "❌ No catalog pod found!"
                                exit 1
                            fi
                            echo "Found catalog pod: \$CATALOG_POD"
                            kubectl exec "\$CATALOG_POD" -n ${K8S_NAMESPACE} -- curl -s --fail http://localhost:8080/catalog/1 || {
                                echo "❌ Catalog service test failed!"
                                exit 1
                            }
                            
                            echo "Testing movie service..."
                            MOVIE_POD=\$(kubectl get pods -n ${K8S_NAMESPACE} -l app=movieinfo -o jsonpath='{.items[0].metadata.name}')
                            if [ -z "\$MOVIE_POD" ]; then
                                echo "❌ No movie pod found!"
                                exit 1
                            fi
                            echo "Found movie pod: \$MOVIE_POD"
                            kubectl exec "\$MOVIE_POD" -n ${K8S_NAMESPACE} -- curl -s --fail http://localhost:8080/movies || {
                                echo "❌ Movie service test failed!"
                                exit 1
                            }
                            
                            echo "Testing rating service..."
                            RATING_POD=\$(kubectl get pods -n ${K8S_NAMESPACE} -l app=ratingdata -o jsonpath='{.items[0].metadata.name}')
                            if [ -z "\$RATING_POD" ]; then
                                echo "❌ No rating pod found!"
                                exit 1
                            fi
                            echo "Found rating pod: \$RATING_POD"
                            kubectl exec "\$RATING_POD" -n ${K8S_NAMESPACE} -- curl -s --fail http://localhost:8080/ratings || {
                                echo "❌ Rating service test failed!"
                                exit 1
                            }
                            
                            echo "✅ All smoke tests passed!"
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
