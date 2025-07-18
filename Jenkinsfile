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

        stage("SonarQube Analysis") {
            steps {
                withSonarQubeEnv('SonarQubeServer') {
                    sh 'mvn sonar:sonar'
                }
            }
        }

        stage("Quality Gate") {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: false
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
            
            // Extract SonarQube results and send email
            script {
                def buildStatus = currentBuild.result ?: 'SUCCESS'
                def sonarUrl = "http://192.168.1.4:9000/dashboard?id=tp-foyer"
                
                // Extract SonarQube metrics
                def sonarMetrics = extractSonarQubeMetrics()
                
                // Send email with SonarQube results
                sendSonarQubeReport(buildStatus, sonarUrl, sonarMetrics)
            }
        }
        success {
            echo "OD ======> Pipeline succeeded! Application deployed successfully"
        }
        failure {
            echo "OD ======> Pipeline failed! Check the logs for details."
        }
    }
}

// Function to extract SonarQube metrics
def extractSonarQubeMetrics() {
    def metrics = [:]
    
    try {
        // Get SonarQube project metrics via API
        def response = sh(
            script: """
            curl -s -u admin:admin \
            'http://192.168.1.4:9000/api/measures/component?component=tp-foyer&metricKeys=coverage,bugs,vulnerabilities,code_smells,duplicated_lines_density,lines,ncloc,sqale_index,reliability_rating,security_rating,sqale_rating'
            """,
            returnStdout: true
        ).trim()
        
        echo "OD ======> SonarQube API Response: ${response}"
        
        // Parse JSON response
        def jsonResponse = readJSON text: response
        
        // Extract metrics
        jsonResponse.component.measures.each { measure ->
            switch(measure.metric) {
                case 'coverage':
                    metrics.coverage = measure.value ?: '0.0'
                    break
                case 'bugs':
                    metrics.bugs = measure.value ?: '0'
                    break
                case 'vulnerabilities':
                    metrics.vulnerabilities = measure.value ?: '0'
                    break
                case 'code_smells':
                    metrics.codeSmells = measure.value ?: '0'
                    break
                case 'duplicated_lines_density':
                    metrics.duplication = measure.value ?: '0.0'
                    break
                case 'lines':
                    metrics.totalLines = measure.value ?: '0'
                    break
                case 'ncloc':
                    metrics.linesOfCode = measure.value ?: '0'
                    break
                case 'sqale_index':
                    metrics.technicalDebt = measure.value ?: '0'
                    break
                case 'reliability_rating':
                    metrics.reliabilityRating = getRatingLabel(measure.value)
                    break
                case 'security_rating':
                    metrics.securityRating = getRatingLabel(measure.value)
                    break
                case 'sqale_rating':
                    metrics.maintainabilityRating = getRatingLabel(measure.value)
                    break
            }
        }
        
        echo "OD ======> Extracted metrics: ${metrics}"
        
    } catch (Exception e) {
        echo "OD ======> Error extracting SonarQube metrics: ${e.getMessage()}"
        metrics = [
            coverage: 'N/A',
            bugs: 'N/A',
            vulnerabilities: 'N/A',
            codeSmells: 'N/A',
            duplication: 'N/A',
            totalLines: 'N/A',
            linesOfCode: 'N/A',
            technicalDebt: 'N/A',
            reliabilityRating: 'N/A',
            securityRating: 'N/A',
            maintainabilityRating: 'N/A'
        ]
    }
    
    return metrics
}

// Function to convert rating numbers to labels
def getRatingLabel(value) {
    if (value == null) return 'N/A'
    
    switch(value.toString()) {
        case '1.0':
            return 'A'
        case '2.0':
            return 'B'
        case '3.0':
            return 'C'
        case '4.0':
            return 'D'
        case '5.0':
            return 'E'
        default:
            return value.toString()
    }
}

// Function to send email with SonarQube results
def sendSonarQubeReport(buildStatus, sonarUrl, metrics) {
    def subject = "OD ======> SonarQube Report - ${env.JOB_NAME} #${env.BUILD_NUMBER} - ${buildStatus}"
    
    def emailBody = """
    <html>
    <head>
        <style>
            body { font-family: Arial, sans-serif; margin: 20px; }
            .header { background-color: #4CAF50; color: white; padding: 10px; text-align: center; }
            .content { padding: 20px; }
            .metrics-table { width: 100%; border-collapse: collapse; margin: 20px 0; }
            .metrics-table th, .metrics-table td { border: 1px solid #ddd; padding: 8px; text-align: left; }
            .metrics-table th { background-color: #f2f2f2; }
            .success { color: #4CAF50; }
            .warning { color: #ff9800; }
            .error { color: #f44336; }
            .footer { margin-top: 30px; padding: 10px; background-color: #f9f9f9; }
        </style>
    </head>
    <body>
        <div class="header">
            <h2>OD ======> SonarQube Analysis Report</h2>
        </div>
        
        <div class="content">
            <h3>Build Information</h3>
            <ul>
                <li><strong>Project:</strong> ${env.JOB_NAME}</li>
                <li><strong>Build Number:</strong> ${env.BUILD_NUMBER}</li>
                <li><strong>Status:</strong> <span class="${buildStatus == 'SUCCESS' ? 'success' : 'error'}">${buildStatus}</span></li>
                <li><strong>Build URL:</strong> <a href="${env.BUILD_URL}">${env.BUILD_URL}</a></li>
                <li><strong>SonarQube Dashboard:</strong> <a href="${sonarUrl}">View Full Report</a></li>
            </ul>
            
            <h3>Code Quality Metrics</h3>
            <table class="metrics-table">
                <tr><th>Metric</th><th>Value</th><th>Description</th></tr>
                <tr>
                    <td>Code Coverage</td>
                    <td><strong>${metrics.coverage}%</strong></td>
                    <td>Percentage of code covered by tests</td>
                </tr>
                <tr>
                    <td>Bugs</td>
                    <td><strong>${metrics.bugs}</strong></td>
                    <td>Number of bugs found</td>
                </tr>
                <tr>
                    <td>Vulnerabilities</td>
                    <td><strong>${metrics.vulnerabilities}</strong></td>
                    <td>Security vulnerabilities</td>
                </tr>
                <tr>
                    <td>Code Smells</td>
                    <td><strong>${metrics.codeSmells}</strong></td>
                    <td>Maintainability issues</td>
                </tr>
                <tr>
                    <td>Duplication</td>
                    <td><strong>${metrics.duplication}%</strong></td>
                    <td>Duplicated lines density</td>
                </tr>
                <tr>
                    <td>Lines of Code</td>
                    <td><strong>${metrics.linesOfCode}</strong></td>
                    <td>Non-comment lines of code</td>
                </tr>
                <tr>
                    <td>Total Lines</td>
                    <td><strong>${metrics.totalLines}</strong></td>
                    <td>Total lines including comments</td>
                </tr>
            </table>
            
            <h3>Quality Ratings</h3>
            <table class="metrics-table">
                <tr><th>Category</th><th>Rating</th><th>Description</th></tr>
                <tr>
                    <td>Reliability</td>
                    <td><strong>${metrics.reliabilityRating}</strong></td>
                    <td>Based on bugs and their severity</td>
                </tr>
                <tr>
                    <td>Security</td>
                    <td><strong>${metrics.securityRating}</strong></td>
                    <td>Based on vulnerabilities and their severity</td>
                </tr>
                <tr>
                    <td>Maintainability</td>
                    <td><strong>${metrics.maintainabilityRating}</strong></td>
                    <td>Based on code smells and technical debt</td>
                </tr>
            </table>
            
            <h3>Recommendations</h3>
            <ul>
                <li>Coverage should be above 80%</li>
                <li>Bugs and vulnerabilities should be 0</li>
                <li>Code smells should be minimized</li>
                <li>Duplication should be below 3%</li>
                <li>All ratings should be A or B</li>
            </ul>
        </div>
        
        <div class="footer">
            <p><strong>OD ======> Generated by Jenkins CI/CD Pipeline</strong></p>
            <p>For detailed analysis, visit: <a href="${sonarUrl}">SonarQube Dashboard</a></p>
        </div>
    </body>
    </html>
    """
    
    try {
        emailext (
            subject: subject,
            body: emailBody,
            to: 'your-email@gmail.com',
            mimeType: 'text/html'
        )
        echo "OD ======> SonarQube report sent successfully!"
    } catch (Exception e) {
        echo "OD ======> Failed to send SonarQube report: ${e.getMessage()}"
    }
}
