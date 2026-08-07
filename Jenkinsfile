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
                    withKubeConfig(credentialsId: 'kubeconfig') {
                        // Debug: Verify kubectl is available
                        sh """
                            echo "==== Current PATH ===="
                            echo \$PATH
                            which kubectl || echo "!!! kubectl NOT FOUND in PATH !!!"
                            kubectl version --client
                        """
                        
                        // Use envsubst to replace variables and apply deployments
                        sh """
                            export IMAGE_TAG=${env.IMAGE_TAG}
                            export DOCKER_USER=${DOCKER_USER}
                            
                            echo "=== Deploying artifact-catalog ==="
                            envsubst < ${CATALOG_SERVICE}/deployment.yaml | kubectl apply -f -
                            
                            echo "=== Deploying movieinfo ==="
                            envsubst < ${MOVIE_SERVICE}/deployment.yaml | kubectl apply -f -
                            
                            echo "=== Deploying ratingdata ==="
                            envsubst < ${RATING_SERVICE}/deployment.yaml | kubectl apply -f -
                        """
                        
                        // Wait for rollouts to complete
                        sh """
                            echo "=== Waiting for deployments to complete ==="
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
                    withKubeConfig(credentialsId: 'kubeconfig') {
                        sh """
                            set -e
                            MAX_RETRY=3
                            SLEEP_SEC=3
                            
                            echo "=== Running smoke tests ==="
                            
                            echo "Testing catalog service..."
                            ATTEMPT=1
                            until [
                                CATALOG_POD=\$(kubectl get pods -n ${K8S_NAMESPACE} -l app=artifact-catalog-service --field-selector status.phase=Running -o jsonpath='{.items[?(@.status.containerStatuses[0].ready==true)].metadata.name}' | awk '{print \$1}')
                                if [ -z "\$CATALOG_POD" ]; then
                                    echo "⚠️ No ready catalog pod found!"
                                    exit 1
                                fi
                                echo "Attempt \$ATTEMPT using pod: \$CATALOG_POD"
                                kubectl exec "\$CATALOG_POD" -n ${K8S_NAMESPACE} -- curl -s --fail http://localhost:8080/catalog/1
                            ]; do
                                if [ \$ATTEMPT -ge \$MAX_RETRY ]; then
                                    echo "❌ Catalog service test failed after \$MAX_RETRY attempts!"
                                    exit 1
                                fi
                                echo "⚠️ Catalog test attempt \$ATTEMPT failed, retry after \$SLEEP_SEC seconds..."
                                ATTEMPT=\$((ATTEMPT+1))
                                sleep \$SLEEP_SEC
                            done
                            
                            echo "Testing movie service..."
                            ATTEMPT=1
                            until [
                                MOVIE_POD=\$(kubectl get pods -n ${K8S_NAMESPACE} -l app=movieinfo-service --field-selector status.phase=Running -o jsonpath='{.items[?(@.status.containerStatuses[0].ready==true)].metadata.name}' | awk '{print \$1}')
                                if [ -z "\$MOVIE_POD" ]; then
                                    echo "⚠️ No ready movie pod found!"
                                    exit 1
                                fi
                                echo "Attempt \$ATTEMPT using pod: \$MOVIE_POD"
                                kubectl exec "\$MOVIE_POD" -n ${K8S_NAMESPACE} -- curl -s --fail http://localhost:8081/movies/1
                            ]; do
                                if [ \$ATTEMPT -ge \$MAX_RETRY ]; then
                                    echo "❌ Movie service test failed after \$MAX_RETRY attempts!"
                                    exit 1
                                fi
                                echo "⚠️ Movie test attempt \$ATTEMPT failed, retry after \$SLEEP_SEC seconds..."
                                ATTEMPT=\$((ATTEMPT+1))
                                sleep \$SLEEP_SEC
                            done
                            
                            echo "Testing rating service..."
                            ATTEMPT=1
                            until [
                                RATING_POD=\$(kubectl get pods -n ${K8S_NAMESPACE} -l app=ratingdata-service --field-selector status.phase=Running -o jsonpath='{.items[?(@.status.containerStatuses[0].ready==true)].metadata.name}' | awk '{print \$1}')
                                if [ -z "\$RATING_POD" ]; then
                                    echo "⚠️ No ready rating pod found!"
                                    exit 1
                                fi
                                echo "Attempt \$ATTEMPT using pod: \$RATING_POD"
                                kubectl exec "\$RATING_POD" -n ${K8S_NAMESPACE} -- curl -s --fail http://localhost:8082/movies/1
                            ]; do
                                if [ \$ATTEMPT -ge \$MAX_RETRY ]; then
                                    echo "❌ Rating service test failed after \$MAX_RETRY attempts!"
                                    exit 1
                                fi
                                echo "⚠️ Rating test attempt \$ATTEMPT failed, retry after \$SLEEP_SEC seconds..."
                                ATTEMPT=\$((ATTEMPT+1))
                                sleep \$SLEEP_SEC
                            done
                            
                            echo "✅ All smoke tests passed!"
                        """
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
                    echo '❌ Pipeline FAILED'
                }
            }
        }
    }   // <-- Close the 'stages' block
}       // <-- Close the 'pipeline' block (THIS WAS MISSING!)
