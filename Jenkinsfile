pipeline {
    agent any

    environment {
        IMAGE_NAME  = 'osamaahmed123456/ivolve_compose'
        IMAGE_TAG   = "${BUILD_NUMBER}"
        DEPLOY_FILE = 'deplyment.yaml'
        SVC_FILE    = 'svc.yaml'
        NAMESPACE   = 'ivolve'
        K8S_SERVER  = 'https://192.168.17.146:6443'
    }

    stages {
        stage('Clone') {
            steps {
                git branch: 'main', url: 'https://github.com/ossmaahmed/ivolve-jenkins'
            }
        }

        stage('Build App') {
            steps {
                sh 'npm install'
            }
        }

        stage('Build Docker Image') {
            steps {
                sh 'docker build -t ${IMAGE_NAME}:${IMAGE_TAG} .'
            }
        }

        stage('Scan Docker Image') {
            steps {
                sh 'trivy image --severity HIGH,CRITICAL --exit-code 0 ${IMAGE_NAME}:${IMAGE_TAG}'
            }
        }

        stage('Push Image to DockerHub') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub-credentials',
                                                  usernameVariable: 'DH_USER',
                                                  passwordVariable: 'DH_PASS')]) {
                    sh '''
                        echo "$DH_PASS" | docker login -u "$DH_USER" --password-stdin
                        docker push ${IMAGE_NAME}:${IMAGE_TAG}
                    '''
                }
            }
        }

        stage('Delete Image Locally') {
            steps {
                sh 'docker rmi ${IMAGE_NAME}:${IMAGE_TAG}'
            }
        }

        stage('Update Image in Deployment File') {
            steps {
                sh 'sed -i "s|image: ${IMAGE_NAME}:.*|image: ${IMAGE_NAME}:${IMAGE_TAG}|" ${DEPLOY_FILE}'
                sh 'grep -n "image:" ${DEPLOY_FILE}'
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                withCredentials([string(credentialsId: 'jenkinssa-token', variable: 'K8S_TOKEN'),
                                 file(credentialsId: 'k8s-ca', variable: 'K8S_CA')]) {
                    sh '''
                        K="kubectl --server=${K8S_SERVER} --certificate-authority=${K8S_CA} --token=${K8S_TOKEN} -n ${NAMESPACE}"
                        $K apply -f ${DEPLOY_FILE}
                        $K apply -f ${SVC_FILE}
                        $K rollout status deployment/ivolveapp --timeout=180s
                    '''
                }
            }
        }
    }

    post {
        always {
            sh 'docker logout || true'
            cleanWs()
        }
        success {
            echo "Pipeline succeeded: ${IMAGE_NAME}:${IMAGE_TAG} deployed to namespace ${NAMESPACE}"
        }
        failure {
            echo 'Pipeline failed, check the console output of the failed stage'
        }
    }
}
