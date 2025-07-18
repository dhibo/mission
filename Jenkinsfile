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
                echo "OD ======> Checking Quality Gate..."
                timeout(time: 2, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: false
                }
            }
        }
        
        stage("Nexus Deploy") {
            steps {
                echo "OD ======> Deploying to Nexus repository..."
                withCredentials([usernamePassword(credentialsId: 'jenkins_nexus', passwordVariable: 'NEXUS_PASSWORD', usernameVariable: 'NEXUS_USER')]) {
                    script {
                        // Créer settings.xml dynamiquement
                        writeFile file: 'settings.xml', text: '''<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                              https://maven.apache.org/xsd/settings-1.0.0.xsd">
    <servers>
        <server>
            <id>nexus_jenkins</id>
            <username>''' + env.NEXUS_USER + '''</username>
            <password>''' + env.NEXUS_PASSWORD + '''</password>
        </server>
    </servers>
</settings>'''
                        
                        sh '''
                        echo "OD ======> Using Nexus user: ${NEXUS_USER}"
                        echo "OD ======> Deploying with settings.xml..."
                        mvn deploy -DskipTests --settings ./settings.xml
                        '''
                    }
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
            
            // Nettoyer settings.xml
            sh 'rm -f settings.xml'
            
            // Send simple email with SonarQube link
            script {
                def buildStatus = currentBuild.result ?: 'SUCCESS'
                def sonarUrl = "http://192.168.1.4:9000/dashboard?id=tp-foyer"
                
                // Extract basic SonarQube metrics using shell commands
                def metrics = extractSonarQubeMetricsSimple()
                
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
    }
}

// Simplified function to extract SonarQube metrics without readJSON
def extractSonarQubeMetricsSimple() {
    def metrics = [:]
    
    try {
        echo "OD ======> Waiting for SonarQube to process results..."
        sleep(20)
        
        // Test different authentication methods
        def authMethods = ["admin:admin", "admin:K4rQNF}MG6zwCQB"]
        def workingAuth = ""
        
        for (auth in authMethods) {
            def testResponse = sh(
                script: "curl -s -u ${auth} 'http://192.168.1.4:9000/api/system/status' | grep -c 'UP' || echo '0'",
                returnStdout: true
            ).trim()
            
            if (testResponse == "1") {
                workingAuth = auth
                echo "OD ======> Working SonarQube auth: ${auth}"
                break
            }
        }
        
        if (workingAuth) {
            // Get project key from pom.xml
            def projectKey = sh(
                script: "grep -o '<sonar.projectKey>.*</sonar.projectKey>' pom.xml | sed 's/<[^>]*>//g' || echo 'tp-foyer'",
                returnStdout: true
            ).trim()
            
            echo "OD ======> Using project key: ${projectKey}"
            
            // Get basic metrics using curl and grep
            def coverage = sh(
                script: "curl -s -u ${workingAuth} 'http://192.168.1.4:9000/api/measures/component?component=${projectKey}&metricKeys=coverage' | grep -o '\"value\":\"[^\"]*\"' | head -1 | cut -d'\"' -f4 || echo 'N/A'",
                returnStdout: true
            ).trim()
            
            def bugs = sh(
                script: "curl -s -u ${workingAuth} 'http://192.168.1.4:9000/api/measures/component?component=${projectKey}&metricKeys=bugs' | grep -o '\"value\":\"[^\"]*\"' | head -1 | cut -d'\"' -f4 || echo 'N/A'",
                returnStdout: true
            ).trim()
            
            def vulnerabilities = sh(
                script: "curl -s -u ${workingAuth} 'http://192.168.1.4:9000/api/measures/component?component=${projectKey}&metricKeys=vulnerabilities' | grep -o '\"value\":\"[^\"]*\"' | head -1 | cut -d'\"' -f4 || echo 'N/A'",
                returnStdout: true
            ).trim()
            
            def codeSmells = sh(
                script: "curl -s -u ${workingAuth} 'http://192.168.1.4:9000/api/measures/component?component=${projectKey}&metricKeys=code_smells' | grep -o '\"value\":\"[^\"]*\"' | head -1 | cut -d'\"' -f4 || echo 'N/A'",
                returnStdout: true
            ).trim()
            
            def linesOfCode = sh(
                script: "curl -s -u ${workingAuth} 'http://192.168.1.4:9000/api/measures/component?component=${projectKey}&metricKeys=ncloc' | grep -o '\"value\":\"[^\"]*\"' | head -1 | cut -d'\"' -f4 || echo 'N/A'",
                returnStdout: true
            ).trim()
            
            metrics.coverage = coverage ?: 'N/A'
            metrics.bugs = bugs ?: 'N/A'
            metrics.vulnerabilities = vulnerabilities ?: 'N/A'
            metrics.codeSmells = codeSmells ?: 'N/A'
            metrics.linesOfCode = linesOfCode ?: 'N/A'
            
            echo "OD ======> Extracted metrics: Coverage=${metrics.coverage}, Bugs=${metrics.bugs}, Vulnerabilities=${metrics.vulnerabilities}, Code Smells=${metrics.codeSmells}, Lines=${metrics.linesOfCode}"
        } else {
            echo "OD ======> No working SonarQube authentication found"
            metrics = [
                coverage: 'AUTH_ERROR',
                bugs: 'AUTH_ERROR',
                vulnerabilities: 'AUTH_ERROR',
                codeSmells: 'AUTH_ERROR',
                linesOfCode: 'AUTH_ERROR'
            ]
        }
        
    } catch (Exception e) {
        echo "OD ======> Error extracting SonarQube metrics: ${e.getMessage()}"
        metrics = [
            coverage: 'ERROR',
            bugs: 'ERROR',
            vulnerabilities: 'ERROR',
            codeSmells: 'ERROR',
            linesOfCode: 'ERROR'
        ]
    }
    
    return metrics
}

// Simplified function to send email report
def sendSimpleSonarQubeReport(buildStatus, sonarUrl, metrics) {
    def subject = "OD ======> Pipeline Report - ${env.JOB_NAME} #${env.BUILD_NUMBER} - ${buildStatus}"
    
    def emailBody = """
    <html>
    <head>
        <style>
            body { font-family: Arial, sans-serif; margin: 20px; }
            .header { background-color: #4CAF50; color: white; padding: 15px; text-align: center; }
            .content { padding: 20px; }
            .metrics { background-color: #f9f9f9; padding: 15px; margin: 10px 0; }
            .success { color: #4CAF50; font-weight: bold; }
            .error { color: #f44336; font-weight: bold; }
            .link { color: #2196F3; text-decoration: none; }
            .debug { background-color: #fff3cd; padding: 10px; margin: 10px 0; }
        </style>
    </head>
    <body>
        <div class="header">
            <h2>OD ======> Jenkins Pipeline Report</h2>
        </div>
        
        <div class="content">
            <h3>Build Information</h3>
            <ul>
                <li><strong>Project:</strong> ${env.JOB_NAME}</li>
                <li><strong>Build Number:</strong> ${env.BUILD_NUMBER}</li>
                <li><strong>Status:</strong> <span class="${buildStatus == 'SUCCESS' ? 'success' : 'error'}">${buildStatus}</span></li>
                <li><strong>Build URL:</strong> <a href="${env.BUILD_URL}" class="link">${env.BUILD_URL}</a></li>
            </ul>
            
            <h3>SonarQube Analysis Results</h3>
            <div class="metrics">
                <p><strong>📊 Code Coverage:</strong> ${metrics.coverage}%</p>
                <p><strong>🐛 Bugs:</strong> ${metrics.bugs}</p>
                <p><strong>🔒 Vulnerabilities:</strong> ${metrics.vulnerabilities}</p>
                <p><strong>💡 Code Smells:</strong> ${metrics.codeSmells}</p>
                <p><strong>📝 Lines of Code:</strong> ${metrics.linesOfCode}</p>
            </div>
            
            <div class="debug">
                <h4>Debug Information</h4>
                <p><strong>SonarQube URL:</strong> <a href="${sonarUrl}" class="link">${sonarUrl}</a></p>
                <p><strong>All Projects:</strong> <a href="http://192.168.1.4:9000/projects" class="link">View All SonarQube Projects</a></p>
            </div>
            
            <h3>Actions</h3>
            <ul>
                <li><a href="${sonarUrl}" class="link">📈 View Full SonarQube Report</a></li>
                <li><a href="${env.BUILD_URL}console" class="link">📋 View Build Console</a></li>
            </ul>
            
            <h3>Recommendations</h3>
            <ul>
                <li>✅ Keep code coverage above 80%</li>
                <li>✅ Fix all bugs and vulnerabilities</li>
                <li>✅ Reduce code smells for better maintainability</li>
            </ul>
        </div>
        
        <div class="footer" style="margin-top: 30px; padding: 10px; background-color: #f0f0f0; text-align: center;">
            <p><strong>OD ======> Generated by Jenkins CI/CD Pipeline</strong></p>
        </div>
    </body>
    </html>
    """
    
    try {
        mail (
            to: 'dhiboussema12@gmail.com',
            subject: subject,
            body: emailBody,
            mimeType: 'text/html'
        )
        echo "OD ======> Email report sent successfully!"
    } catch (Exception e) {
        echo "OD ======> Failed to send email: ${e.getMessage()}"
    }
} 
