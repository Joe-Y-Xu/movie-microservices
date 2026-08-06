pipeline {
    agent any

    tools {
        jdk 'JDK17'
        maven 'Maven3'
    }

    environment {
        DOCKER_REGISTRY = 'docker.io'
        DOCKER_USER = 'joe-y-xu'
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
                // Prepend docker binary path for all steps inside this stage
                PATH = "/usr/local/bin:${env.PATH}"
            }
            steps {
                script {
                    // Generate immutable unique tag at runtime
                    def gitShort = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                    def IMAGE_TAG = "${env.BUILD_NUMBER}-${gitShort}"
                    echo "✅ Using Image Tag: ${IMAGE_TAG}"

                    // Build once per service
                    def catalogImg = docker.build("${DOCKER_USER}/${CATALOG_SERVICE}:${IMAGE_TAG}", "./${CATALOG_SERVICE}")
                    def movieImg  = docker.build("${DOCKER_USER}/${MOVIE_SERVICE}:${IMAGE_TAG}", "./${MOVIE_SERVICE}")
                    def ratingImg = docker.build("${DOCKER_USER}/${RATING_SERVICE}:${IMAGE_TAG}", "./${RATING_SERVICE}")

                    // Push to Docker Hub
                    docker.withRegistry('https://docker.io', 'dockerhub-credentials') {
                        catalogImg.push()
                        movieImg.push()
                        ratingImg.push()
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
            script {
                def gitShort = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                def tagName = "deploy-build-${env.BUILD_NUMBER}-${gitShort}"

                echo "✅ Creating Git tag: $tagName"

                sh """
                    git config user.name "Jenkins CI"
                    git config user.email "jenkins@ci.local"
                    git tag -f ${tagName}
                    git push -f origin ${tagName}
                """

                echo "✅ Git Tag pushed successfully: ${tagName}"
            }
        }

        failure {
            echo '❌ Pipeline failed'
        }
    }
}
