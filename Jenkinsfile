pipeline {
    agent any

    environment {
        // Public configuration only - no credentials
        SONAR_HOST_URL = 'http://192.168.1.4:9000'
        SONAR_PROJECT_KEY = 'tp-foyer'
        RECIPIENT_EMAIL = 'dhiboussema12@gmail.com'
        NEXUS_URL = 'http://192.168.1.4:8081'
        DOCKER_REGISTRY = 'dhibo'
    }

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

        stage("Generate Coverage Report") {
            steps {
                echo "OD ======> Generating JaCoCo coverage report..."
                sh 'mvn jacoco:report'
            }
        }

        stage("SonarQube Analysis") {
            steps {
                withSonarQubeEnv('SonarQubeServer') {
                    sh 'mvn sonar:sonar'
                }
            }
        }

        stage("Quality Gate") {
            steps {
                echo "OD ======> Checking Quality Gate..."
                script {
                    try {
                        timeout(time: 1, unit: 'MINUTES') {
                            def qg = waitForQualityGate abortPipeline: false
                            echo "OD ======> Quality Gate Status: ${qg.status}"
                            if (qg.status != 'OK') {
                                echo "OD ======> Quality Gate failed but continuing..."
                                currentBuild.result = 'UNSTABLE'
                            }
                        }
                    } catch (Exception e) {
                        echo "OD ======> Quality Gate check failed: ${e.message}"
                        echo "OD ======> Continuing pipeline anyway..."
                        currentBuild.result = 'UNSTABLE'
                    }
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

                    
                    sh """
                    docker build -t tpfoyer:latest .
                    docker tag tpfoyer:latest ${DOCKER_REGISTRY}/tpfoyer:latest
                    """
                }
            }
        }                
        
        stage("Deploy Image") {
            steps {
                script {
                    sh """
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
            
            script {
                def buildStatus = currentBuild.result ?: 'SUCCESS'
                def sonarUrl = "${SONAR_HOST_URL}/dashboard?id=${SONAR_PROJECT_KEY}"
                
                // Extract SonarQube metrics using secure credentials
                def metrics = extractSonarQubeMetricsSecure()
                
                // Send email report
                sendSimpleSonarQubeReport(buildStatus, sonarUrl, metrics)
            }
        }
        success {
            echo "OD ======> Pipeline succeeded! Application deployed successfully"
        }
        failure {
            echo "OD ======> Pipeline failed! Check the logs for details."
        }
        unstable {
            echo "OD ======> Pipeline completed with warnings (Quality Gate issues)"
        }
    }
}

// Secure function to extract SonarQube metrics using Jenkins credentials
def extractSonarQubeMetricsSecure() {
    def metrics = [:]
    
    try {
        echo "OD ======> Waiting for SonarQube to process results..."
        sleep(10)
        
        // Use Jenkins credentials securely
        withCredentials([string(credentialsId: 'creds', variable: 'SONAR_TOKEN')]) {
            def baseUrl = "${SONAR_HOST_URL}/api/measures/component?component=${SONAR_PROJECT_KEY}&metricKeys="
            
            // Get metrics using the secure token
            def coverage = sh(
                script: "curl -s -u \"\${SONAR_TOKEN}:\" '${baseUrl}coverage' | grep -o '\"value\":\"[^\"]*\"' | head -1 | cut -d'\"' -f4 || echo 'N/A'",
                returnStdout: true
            ).trim()
            
            def bugs = sh(
                script: "curl -s -u \"\${SONAR_TOKEN}:\" '${baseUrl}bugs' | grep -o '\"value\":\"[^\"]*\"' | head -1 | cut -d'\"' -f4 || echo 'N/A'",
                returnStdout: true
            ).trim()
            
            def vulnerabilities = sh(
                script: "curl -s -u \"\${SONAR_TOKEN}:\" '${baseUrl}vulnerabilities' | grep -o '\"value\":\"[^\"]*\"' | head -1 | cut -d'\"' -f4 || echo 'N/A'",
                returnStdout: true
            ).trim()
            
            def codeSmells = sh(
                script: "curl -s -u \"\${SONAR_TOKEN}:\" '${baseUrl}code_smells' | grep -o '\"value\":\"[^\"]*\"' | head -1 | cut -d'\"' -f4 || echo 'N/A'",
                returnStdout: true
            ).trim()
            
            def linesOfCode = sh(
                script: "curl -s -u \"\${SONAR_TOKEN}:\" '${baseUrl}ncloc' | grep -o '\"value\":\"[^\"]*\"' | head -1 | cut -d'\"' -f4 || echo 'N/A'",
                returnStdout: true
            ).trim()
            
            // Get Quality Gate status
            def qualityGate = sh(
                script: "curl -s -u \"\${SONAR_TOKEN}:\" '${SONAR_HOST_URL}/api/qualitygates/project_status?projectKey=${SONAR_PROJECT_KEY}' | grep -o '\"status\":\"[^\"]*\"' | head -1 | cut -d'\"' -f4 || echo 'N/A'",
                returnStdout: true
            ).trim()
            
            metrics.coverage = coverage ?: 'N/A'
            metrics.bugs = bugs ?: 'N/A'
            metrics.vulnerabilities = vulnerabilities ?: 'N/A'
            metrics.codeSmells = codeSmells ?: 'N/A'
            metrics.linesOfCode = linesOfCode ?: 'N/A'
            metrics.qualityGate = qualityGate ?: 'N/A'
        }
        
        echo "OD ======> Extracted metrics: Coverage=${metrics.coverage}, Bugs=${metrics.bugs}, Vulnerabilities=${metrics.vulnerabilities}, Code Smells=${metrics.codeSmells}, Lines=${metrics.linesOfCode}, Quality Gate=${metrics.qualityGate}"
        
    } catch (Exception e) {
        echo "OD ======> Error extracting SonarQube metrics: ${e.getMessage()}"
        metrics = [
            coverage: 'ERROR',
            bugs: 'ERROR',
            vulnerabilities: 'ERROR',
            codeSmells: 'ERROR',
            linesOfCode: 'ERROR',
            qualityGate: 'ERROR'
        ]
    }
    
    return metrics
}

// Function to send email report
def sendSimpleSonarQubeReport(buildStatus, sonarUrl, metrics) {
    def subject = "OD ======> Pipeline Report - ${env.JOB_NAME} #${env.BUILD_NUMBER} - ${buildStatus}"
    
    // Dynamic status colors and icons
    def statusColor = buildStatus == 'SUCCESS' ? '#4CAF50' : (buildStatus == 'UNSTABLE' ? '#FF9800' : '#f44336')
    def statusIcon = buildStatus == 'SUCCESS' ? '✅' : (buildStatus == 'UNSTABLE' ? '⚠️' : '❌')
    
    def emailBody = """
    <html>
    <head>
        <style>
            body { font-family: Arial, sans-serif; margin: 20px; }
            .header { background-color: ${statusColor}; color: white; padding: 15px; text-align: center; }
            .content { padding: 20px; }
            .metrics { background-color: #f9f9f9; padding: 15px; margin: 10px 0; border-radius: 5px; }
            .success { color: #4CAF50; font-weight: bold; }
            .warning { color: #FF9800; font-weight: bold; }
            .error { color: #f44336; font-weight: bold; }
            .link { color: #2196F3; text-decoration: none; }
            .quality-gate { padding: 10px; margin: 10px 0; border-radius: 5px; }
            .quality-gate.OK { background-color: #d4edda; color: #155724; }
            .quality-gate.ERROR { background-color: #f8d7da; color: #721c24; }
            .quality-gate.N_A { background-color: #fff3cd; color: #856404; }
        </style>
    </head>
    <body>
        <div class="header">
            <h2>${statusIcon} Jenkins Pipeline Report</h2>
        </div>
        
        <div class="content">
            <h3>🔧 Build Information</h3>
            <ul>
                <li><strong>Project:</strong> ${env.JOB_NAME}</li>
                <li><strong>Build Number:</strong> ${env.BUILD_NUMBER}</li>
                <li><strong>Status:</strong> <span class="${buildStatus == 'SUCCESS' ? 'success' : (buildStatus == 'UNSTABLE' ? 'warning' : 'error')}">${buildStatus}</span></li>
                <li><strong>Build URL:</strong> <a href="${env.BUILD_URL}" class="link">${env.BUILD_URL}</a></li>
                <li><strong>Duration:</strong> ${currentBuild.durationString}</li>
                <li><strong>Node:</strong> ${env.NODE_NAME}</li>
            </ul>
            
            <h3>📊 SonarQube Analysis Results</h3>
            <div class="quality-gate ${metrics.qualityGate.replace('/', '_')}">
                <h4>🎯 Quality Gate Status: ${metrics.qualityGate}</h4>
            </div>
            
            <div class="metrics">
                <p><strong>📈 Code Coverage:</strong> ${metrics.coverage}${metrics.coverage != 'N/A' && metrics.coverage != 'ERROR' ? '%' : ''}</p>
                <p><strong>🐛 Bugs:</strong> ${metrics.bugs}</p>
                <p><strong>🔒 Vulnerabilities:</strong> ${metrics.vulnerabilities}</p>
                <p><strong>💡 Code Smells:</strong> ${metrics.codeSmells}</p>
                <p><strong>📝 Lines of Code:</strong> ${metrics.linesOfCode}</p>
            </div>
            
            <h3>🔗 Quick Actions</h3>
            <ul>
                <li><a href="${sonarUrl}" class="link">📈 View Full SonarQube Report</a></li>
                <li><a href="${env.BUILD_URL}console" class="link">📋 View Build Console</a></li>
                <li><a href="${SONAR_HOST_URL}/projects" class="link">📊 All SonarQube Projects</a></li>
                <li><a href="${env.BUILD_URL}testReport" class="link">🧪 Test Results</a></li>
            </ul>
            
            <h3>💡 Quality Recommendations</h3>
            <ul>
                <li>✅ Target: Code coverage above 80%</li>
                <li>✅ Priority: Fix all bugs and vulnerabilities</li>
                <li>✅ Maintenance: Reduce code smells</li>
                <li>✅ Goal: Ensure Quality Gate passes</li>
            </ul>
        </div>
        
        <div class="footer" style="margin-top: 30px; padding: 10px; background-color: #f0f0f0; text-align: center;">
            <p><strong>🚀 Generated by Jenkins CI/CD Pipeline</strong></p>
            <p>Environment: ${env.NODE_NAME} | Build Date: ${new Date()}</p>
        </div>
    </body>
    </html>
    """
    
    try {
        mail (
            to: "${RECIPIENT_EMAIL}",
            subject: subject,
            body: emailBody,
            mimeType: 'text/html'
        )
        echo "OD ======> Email report sent successfully to ${RECIPIENT_EMAIL}!"
    } catch (Exception e) {
        echo "OD ======> Failed to send email: ${e.getMessage()}"
    }
}
