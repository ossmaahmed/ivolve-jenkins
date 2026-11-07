@Library('ivolve-shared-lib') _

pipeline {
    agent { label 'docker-agent' }

    environment {
        IMAGE_NAME = 'osamaahmed123456/ivolve'
        K8S_SERVER = 'https://192.168.17.146:6443'
        NAMESPACE  = 'ivolve'
    }

    stages {
        stage('Clone') {
            steps {
                git branch: 'main', url: 'https://github.com/ossmaahmed/ivolve-jenkins'
            }
        }

        stage('Build App') {
            steps {
                BuildApp()
            }
        }

        stage('Build Image') {
            steps {
                BuildImage(image: env.IMAGE_NAME, tag: env.BUILD_NUMBER, credentialsId: 'docker-hub')
            }
        }

        stage('Deploy On K8s') {
            steps {
                DeployOnK8s(
                    image: env.IMAGE_NAME,
                    tag: env.BUILD_NUMBER,
                    server: env.K8S_SERVER,
                    namespace: env.NAMESPACE,
                    deployFile: 'deplyment.yaml',
                    svcFile: 'svc.yaml',
                    containerName: 'myapp-container',
                    deployName: 'ivolveapp',
                    tokenCredentialsId: 'jenkins-sa',
                    caCredentialsId: 'jenkins-ca'
                )
            }
        }
    }

    post {
        always {
            sh 'docker logout || true'
            cleanWs()
        }
        success {
            echo "Pipeline succeeded: ${env.IMAGE_NAME}:${env.BUILD_NUMBER} deployed to ${env.NAMESPACE}"
        }
        failure {
            echo 'Pipeline failed, check the console output of the failed stage'
        }
    }
}
