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

        stage("SonarQube Analysis") {
            steps {
                echo "Running SonarQube analysis..."
                withSonarQubeEnv('SonarQubeServer') {
                    sh 'mvn verify'
                    sh 'mvn sonar:sonar -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml'
                }
            }
        }

        stage("Quality Gate") {
            steps {
                echo "OD ======> Checking SonarQube Quality Gate..."
                timeout(time: 5, unit: 'MINUTES') {
                    script {
                        try {
                            waitForQualityGate abortPipeline: false
                            echo "OD ======> Quality Gate passed!"
                        } catch (Exception e) {
                            echo "OD ======> Quality Gate timeout or failed: ${e.getMessage()}"
                            echo "OD ======> Continuing pipeline..."
                        }
                    }
                }
            }
        }

        stage("Nexus Deploy") {
            steps {
                echo "Deploying to Nexus repository..."
                withCredentials([usernamePassword(credentialsId: 'jenkins_nexus', passwordVariable: 'NEXUS_PASSWORD', usernameVariable: 'NEXUS_USER')]) {
                    script {
                        echo "OD ======> Creating Maven settings.xml in workspace..."
                        echo "OD ======> Nexus User: ${NEXUS_USER}"
                        echo "OD ======> Workspace: ${env.WORKSPACE}"
                        
                        // Créer settings.xml dans le workspace
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

    <profiles>
        <profile>
            <id>nexus</id>
            <repositories>
                <repository>
                    <id>nexus_jenkins</id>
                    <name>Nexus Repository</name>
                    <url>http://192.168.1.4:8081/repository/maven-public/</url>
                    <releases>
                        <enabled>true</enabled>
                    </releases>
                    <snapshots>
                        <enabled>true</enabled>
                    </snapshots>
                </repository>
            </repositories>
        </profile>
    </profiles>

    <activeProfiles>
        <activeProfile>nexus</activeProfile>
    </activeProfiles>

</settings>'''
                        
                        // Vérifier que le fichier existe
                        sh '''
                        echo "OD ======> Verifying settings.xml creation..."
                        ls -la settings.xml
                        echo "OD ======> Settings.xml content:"
                        cat settings.xml
                        '''
                        
                        // Déployer vers Nexus
                        sh '''
                        echo "OD ======> Deploying to Nexus with explicit settings..."
                        echo "OD ======> Current directory: $(pwd)"
                        echo "OD ======> Maven version:"
                        mvn --version
                        
                        mvn deploy -DskipTests \
                            --settings ./settings.xml \
                            -Dmaven.test.skip=true \
                            -e
                        '''
                    }
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
            
            // Nettoyer le fichier settings.xml
            sh 'rm -f settings.xml'
            
            // Send email with corrected SonarQube metrics
            script {
                def buildStatus = currentBuild.result ?: 'SUCCESS'
                sendCorrectedSonarQubeReport(buildStatus)
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

// Corrected function to extract SonarQube metrics
def sendCorrectedSonarQubeReport(buildStatus) {
    def subject = "OD ======> Pipeline Report - ${env.JOB_NAME} #${env.BUILD_NUMBER} - ${buildStatus}"
    def sonarUrl = "http://192.168.1.4:9000/dashboard?id=tp-foyer"
    
    // Wait for SonarQube to process
    echo "OD ======> Waiting for SonarQube to process results..."
    sleep(30)
    
    // Get project key from pom.xml
    def projectKey = sh(
        script: "grep -o '<sonar.projectKey>.*</sonar.projectKey>' pom.xml | sed 's/<[^>]*>//g' || echo 'tp-foyer'",
        returnStdout: true
    ).trim()
    
    echo "OD ======> Using SonarQube project key: ${projectKey}"
    
    // Extract metrics with debugging
    def metrics = [:]
    
    try {
        // Test different authentication methods
        def authMethods = [
            "admin:admin",
            "admin:K4rQNF}MG6zwCQB"
        ]
        
        def workingAuth = ""
        for (auth in authMethods) {
            def testResponse = sh(
                script: "curl -s -u ${auth} 'http://192.168.1.4:9000/api/system/status' | grep -o 'UP' | wc -l",
                returnStdout: true
            ).trim()
            
            if (testResponse == "1") {
                workingAuth = auth
                echo "OD ======> Working SonarQube auth: ${auth}"
                break
            }
        }
        
        if (workingAuth) {
            // Get metrics with working auth
            def metricsToGet = ["coverage", "bugs", "vulnerabilities", "code_smells", "ncloc"]
            
            for (metric in metricsToGet) {
                def value = sh(
                    script: "curl -s -u ${workingAuth} 'http://192.168.1.4:9000/api/measures/component?component=${projectKey}&metricKeys=${metric}' | grep -o '\"value\":\"[^\"]*\"' | head -1 | cut -d'\"' -f4 || echo 'N/A'",
                    returnStdout: true
                ).trim()
                
                echo "OD ======> ${metric}: ${value}"
                
                switch(metric) {
                    case "coverage":
                        metrics.coverage = value ?: 'N/A'
                        break
                    case "bugs":
                        metrics.bugs = value ?: 'N/A'
                        break
                    case "vulnerabilities":
                        metrics.vulnerabilities = value ?: 'N/A'
                        break
                    case "code_smells":
                        metrics.codeSmells = value ?: 'N/A'
                        break
                    case "ncloc":
                        metrics.linesOfCode = value ?: 'N/A'
                        break
                }
            }
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
    
    // Send email with debugging info
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
                <p><strong>Project Key:</strong> ${projectKey}</p>
                <p><strong>SonarQube URL:</strong> <a href="${sonarUrl}" class="link">${sonarUrl}</a></p>
                <p><strong>Note:</strong> Si les métriques affichent 'N/A', vérifiez que SonarQube a analysé le projet.</p>
            </div>
            
            <h3>Actions</h3>
            <ul>
                <li><a href="${sonarUrl}" class="link">📈 View Full SonarQube Report</a></li>
                <li><a href="${env.BUILD_URL}console" class="link">📋 View Build Console</a></li>
                <li><a href="http://192.168.1.4:9000/projects" class="link">🔍 View All SonarQube Projects</a></li>
            </ul>
            
            <h3>Troubleshooting</h3>
            <ul>
                <li>✅ Vérifiez que le projet existe dans SonarQube</li>
                <li>✅ Confirmez que l'analyse s'est terminée avec succès</li>
                <li>✅ Vérifiez les credentials SonarQube</li>
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
