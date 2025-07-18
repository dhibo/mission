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

        stage("Version Increment") {
            steps {
                echo "OD ======> Incrementing version to avoid artifact conflicts..."
                script {
                    // Make script executable and run it
                    sh 'chmod +x increment-version.sh'
                    sh './increment-version.sh'
                    
                    // Get the new version for later use
                    def newVersion = sh(script: "mvn help:evaluate -Dexpression=project.version -q -DforceStdout", returnStdout: true).trim()
                    env.PROJECT_VERSION = newVersion
                    echo "OD ======> Updated to version: ${newVersion}"
                }
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

        stage("All Tests") {
            parallel {
                stage("Unit Tests") {
                    steps {
                        echo "OD ======> Running Unit Tests..."
                        sh 'mvn test -Dtest="*ServiceTest,*RestControllerTest" -DfailIfNoTests=false'
                    }
                    post {
                        always {
                            junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
                            publishTestResults testResultsPattern: '**/target/surefire-reports/*.xml'
                        }
                    }
                }
                
                stage("Integration Tests") {
                    steps {
                        echo "OD ======> Running Integration Tests..."
                        sh 'mvn test -Dtest="*IntegrationTest*" -DfailIfNoTests=false'
                    }
                    post {
                        always {
                            junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
                            publishTestResults testResultsPattern: '**/target/surefire-reports/*.xml'
                        }
                    }
                }
            }
        }

        stage("All Tests Verification") {
            steps {
                echo "OD ======> Running comprehensive test suite..."
                sh 'mvn test -DfailIfNoTests=false'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
                    publishTestResults testResultsPattern: '**/target/surefire-reports/*.xml'
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
                echo "OD ======> Deploying to Nexus repository with version ${env.PROJECT_VERSION}..."
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
                    def version = env.PROJECT_VERSION ?: sh(script: "mvn help:evaluate -Dexpression=project.version -q -DforceStdout", returnStdout: true).trim()
                    echo "OD ======> Building Docker image with VERSION: ${version}"
                    
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
                    def version = env.PROJECT_VERSION ?: sh(script: "mvn help:evaluate -Dexpression=project.version -q -DforceStdout", returnStdout: true).trim()
                    echo "OD ======> Deploying with DOCKER_TAG: ${version}"
                    
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
            // Archive test results and coverage reports
            archiveArtifacts artifacts: '**/target/surefire-reports/*.xml', allowEmptyArchive: true
            archiveArtifacts artifacts: '**/target/site/jacoco/*.xml', allowEmptyArchive: true
        }
        success {
            echo "OD ======> Pipeline succeeded! Application deployed successfully with version ${env.PROJECT_VERSION}"
        }
        failure {
            echo "OD ======> Pipeline failed! Check the logs for details."
        }
    }
}
