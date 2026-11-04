pipeline {
    agent any

    environment {
        IMAGE_NAME  = 'osamaahmed123456/ivolve-nodeapp'
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
                sh 'trivy image --timeout 20m --scanners vuln --severity HIGH,CRITICAL --exit-code 0 ${IMAGE_NAME}:${IMAGE_TAG}'
            }
        }

        stage('Push Image to DockerHub') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'docker-hub',
                                                  usernameVariable: 'DH_USER',
                                                  passwordVariable: 'DH_PASS')]) {
                    sh 'echo "$DH_PASS" | docker login -u "$DH_USER" --password-stdin'
                    retry(4) {
                        sh 'docker push ${IMAGE_NAME}:${IMAGE_TAG}'
                    }
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
                sh 'sed -i "/name: myapp-container/{n;s|image:.*|image: ${IMAGE_NAME}:${IMAGE_TAG}|}" ${DEPLOY_FILE}'
                sh 'grep -n "image:" ${DEPLOY_FILE}'
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                withCredentials([string(credentialsId: 'jenkins-sa', variable: 'K8S_TOKEN'),
                                 string(credentialsId: 'jenkins-ca', variable: 'K8S_CA')]) {
                    sh '''
                        CA_FILE=$(mktemp)
                        trap 'rm -f "$CA_FILE"' EXIT
                        RAW=$(printf '%s' "$K8S_CA" | tr -d '[:space:]')
                        case "$RAW" in
                            LS0tLS1CRUdJTi*) RAW=$(printf '%s' "$RAW" | base64 -d | tr -d '[:space:]') ;;
                        esac
                        BODY=$(printf '%s' "$RAW" | sed -e 's/-----BEGINCERTIFICATE-----//' -e 's/-----ENDCERTIFICATE-----//')
                        { echo "-----BEGIN CERTIFICATE-----"; printf '%s' "$BODY" | fold -w 64; echo; echo "-----END CERTIFICATE-----"; } > "$CA_FILE"
                        TOKEN=$(printf '%s' "$K8S_TOKEN" | tr -d '[:space:]')
                        K="kubectl --server=${K8S_SERVER} --certificate-authority=${CA_FILE} --token=${TOKEN} -n ${NAMESPACE}"
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
