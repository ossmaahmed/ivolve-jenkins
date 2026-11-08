def call(Map cfg = [:]) {
    ['image', 'server'].each { key ->
        if (!cfg[key]) {
            error "DeployOnK8s: parameter \"${key}\" is required"
        }
    }
    def tag           = cfg.tag ?: env.BUILD_NUMBER
    def deployFile    = cfg.deployFile ?: 'deplyment.yaml'
    def svcFile       = cfg.svcFile ?: 'svc.yaml'
    def namespace     = cfg.namespace ?: 'default'
    def containerName = cfg.containerName ?: 'myapp-container'
    def deployName    = cfg.deployName ?: 'ivolveapp'
    def tokenCredId   = cfg.tokenCredentialsId ?: 'jenkins-sa'
    def caCredId      = cfg.caCredentialsId ?: 'jenkins-ca'
    def fullName      = "${cfg.image}:${tag}"

    echo "Updating image in ${deployFile} to ${fullName}"
    withEnv(["IMG=${fullName}", "DEPLOY_FILE=${deployFile}", "CONTAINER=${containerName}"]) {
        sh '''
            sed -i "/name: ${CONTAINER}/{n;s|image:.*|image: ${IMG}|}" "${DEPLOY_FILE}"
            grep -n "image:" "${DEPLOY_FILE}"
        '''
    }

    echo "Deploying to the cluster ${cfg.server}"
    withEnv(["K8S_SERVER=${cfg.server}", "NAMESPACE=${namespace}", "DEPLOY_FILE=${deployFile}",
             "SVC_FILE=${svcFile}", "DEPLOY_NAME=${deployName}"]) {
        withCredentials([string(credentialsId: tokenCredId, variable: 'K8S_TOKEN'),
                         string(credentialsId: caCredId, variable: 'K8S_CA')]) {
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
                retry_cmd() {
                    n=0
                    until "$@"; do
                        n=$((n+1))
                        if [ "$n" -ge 6 ]; then return 1; fi
                        echo "command failed, retry $n of 5 in 15s"
                        sleep 15
                    done
                }
                retry_cmd $K apply --validate=false -f ${DEPLOY_FILE}
                retry_cmd $K apply --validate=false -f ${SVC_FILE}
                retry_cmd $K rollout status deployment/${DEPLOY_NAME} --timeout=180s
            '''
        }
    }
}