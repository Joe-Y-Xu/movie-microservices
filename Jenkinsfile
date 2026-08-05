pipeline {
    agent any
    tools {
        jdk 'JDK17'
        maven 'Maven-3.9'
    }
    stages {
        stage('Checkout Source') {
            steps {
                checkout scm
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
    }
    post {
        success { echo '✅ Build completed, all jars archived' }
        failure { echo '❌ Build failed, check console output' }
    }
}
