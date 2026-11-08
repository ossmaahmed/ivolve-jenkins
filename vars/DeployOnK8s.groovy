def call(Map cfg = [:]) {
    ['image', 'repoUrl'].each { key ->
        if (!cfg[key]) {
            error "DeployOnK8s: parameter \"${key}\" is required"
        }
    }
    def tag           = cfg.tag ?: env.BUILD_NUMBER
    def deployFile    = cfg.deployFile ?: 'k8s/deplyment.yaml'
    def containerName = cfg.containerName ?: 'myapp-container'
    def branch        = cfg.branch ?: 'main'
    def credsId       = cfg.credentialsId ?: 'github'
    def gitName       = cfg.gitName ?: 'jenkins'
    def gitEmail      = cfg.gitEmail ?: 'jenkins@ivolve.local'
    def fullName      = "${cfg.image}:${tag}"

    echo "Updating image in ${deployFile} to ${fullName}"
    withEnv(["IMG=${fullName}", "DEPLOY_FILE=${deployFile}", "CONTAINER=${containerName}"]) {
        sh '''
            sed -i "/name: ${CONTAINER}/{n;s|image:.*|image: ${IMG}|}" "${DEPLOY_FILE}"
            grep -n "image:" "${DEPLOY_FILE}"
        '''
    }

    echo "Pushing ${deployFile} to GitHub"
    withEnv(["REPO_URL=${cfg.repoUrl}", "BRANCH=${branch}", "GIT_NAME=${gitName}",
             "GIT_EMAIL=${gitEmail}", "DEPLOY_FILE=${deployFile}", "IMG=${fullName}"]) {
        withCredentials([usernamePassword(credentialsId: credsId,
                                          usernameVariable: 'GH_USER',
                                          passwordVariable: 'GH_TOKEN')]) {
            sh '''
                git config user.name "${GIT_NAME}"
                git config user.email "${GIT_EMAIL}"
                git add "${DEPLOY_FILE}"
                if git diff --cached --quiet; then
                    echo "No change in ${DEPLOY_FILE}, nothing to push"
                    exit 0
                fi
                git commit -m "Update image to ${IMG}"
                AUTH_URL="https://${GH_USER}:${GH_TOKEN}@${REPO_URL#https://}"
                git pull --rebase "${AUTH_URL}" "${BRANCH}"
                git push "${AUTH_URL}" HEAD:"${BRANCH}"
            '''
        }
    }
}