pipeline {
    agent any

    options {
        skipDefaultCheckout(true)
        disableConcurrentBuilds()
        timeout(time: 30, unit: 'MINUTES')
    }

    stages {
        stage('Checkout') {
            steps {
                // gets latest update from reppo
                checkout scm
            }
        }

        stage('Build and test') {
            agent {
                docker {
                    image 'maven:3.9-eclipse-temurin-17'
                    reuseNode true
                }
            }
            steps {
                dir('order-service') {
                    sh 'mvn -B -ntp -Dspring.profiles.active=ci clean verify'
                }
            }
            post {
                always {
                    junit testResults: 'order-service/target/surefire-reports/TEST-*.xml'
                }
            }
        }

        stage('Build Docker images') {
            steps {
                sh 'docker compose build'
            }
        }
        // TODO: add other .envs here
        stage('Run containers') {
            steps {
                withCredentials([
                    file(credentialsId: 'orders-env', variable: 'ORDERS_ENV')
                ]) {
                    sh '''
                        cp "$ORDERS_ENV" order-service/.env
                        trap 'rm -f order-service/.env' EXIT
                        docker compose up -d
                    '''
                }
            }
        }
    }
}
