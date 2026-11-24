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
                sh 'docker build -t order-service:latest ./order-service'
                // sh 'docker build -t activity-service:latest ./activity-service'
            }
        }

        stage('Push to ECR') {
            steps {
                sh '''
                    IMAGE="$ECR_REPO:order-service-$BUILD_NUMBER"

                    docker tag order-service:latest "$IMAGE"
                    docker login --username AWS --password-stdin "$ECR_REGISTRY" < /ecr/token
                    docker push "$IMAGE"
                '''
            }
            post {
                always {
                    sh 'docker logout "$ECR_REGISTRY" > /dev/null 2>&1 || true'
                }
            }
        }

        stage('Run Order Service') {
            steps {
                withCredentials([
                    file(credentialsId: 'orders-env', variable: 'ORDERS_ENV')
                ]) {
                    sh '''
                        existing=$(docker ps -aq --filter 'name=^/order-service$')
                        if [ -n "$existing" ]; then
                            docker rm -f "$existing"
                        fi
                        docker run -d --name order-service \\
                            --restart unless-stopped \\
                            --env-file "$ORDERS_ENV" \\
                            -p 8080:8080 \\
                            order-service:latest
                    '''
                }
            }
        }
    }
}
