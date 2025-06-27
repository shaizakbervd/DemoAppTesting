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
        ANDROID_HOME = 'C:\\Android\\sdk'
        JAVA_HOME = 'C:\\Program Files\\Java\\jdk-21'
        PATH = "${ANDROID_HOME}\\platform-tools;${ANDROID_HOME}\\tools;${ANDROID_HOME}\\emulator;${JAVA_HOME}\\bin;${PATH}"
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
                        bat '''
                            echo "Checking Java version..."
                            java -version
                            
                            echo "Checking Maven version..."
                            mvn -version
                            
                            echo "Checking Node.js and Appium..."
                            where node || echo "Node.js not found in PATH"
                            where npm || echo "npm not found in PATH"
                            
                            echo "Installing/Updating Appium if needed..."
                            npm list -g appium || npm install -g appium@2.0.0
                            npm list -g @appium/doctor || npm install -g @appium/doctor
                            
                            echo "Verifying Appium installation..."
                            appium --version
                        '''
                    }
                }
                
                stage('Verify Android Setup') {
                    steps {
                        bat '''
                            echo "Checking Android SDK installation..."
                            if not exist "%ANDROID_HOME%" (
                                echo "ERROR: Android SDK not found at %ANDROID_HOME%"
                                echo "Please install Android SDK or update ANDROID_HOME path"
                                exit /b 1
                            )
                            
                            echo "Checking ADB..."
                            where adb || echo "ADB not found in PATH"
                            
                            echo "Listing available devices..."
                            adb devices
                            
                            echo "Checking emulator..."
                            if exist "%ANDROID_HOME%\\emulator\\emulator.exe" (
                                "%ANDROID_HOME%\\emulator\\emulator.exe" -list-avds
                            ) else (
                                echo "Emulator not found"
                            )
                        '''
                    }
                }
            }
        }
        
        stage('Prepare Test Environment') {
            steps {
                bat '''
                    echo "Cleaning previous test artifacts..."
                    if exist "target" rmdir /s /q "target"
                    if exist "test-output" rmdir /s /q "test-output"
                    if exist "logs\\*.log" del /q "logs\\*.log"
                    
                    echo "Creating necessary directories..."
                    if not exist "logs" mkdir "logs"
                    if not exist "Videos" mkdir "Videos"
                    if not exist "Screenshots" mkdir "Screenshots"
                    
                    echo "Checking APK file..."
                    if exist "src\\main\\resources\\App\\draganddrop.apk" (
                        echo "APK file found"
                        dir "src\\main\\resources\\App\\draganddrop.apk"
                    ) else (
                        echo "ERROR: APK file not found!"
                        exit /b 1
                    )
                '''
            }
        }
        
        stage('Start Android Emulator') {
            when {
                expression { params.UDID.startsWith('emulator-') }
            }
            steps {
                script {
                    bat '''
                        echo "Starting Android emulator..."
                        
                        rem Check if emulator is already running
                        adb devices | findstr "%UDID%" >nul
                        if %errorlevel% equ 0 (
                            echo "Emulator %UDID% is already running"
                        ) else (
                            echo "Starting emulator %DEVICE_NAME%..."
                            
                            rem Extract port number from UDID (emulator-5554 -> 5554)
                            for /f "tokens=2 delims=-" %%a in ("%UDID%") do set EMULATOR_PORT=%%a
                            
                            rem Start emulator in background using PowerShell
                            powershell -Command "Start-Process -FilePath '%ANDROID_HOME%\\emulator\\emulator.exe' -ArgumentList '-avd', '%DEVICE_NAME%', '-port', '%EMULATOR_PORT%', '-no-audio', '-no-window', '-gpu', 'swiftshader_indirect' -WindowStyle Hidden"
                            
                            rem Wait for emulator to be ready
                            echo "Waiting for emulator to start..."
                            set TIMEOUT=60
                            :wait_loop
                            if %TIMEOUT% leq 0 (
                                echo "ERROR: Emulator failed to start within 5 minutes"
                                exit /b 1
                            )
                            timeout/t 5 >nul
                            adb devices | findstr "%UDID%" >nul
                            if %errorlevel% neq 0 (
                                set /a TIMEOUT=%TIMEOUT%-1
                                goto wait_loop
                            )
                            
                            echo "Emulator is connected"
                            
                            rem Wait for emulator to be fully booted
                            echo "Waiting for emulator to be ready..."
                            adb -s %UDID% wait-for-device
                            adb -s %UDID% shell "while [[ -z $(getprop sys.boot_completed) ]]; do sleep 1; done"
                        )
                        
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
                        bat '''
                            echo "Compiling the project..."
                            mvn clean compile -DskipTests=true
                            
                            echo "Running Appium tests..."
                            echo "Note: Appium server will be started programmatically by the framework"
                            
                            rem Update TestNG XML with current device parameters (Windows version)
                            powershell -Command "(Get-Content testng.xml) -replace 'value=\"pixel_7\"', 'value=\"%DEVICE_NAME%\"' | Set-Content testng.xml"
                            powershell -Command "(Get-Content testng.xml) -replace 'value=\"emulator-5554\"', 'value=\"%UDID%\"' | Set-Content testng.xml"
                            
                            echo "Updated testng.xml with device: %DEVICE_NAME%, UDID: %UDID%"
                            type testng.xml
                            
                            rem Run tests with Maven Surefire plugin
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
                bat '''
                    echo "Generating test reports..."
                    
                    rem Archive TestNG reports
                    if exist "test-output" (
                        echo "TestNG reports found"
                        dir "test-output"
                    )
                    
                    rem Archive Surefire reports
                    if exist "target\\surefire-reports" (
                        echo "Surefire reports found"
                        dir "target\\surefire-reports"
                    )
                    
                    rem Archive logs
                    if exist "logs" (
                        echo "Log files found"
                        dir "logs"
                    )
                    
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
                    bat '''
                        echo "Cleaning up emulator..."
                        adb -s %UDID% emu kill || echo "Failed to kill emulator via ADB"
                        taskkill /f /im emulator.exe || echo "No emulator process found"
                        taskkill /f /im qemu-system*.exe || echo "No qemu process found"
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
                        <li><strong>Platform:</strong> Android on Windows</li>
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
                    <li><strong>Platform:</strong> Android on Windows</li>
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
                    <li><strong>Platform:</strong> Android on Windows</li>
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