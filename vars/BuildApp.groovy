def call(Map cfg = [:]) {
    def installCmd = cfg.installCmd ?: 'npm install'
    def testCmd    = cfg.testCmd    ?: 'npm test --if-present'
    def buildCmd   = cfg.buildCmd   ?: 'npm run build --if-present'

    echo 'Installing dependencies'
    sh installCmd

    echo 'Running unit tests'
    sh testCmd

    echo 'Building the app'
    sh buildCmd
}