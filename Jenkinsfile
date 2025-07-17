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
                        url: 'https://github.com/arijhakouna/tpFoyer.git'
                    
                    def baseVersion = "1.0.1-SNAPSHOT"
                    def fullVersion = "${baseVersion}-${env.BUILD_NUMBER}"
                    
                    echo "Base version: ${baseVersion}"
                    echo "Full version: ${fullVersion}"
                    echo "Build number: ${env.BUILD_NUMBER}"
                    
                    sh "mvn versions:set -DnewVersion=${fullVersion} -DgenerateBackupPoms=false"
                    
                    sh "git add pom.xml"
                    sh "git commit -m 'Update version to ${fullVersion}' || true"
                    sh "git tag -a ${fullVersion} -m 'Release ${fullVersion}'"
                    sh "git push git@github.com:arijhakouna/tpFoyer.git ${fullVersion}"
                    
                    echo "Tag ${fullVersion} created and pushed successfully"
                    
                
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

        stage("Nexus") {
            steps {
                echo "Deploying to Nexus repository..."
                sh 'mvn deploy -DskipTests'
            }
        }

        stage("Docker") {
            steps {
                echo "Building and pushing Docker image..."
                script {
                    def version = sh(script: "mvn help:evaluate -Dexpression=project.version -q -DforceStdout", returnStdout: true).trim()
                    def tagVersion = version
                    
                    echo "Using VERSION: ${version}"
                    echo "Using TAG_VERSION: ${tagVersion}"
                    
                    sh """
                    docker build --build-arg VERSION=${version} -t tpfoyer:${tagVersion} .
                    docker tag tpfoyer:${tagVersion} arijhakouna/tpfoyer:${tagVersion}
                    docker login -u arijhakouna -p azerty123
                    docker push arijhakouna/tpfoyer:${tagVersion}
                    """
                }
            }
        }

        stage("Docker Compose") {
            steps {
                echo "Deploying with Docker Compose..."
                script {
                    def tagVersion = sh(script: "mvn help:evaluate -Dexpression=project.version -q -DforceStdout", returnStdout: true).trim()
                    
                    echo "Using DOCKER_TAG: ${tagVersion}"
                    sh """
                    export DOCKER_TAG=${tagVersion}
                    docker compose -f Docker-compose.yml up -d
                    docker ps -a
                    """
                }
            }
        }

        stage('Monitoring') {
            steps {
                echo "Monitoring CPU/RAM via Prometheus"
                sh '''
                PROM_URL="http://192.168.35.146:9090"
                CONTAINER="validation-tp-foyer-1"
                CPU=$(curl -s "$PROM_URL/api/v1/query?query=rate(container_cpu_usage_seconds_total{container_label_io_kubernetes_container_name=\\"$CONTAINER\\"}[1m]) * 100" | jq '.data.result[0].value[1]' | tr -d '"')
                RAM=$(curl -s "$PROM_URL/api/v1/query?query=container_memory_usage_bytes{container_label_io_kubernetes_container_name=\\"$CONTAINER\\"}" | jq '.data.result[0].value[1]' | tr -d '"')
                RAM_MB=$(echo "$RAM / 1024 / 1024" | bc)
                echo "CPU usage: $CPU %"
                echo "RAM usage: $RAM_MB MB"

                CONTAINER="validation-mysqldb-1"
                CPU=$(curl -s "$PROM_URL/api/v1/query?query=rate(container_cpu_usage_seconds_total{container_label_io_kubernetes_container_name=\\"$CONTAINER\\"}[1m]) * 100" | jq '.data.result[0].value[1]' | tr -d '"')
                RAM=$(curl -s "$PROM_URL/api/v1/query?query=container_memory_usage_bytes{container_label_io_kubernetes_container_name=\\"$CONTAINER\\"}" | jq '.data.result[0].value[1]' | tr -d '"')
                RAM_MB=$(echo "$RAM / 1024 / 1024" | bc)
                echo "CPU usage: $CPU %"
                echo "RAM usage: $RAM_MB MB"
                '''
                echo "Consulte le dashboard Grafana ici : http://192.168.35.146:3000/d/874c43a4-20d9-4f85-894e-d8a1615a931c"
            }
        }
    }

    post {
        always {
            echo "Pipeline completed with result: ${currentBuild.result}"
        }
        success {
            echo "Pipeline succeeded! Application deployed successfully with tag ${env.TAG_VERSION}"
        }
        failure {
            echo "Pipeline failed! Check the logs for details."
            mail to: 'Arij.Hakouna@esprit.tn',
                 subject: "[Jenkins] ECHEC du pipeline ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                 body: "Le pipeline a échoué. Consultez les logs Jenkins pour plus de détails."
        }
        unstable {
            mail to: 'Arij.Hakouna@esprit.tn',
                 subject: "[Jenkins] Pipeline instable ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                 body: "Le pipeline est instable. Consultez les logs Jenkins pour plus de détails."
        }
    }
}
