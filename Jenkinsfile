@Library('ivolve-shared-lib') _

pipeline {
    agent { label 'docker-agent' }

    environment {
        IMAGE_NAME = 'osamaahmed123456/ivolve'
        REPO_URL   = 'https://github.com/ossmaahmed/ivolve-jenkins'
    }

    stages {
        stage('Clone') {
            steps {
                git branch: 'main', url: env.REPO_URL
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
                    repoUrl: env.REPO_URL,
                    deployFile: 'k8s/deplyment.yaml',
                    containerName: 'myapp-container',
                    credentialsId: 'github'
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
            echo "Pipeline succeeded: ${env.IMAGE_NAME}:${env.BUILD_NUMBER} pushed to Git, Argo CD will sync it"
        }
        failure {
            echo 'Pipeline failed, check the console output of the failed stage'
        }
    }
}