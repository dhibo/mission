pipeline {
    agent any

    environment {
        VERSION = ''
        TAG_VERSION = ''
    }

    stages {
        stage("Checkout & Tagging") {
            steps {
                echo "Checking out code and creating tag..."
                script {
                    git branch: 'master',
                        url: 'https://github.com/dhibo/mission.git'
                    
                    def baseVersion = "1.0.1-SNAPSHOT"
                    def fullVersion = "${baseVersion}-${env.BUILD_NUMBER}"
                    
                    echo "Base version: ${baseVersion}"
                    echo "Full version: ${fullVersion}"
                    echo "Build number: ${env.BUILD_NUMBER}"
                    
                    sh "mvn versions:set -DnewVersion=${fullVersion} -DgenerateBackupPoms=false"
                    
                    sh "git add pom.xml"
                    sh "git commit -m 'Update version to ${fullVersion}' || true"
                    sh "git tag -a ${fullVersion} -m 'Release ${fullVersion}'"
                    
                    echo "Tag ${fullVersion} created successfully"
                }
            }
        }

        stage("Clean & Compile") {
            steps {
                echo "Cleaning and compiling the project..."
                sh 'mvn clean compile'
            }
        }

        stage("Unit Tests") {
            steps {
                echo "Running Unit Tests..."
                sh 'mvn test -Dtest="*ServiceTest,*RestControllerTest" -DfailIfNoTests=false'
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }

        stage("Integration Tests") {
            steps {
                echo "Running Integration Tests..."
                sh 'mvn test -Dtest="*IntegrationTest" -DfailIfNoTests=false'
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }

        stage("SonarQube") {
            steps {
                echo "Running SonarQube analysis..."
                withSonarQubeEnv('SonarQubeServer') {
                    sh 'mvn verify'
                    sh 'mvn sonar:sonar -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml'
                }
                waitForQualityGate abortPipeline: true
            }
        }

        stage("Nexus Deploy") {
            steps {
                echo "Deploying to Nexus repository..."
                withCredentials([usernamePassword(credentialsId: 'nexus_jenkins', passwordVariable: 'NEXUS_PASSWORD', usernameVariable: 'NEXUS_USER')]) {
                    sh '''
                    echo "OD ======> Configuring Maven settings for Nexus..."
                    mvn deploy -DskipTests \
                        -Dnexus.username=${NEXUS_USER} \
                        -Dnexus.password=${NEXUS_PASSWORD} \
                        -s settings.xml
                    '''
                }
            }
        }

        stage("Building Image") {
            steps {
                echo "Building Docker image..."
                script {
                    def version = sh(script: "mvn help:evaluate -Dexpression=project.version -q -DforceStdout", returnStdout: true).trim()
                    def tagVersion = version
                    
                    echo "OD ======> Using VERSION: ${version}"
                    echo "OD ======> Using TAG_VERSION: ${tagVersion}"
                    
                    sh """
                    docker build --build-arg VERSION=${version} -t tpfoyer:${tagVersion} .
                    docker tag tpfoyer:${tagVersion} dhibo/tpfoyer:${tagVersion}
                    """
                }
            }
        }

        stage("Deploy Image") {
            steps {
                echo "Deploying Docker image..."
                script {
                    def tagVersion = sh(script: "mvn help:evaluate -Dexpression=project.version -q -DforceStdout", returnStdout: true).trim()
                    
                    echo "OD ======> Using DOCKER_TAG: ${tagVersion}"
                    sh """
                    export DOCKER_TAG=${tagVersion}
                    docker compose -f Docker-compose.yml up -d
                    docker ps -a
                    """
                }
            }
        }
    }

    post {
        always {
            echo "OD ======> Pipeline completed with result: ${currentBuild.result}"
        }
        success {
            echo "OD ======> Pipeline succeeded! Application deployed successfully"
        }
        failure {
            echo "OD ======> Pipeline failed! Check the logs for details."
        }
    }
} 
