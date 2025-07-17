pipeline {
    agent any

    environment {
        VERSION = ''
        TAG_VERSION = ''
    }

    stages {
        stage("Checkout") {
            steps {
                echo "Checking out code and creating tag..."
                script {
                    git branch: 'master',
                        url: 'https://github.com/dhibo/mission.git'
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
            }
        }

        stage("Nexus Deploy") {
            steps {
                echo "Deploying to Nexus repository..."
                withCredentials([usernamePassword(credentialsId: 'jenkins_nexus', passwordVariable: 'NEXUS_PASSWORD', usernameVariable: 'NEXUS_USER')]) {
                    sh '''
                        # Create Maven settings.xml with Nexus credentials
                        mkdir -p ~/.m2
                        cat > ~/.m2/settings.xml << SETTINGS_EOF
            <?xml version="1.0" encoding="UTF-8"?>
            <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0">
                <servers>
                    <server>
                        <id>jenkins_nexus</id>
                        <username>${NEXUS_USER}</username>
                        <password>${NEXUS_PASSWORD}</password>
                    </server>
                </servers>
            </settings>
            SETTINGS_EOF
                        mvn deploy -DskipTests
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
