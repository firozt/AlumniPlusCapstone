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
                 sh 'docker build -t activity-service:latest ./activity-service'
            }
        }

       stage('Push to ECR') {
            steps {
                sh '''
                    ORDER_IMAGE="$ECR_REPO:order-service-$BUILD_NUMBER"
                    ACTIVITY_IMAGE="$ECR_REPO:activity-service-$BUILD_NUMBER"

                    docker tag order-service:latest "$ORDER_IMAGE"
                    docker tag activity-service:latest "$ACTIVITY_IMAGE"

                    docker login --username AWS --password-stdin "$ECR_REGISTRY" < /ecr/token

                    docker push "$ORDER_IMAGE"
                    docker push "$ACTIVITY_IMAGE"
                '''
            }
            post {
                always {
                    sh 'docker logout "$ECR_REGISTRY" > /dev/null 2>&1 || true'
                }
            }
        }

        stage('Deploy Activity Service') {
            steps {
                sh '''
                    IMAGE="$ECR_REPO:activity-service-$BUILD_NUMBER"

                    sed "s|IMAGE_PLACEHOLDER|$IMAGE|g" \
                        k8s/activity-deployment.yaml \
                        > activity-deployment-rendered.yaml

                    kubectl apply --dry-run=server \
                        -f activity-deployment-rendered.yaml \
                        -f k8s/activity-service.yaml

                    kubectl apply \
                        -f activity-deployment-rendered.yaml \
                        -f k8s/activity-service.yaml

                    kubectl rollout status deployment/activity-service \
                        -n team-1 --timeout=360s
                '''
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
