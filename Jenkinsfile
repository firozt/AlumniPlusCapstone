
pipeline {
    agent any

    parameters {
        booleanParam(
            name: 'ACTIVITY_ONLY',
            defaultValue: false,
            description: 'Run only Activity Service stages'
        )

        booleanParam(
            name: 'DEPLOY_ACTIVITY_SERVICE',
            defaultValue: false,
            description: 'Deploy Activity Service to Kubernetes'
        )
    }

    options {
        skipDefaultCheckout(true)
        disableConcurrentBuilds()
        timeout(time: 30, unit: 'MINUTES')
    }

    stages {
        stage('Checkout') {
            steps {
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
                script {
                    if (!params.ACTIVITY_ONLY) {
                        dir('order-service') {
                            sh 'mvn -B -ntp -Dspring.profiles.active=ci clean verify'
                        }
                    }
                }

                dir('activity-service') {
                    sh 'mvn -B -ntp -Dspring.profiles.active=ci clean verify'
                }
            }

            post {
                always {
                    junit testResults: '**/target/surefire-reports/TEST-*.xml'
                }
            }
        }

        stage('Build Docker images') {
            steps {
                script {
                    if (!params.ACTIVITY_ONLY) {
                        sh 'docker build -t order-service:latest ./order-service'
                        sh 'docker build -t react-frontend:latest ./client'
                    }
                }

                sh 'docker build -t activity-service:latest ./activity-service'
            }
        }

        stage('Push to ECR') {
            steps {
                sh '''
                    docker login --username AWS --password-stdin "$ECR_REGISTRY" < /ecr/token
                '''

                script {
                    if (!params.ACTIVITY_ONLY) {
                        sh '''
                            ORDER_IMAGE="$ECR_REPO:order-service-$BUILD_NUMBER"

                            docker tag order-service:latest "$ORDER_IMAGE"
                            docker push "$ORDER_IMAGE"

                            FRONTEND_IMAGE="$ECR_REPO:react-frontend-$BUILD_NUMBER"

                            docker tag react-frontend:latest "$FRONTEND_IMAGE"
                            docker push "$FRONTEND_IMAGE"
                        '''
                    }
                }

                sh '''
                    ACTIVITY_IMAGE="$ECR_REPO:activity-service-$BUILD_TAG"

                    docker tag activity-service:latest "$ACTIVITY_IMAGE"
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
            when {
                expression {
                    params.DEPLOY_ACTIVITY_SERVICE
                }
            }

            steps {
                sh '''
                    IMAGE="$ECR_REPO:activity-service-$BUILD_TAG"

                    sed "s|IMAGE_PLACEHOLDER|$IMAGE|g" \\
                        k8s/activity-deployment.yaml \\
                        > activity-deployment-rendered.yaml

                    kubectl apply --dry-run=server \\
                        -f activity-deployment-rendered.yaml \\
                        -f k8s/activity-service.yaml

                    kubectl apply \\
                        -f activity-deployment-rendered.yaml \\
                        -f k8s/activity-service.yaml

                    kubectl rollout status deployment/activity-service \\
                        -n team-1 --timeout=360s
                '''
            }
        }

        stage('Run Order Service') {
            when {
                expression {
                    !params.ACTIVITY_ONLY
                }
            }

            steps {
                withCredentials([
                    file(credentialsId: 'orders-env', variable: 'ORDERS_ENV')
                ]) {
                    sh '''
                        docker network inspect capstone > /dev/null 2>&1 || docker network create capstone

                        existing=$(docker ps -aq --filter 'name=^/order-service$')
                        if [ -n "$existing" ]; then
                            docker rm -f "$existing"
                        fi
                        docker run -d --name order-service \\
                            --restart unless-stopped \\
                            --network capstone \\
                            --env-file "$ORDERS_ENV" \\
                            -p 8080:8080 \\
                            order-service:latest
                    '''
                }
            }
        }

        stage('Run Frontend') {
            when {
                expression {
                    !params.ACTIVITY_ONLY
                }
            }

            steps {
                sh '''
                    existing=$(docker ps -aq --filter 'name=^/react-frontend$')
                    if [ -n "$existing" ]; then
                        docker rm -f "$existing"
                    fi
                    docker run -d --name react-frontend \\
                        --restart unless-stopped \\
                        --network capstone \\
                        -p 5173:8080 \\
                        react-frontend:latest
                '''
            }
        }
    }
}
