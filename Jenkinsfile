pipeline {
    agent any

    options { disableConcurrentBuilds() }

    triggers {
        githubPush()
    }

    stages {
        stage('Build') {
            steps {
                sh 'make build-no-tests'
            }
        }

        stage('Unit Tests') {
            steps {
                sh 'make tests'
            }
        }

    }
}
