pipeline {
    agent any
    
    parameters {
        choice(
            name: 'DEVICE_NAME',
            choices: ['pixel_7', 'pixel_6', 'nexus_5'],
            description: 'Select the Android device/emulator to run tests on'
        )
        string(
            name: 'UDID',
            defaultValue: 'emulator-5554',
            description: 'Device UDID or emulator port'
        )
        choice(
            name: 'TEST_SUITE',
            choices: ['testng.xml', 'smoke_tests', 'regression_tests'],
            description: 'Select test suite to execute'
        )
    }
    
    environment {
        ANDROID_HOME = '/opt/android-sdk'
        JAVA_HOME = '/usr/lib/jvm/java-21-openjdk'
        PATH = "${ANDROID_HOME}/platform-tools:${ANDROID_HOME}/tools:${ANDROID_HOME}/emulator:${JAVA_HOME}/bin:${PATH}"
        MAVEN_OPTS = '-Xmx1024m'
        BUILD_TIMESTAMP = "${new Date().format('yyyy-MM-dd_HH-mm-ss')}"
    }
    
    stages {
        stage('Checkout') {
            steps {
                script {
                    echo "Starting Appium Test Pipeline at ${BUILD_TIMESTAMP}"
                    echo "Selected Device: ${params.DEVICE_NAME}"
                    echo "UDID: ${params.UDID}"
                }
                checkout scm
            }
        }
        
        stage('Environment Setup') {
            parallel {
                stage('Install Dependencies') {
                    steps {
                        sh '''
                            echo "Checking Java version..."
                            java -version
                            
                            echo "Checking Maven version..."
                            mvn -version
                            
                            echo "Installing Node.js and Appium (backup in case programmatic server fails)..."
                            curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
                            sudo apt-get install -y nodejs
                            sudo npm install -g appium@2.0.0
                            sudo npm install -g @appium/doctor
                            
                            echo "Verifying Appium installation..."
                            appium --version
                        '''
                    }
                }
                
                stage('Verify Android Setup') {
                    steps {
                        sh '''
                            echo "Checking Android SDK installation..."
                            if [ ! -d "$ANDROID_HOME" ]; then
                                echo "Android SDK not found. Installing..."
                                sudo mkdir -p /opt/android-sdk
                                cd /opt/android-sdk
                                sudo wget https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip
                                sudo unzip commandlinetools-linux-9477386_latest.zip
                                sudo rm commandlinetools-linux-9477386_latest.zip
                            fi
                            
                            echo "Listing available devices..."
                            adb devices
                            
                            echo "Checking emulator status..."
                            ${ANDROID_HOME}/emulator/emulator -list-avds || true
                        '''
                    }
                }
            }
        }
        
        stage('Prepare Test Environment') {
            steps {
                sh '''
                    echo "Cleaning previous test artifacts..."
                    rm -rf target/
                    rm -rf test-output/
                    rm -rf logs/*.log
                    
                    echo "Creating necessary directories..."
                    mkdir -p logs
                    mkdir -p Videos
                    mkdir -p Screenshots
                    
                    echo "Checking APK file..."
                    if [ -f "src/main/resources/App/draganddrop.apk" ]; then
                        echo "APK file found: $(ls -la src/main/resources/App/draganddrop.apk)"
                    else
                        echo "ERROR: APK file not found!"
                        exit 1
                    fi
                '''
            }
        }
        
        stage('Start Android Emulator') {
            when {
                expression { params.UDID.startsWith('emulator-') }
            }
            steps {
                script {
                    sh '''
                        echo "Starting Android emulator..."
                        
                        # Check if emulator is already running
                        if adb devices | grep -q "${UDID}"; then
                            echo "Emulator ${UDID} is already running"
                        else
                            echo "Starting emulator ${DEVICE_NAME}..."
                            
                            # Start emulator in background
                            nohup ${ANDROID_HOME}/emulator/emulator -avd ${DEVICE_NAME} -port ${UDID##*-} -no-audio -no-window -gpu swiftshader_indirect &
                            
                            # Wait for emulator to be ready
                            echo "Waiting for emulator to start..."
                            timeout=300
                            while [ $timeout -gt 0 ]; do
                                if adb devices | grep -q "${UDID}"; then
                                    echo "Emulator is connected"
                                    break
                                fi
                                sleep 5
                                timeout=$((timeout - 5))
                            done
                            
                            if [ $timeout -le 0 ]; then
                                echo "ERROR: Emulator failed to start within 5 minutes"
                                exit 1
                            fi
                            
                            # Wait for emulator to be fully booted
                            echo "Waiting for emulator to be ready..."
                            adb -s ${UDID} wait-for-device shell 'while [[ -z $(getprop sys.boot_completed | tr -d \'\\r\') ]]; do sleep 1; done; input keyevent 82'
                        fi
                        
                        echo "Emulator is ready!"
                        adb devices
                    '''
                }
            }
        }
        
        stage('Build and Test') {
            steps {
                script {
                    try {
                        sh '''
                            echo "Compiling the project..."
                            mvn clean compile -DskipTests=true
                            
                            echo "Running Appium tests..."
                            echo "Note: Appium server will be started programmatically by the framework"
                            
                            # Update TestNG XML with current device parameters
                            sed -i "s/value=\"pixel_7\"/value=\"${DEVICE_NAME}\"/g" testng.xml
                            sed -i "s/value=\"emulator-5554\"/value=\"${UDID}\"/g" testng.xml
                            
                            echo "Updated testng.xml with device: ${DEVICE_NAME}, UDID: ${UDID}"
                            cat testng.xml
                            
                            # Run tests with Maven Surefire plugin
                            mvn test -Dsurefire.suiteXmlFiles=testng.xml -DforkCount=1 -DreuseForks=false
                        '''
                    } catch (Exception e) {
                        currentBuild.result = 'UNSTABLE'
                        echo "Tests completed with failures: ${e.getMessage()}"
                    }
                }
            }
        }
        
        stage('Generate Reports') {
            steps {
                sh '''
                    echo "Generating test reports..."
                    
                    # Archive TestNG reports
                    if [ -d "test-output" ]; then
                        echo "TestNG reports found"
                        ls -la test-output/
                    fi
                    
                    # Archive Surefire reports
                    if [ -d "target/surefire-reports" ]; then
                        echo "Surefire reports found"
                        ls -la target/surefire-reports/
                    fi
                    
                    # Archive logs
                    if [ -d "logs" ]; then
                        echo "Log files found"
                        ls -la logs/
                    fi
                    
                    echo "Report generation completed"
                '''
            }
        }
    }
    
    post {
        always {
            script {
                // Clean up emulator if it was started
                if (params.UDID.startsWith('emulator-')) {
                    sh '''
                        echo "Cleaning up emulator..."
                        adb -s ${UDID} emu kill || true
                        pkill -f "emulator.*${DEVICE_NAME}" || true
                    '''
                }
                
                // Archive artifacts
                archiveArtifacts artifacts: 'test-output/**/*', allowEmptyArchive: true
                archiveArtifacts artifacts: 'target/surefire-reports/**/*', allowEmptyArchive: true
                archiveArtifacts artifacts: 'logs/**/*', allowEmptyArchive: true
                archiveArtifacts artifacts: 'Screenshots/**/*', allowEmptyArchive: true
                archiveArtifacts artifacts: 'Videos/**/*', allowEmptyArchive: true
                
                // Publish TestNG results
                publishTestResults testResultsPattern: 'test-output/testng-results.xml, target/surefire-reports/TEST-*.xml'
                
                // Publish HTML reports
                publishHTML([
                    allowMissing: false,
                    alwaysLinkToLastBuild: true,
                    keepAll: true,
                    reportDir: 'test-output',
                    reportFiles: 'emailable-report.html',
                    reportName: 'TestNG HTML Report'
                ])
            }
        }
        
        success {
            script {
                def testResults = readFile('test-output/emailable-report.html')
                emailext(
                    to: 'shaiz.akber@venturedive.com',
                    subject: "✅ Appium Test Suite PASSED - Build #${BUILD_NUMBER}",
                    body: """
                    <h2>🎉 Appium Test Execution Successful!</h2>
                    
                    <h3>Build Information:</h3>
                    <ul>
                        <li><strong>Build Number:</strong> ${BUILD_NUMBER}</li>
                        <li><strong>Build Timestamp:</strong> ${BUILD_TIMESTAMP}</li>
                        <li><strong>Job Name:</strong> ${JOB_NAME}</li>
                        <li><strong>Build URL:</strong> <a href="${BUILD_URL}">${BUILD_URL}</a></li>
                    </ul>
                    
                    <h3>Test Configuration:</h3>
                    <ul>
                        <li><strong>Device:</strong> ${params.DEVICE_NAME}</li>
                        <li><strong>UDID:</strong> ${params.UDID}</li>
                        <li><strong>Test Suite:</strong> ${params.TEST_SUITE}</li>
                        <li><strong>Platform:</strong> Android</li>
                    </ul>
                    
                    <h3>📊 Reports Available:</h3>
                    <ul>
                        <li><a href="${BUILD_URL}TestNG_HTML_Report/">TestNG HTML Report</a></li>
                        <li><a href="${BUILD_URL}testReport/">Test Results</a></li>
                        <li><a href="${BUILD_URL}artifact/">Build Artifacts</a></li>
                    </ul>
                    
                    <p><strong>Note:</strong> All tests passed successfully. The Appium server was managed programmatically by the framework.</p>
                    
                    <hr>
                    <p><em>This is an automated notification from Jenkins CI/CD Pipeline</em></p>
                    """,
                    mimeType: 'text/html',
                    attachmentsPattern: 'test-output/emailable-report.html'
                )
            }
        }
        
        failure {
            emailext(
                to: 'shaiz.akber@venturedive.com',
                subject: "❌ Appium Test Suite FAILED - Build #${BUILD_NUMBER}",
                body: """
                <h2>❌ Appium Test Execution Failed!</h2>
                
                <h3>Build Information:</h3>
                <ul>
                    <li><strong>Build Number:</strong> ${BUILD_NUMBER}</li>
                    <li><strong>Build Timestamp:</strong> ${BUILD_TIMESTAMP}</li>
                    <li><strong>Job Name:</strong> ${JOB_NAME}</li>
                    <li><strong>Build URL:</strong> <a href="${BUILD_URL}">${BUILD_URL}</a></li>
                </ul>
                
                <h3>Test Configuration:</h3>
                <ul>
                    <li><strong>Device:</strong> ${params.DEVICE_NAME}</li>
                    <li><strong>UDID:</strong> ${params.UDID}</li>
                    <li><strong>Test Suite:</strong> ${params.TEST_SUITE}</li>
                    <li><strong>Platform:</strong> Android</li>
                </ul>
                
                <h3>🔍 Troubleshooting:</h3>
                <ul>
                    <li><a href="${BUILD_URL}console">View Console Output</a></li>
                    <li><a href="${BUILD_URL}testReport/">View Test Results</a></li>
                    <li><a href="${BUILD_URL}artifact/">Download Artifacts</a></li>
                </ul>
                
                <p><strong>Action Required:</strong> Please check the console output and test reports to identify the root cause.</p>
                
                <hr>
                <p><em>This is an automated notification from Jenkins CI/CD Pipeline</em></p>
                """,
                mimeType: 'text/html',
                attachmentsPattern: 'logs/*.log'
            )
        }
        
        unstable {
            emailext(
                to: 'shaiz.akber@venturedive.com',
                subject: "⚠️ Appium Test Suite UNSTABLE - Build #${BUILD_NUMBER}",
                body: """
                <h2>⚠️ Appium Test Execution Completed with Issues!</h2>
                
                <h3>Build Information:</h3>
                <ul>
                    <li><strong>Build Number:</strong> ${BUILD_NUMBER}</li>
                    <li><strong>Build Timestamp:</strong> ${BUILD_TIMESTAMP}</li>
                    <li><strong>Job Name:</strong> ${JOB_NAME}</li>
                    <li><strong>Build URL:</strong> <a href="${BUILD_URL}">${BUILD_URL}</a></li>
                </ul>
                
                <h3>Test Configuration:</h3>
                <ul>
                    <li><strong>Device:</strong> ${params.DEVICE_NAME}</li>
                    <li><strong>UDID:</strong> ${params.UDID}</li>
                    <li><strong>Test Suite:</strong> ${params.TEST_SUITE}</li>
                    <li><strong>Platform:</strong> Android</li>
                </ul>
                
                <h3>📊 Reports Available:</h3>
                <ul>
                    <li><a href="${BUILD_URL}TestNG_HTML_Report/">TestNG HTML Report</a></li>
                    <li><a href="${BUILD_URL}testReport/">Detailed Test Results</a></li>
                    <li><a href="${BUILD_URL}artifact/">Build Artifacts</a></li>
                </ul>
                
                <p><strong>Status:</strong> Some tests may have failed or been skipped. Please review the detailed reports.</p>
                
                <hr>
                <p><em>This is an automated notification from Jenkins CI/CD Pipeline</em></p>
                """,
                mimeType: 'text/html',
                attachmentsPattern: 'test-output/emailable-report.html, logs/*.log'
            )
        }
    }
} 