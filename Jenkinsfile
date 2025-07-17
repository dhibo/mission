pipeline {
    agent any

    stages {
        stage("Git stage") {
            steps {
                echo "Pulling from GitHub..."
                git branch: 'master',
                    url: 'https://github.com/dhibo/mission.git'
            }
        }

        stage("MVN CLEAN") {
            steps {
                sh 'mvn clean'
            }
        }

        stage("MVN COMPILE") {
            steps {
                sh 'mvn compile'
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

        stage("MVN SONARQUBE") {
            steps {
                withSonarQubeEnv('SonarQubeServer') {
                    sh 'mvn sonar:sonar'
                } 
            }
        }
        
        stage("Nexus Deploy") {
            steps {
                echo "OD ======> Deploying to Nexus repository..."
                withCredentials([usernamePassword(credentialsId: 'jenkins_nexus', passwordVariable: 'NEXUS_PASSWORD', usernameVariable: 'NEXUS_USER')]) {
                    sh '''
                    echo "OD ======> Using Nexus user: ${NEXUS_USER}"
                    mvn deploy -DskipTests -Dusername=${NEXUS_USER} -Dpassword=${NEXUS_PASSWORD}
                    '''
                }
            }
        }
        
        stage("Building Image") {
            steps {
                script {
                    def version = sh(script: "mvn help:evaluate -Dexpression=project.version -q -DforceStdout", returnStdout: true).trim()
                    echo "OD ======> Using VERSION: ${version}"
                    
                    sh """
                    docker build -t tpfoyer:${version} .
                    docker tag tpfoyer:${version} dhibo/tpfoyer:${version}
                    """
                }
            }
        }                
        
        stage("Deploy Image") {
            steps {
                script {
                    def version = sh(script: "mvn help:evaluate -Dexpression=project.version -q -DforceStdout", returnStdout: true).trim()
                    echo "OD ======> Using DOCKER_TAG: ${version}"
                    
                    sh """
                    export DOCKER_TAG=${version}
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
