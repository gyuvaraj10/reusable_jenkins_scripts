stage('Reporting SDK Installation') {
    build job:'Reporting SDK Installation'
}
stage('System Tests') {
    build job:'run_sit_web_tests', propagate:false
}
stage('System Test Stage Gate') {
    def buildStatus = build job: 'CollectTestExecutionMetrics', propagate: false, parameters: [string(name: 'VIEW_NAME', value: 'Scot Logic System Tests'), string(name: 'PASS_PERCENTAGE', value: '80')]
    if(hudson.model.Result.SUCCESS.equals(buildStatus.result)){
        currentBuild.result = 'SUCCESS'
    } else {
           String url = "${BUILD_URL}";
           String body = "<html><header><h1>XCUIT Test Execution Status</h1></header><body><a href=\""+url+"/input/\"/>"+buildStatus.description+"</a></body></html>"
           mail body: body, mimeType:'text/html', subject: 'XCUIT System tests', to: 'yuvaraj.gunisetti@xxxxx.co.uk'
           String command = "curl \'https://api.twilio.com/2010-04-01/Accounts/xxxxxxxx/Messages.json\' -X POST "+
"--data-urlencode 'To=+447340352378' "+
"--data-urlencode 'From=+441293344601' "+
"--data-urlencode 'Body="+buildStatus.description+"' "+
"-u xxxxx:xxxxxx"
        //sh label: '', returnStdout: true, script: command
        def submitter = input id: 'SystemTestResult', message: buildStatus.description +'.Do you want to proceed with Smoke test?', submitter: 'admin', submitterParameter: 'message'
        echo submitter + ' has approved the pipeline to proceed to smoke testing'
    }
}
stage('Atom\'s Smoke Tests') {
    build job:'run_sit_mobile_simulator_tests', propagate:false
}
stage('Smoke Test Stage Gate') {
    def buildStatus = build job: 'CollectTestExecutionMetrics', propagate: false, parameters: [string(name: 'VIEW_NAME', value: 'Smoke Tests'), string(name: 'PASS_PERCENTAGE', value: '80')]
    if(hudson.model.Result.SUCCESS.equals(buildStatus.result)){
        currentBuild.result = 'SUCCESS'
    } else {
           String url = "${BUILD_URL}";
           String body = "<html><header><h1>Smoke Test Execution Status</h1></header><body><a href=\""+url+"/input/\"/>"+buildStatus.description+"</a></body></html>"
           mail body: body, mimeType:'text/html', subject: 'Smoke Test Execution Results', to: 'yuvaraj.gunisetti@xxxxxxxx.co.uk'
           String command = "curl \'https://api.twilio.com/2010-04-01/Accounts/xxxxxxx/Messages.json\' -X POST "+
"--data-urlencode 'To=+447340352378' "+
"--data-urlencode 'From=+441293344601' "+
"--data-urlencode 'Body="+buildStatus.description+"' "+
"-u xxxxx:xxxxxx"
        //sh label: '', returnStdout: true, script: command
        def submitter = input id: 'SystemTestResult', message: buildStatus.description +'.Do you want to proceed with System Integration test?', submitter: 'admin', submitterParameter: 'message'
        echo submitter + ' has approved the pipeline to proceed to system integration testing'
    }
}
stage('Atom\'s System Integration Tests') {
    build job:'Atom_System_Integration_Tests'
}
stage('System Integration Test Stage Gate') {
    def buildStatus = build job: 'CollectTestExecutionMetrics', propagate: false, parameters: [string(name: 'VIEW_NAME', value: 'System Integration Tests'), string(name: 'PASS_PERCENTAGE', value: '80')]
    if(hudson.model.Result.SUCCESS.equals(buildStatus.result)){
        currentBuild.result = 'SUCCESS'
    } else {
           String url = "${BUILD_URL}";
           String body = "<html><header><h1>System Integration Test Execution Status</h1></header><body><a href=\""+url+"/input/\"/>"+buildStatus.description+"</a></body></html>"
           mail body: body, mimeType:'text/html', subject: 'System Integration Test Execution Results', to: 'yuvaraj.gunisetti@atombank.co.uk'
           String command = "curl \'https://api.twilio.com/2010-04-01/Accounts/xxxxx/Messages.json\' -X POST "+
"--data-urlencode 'To=+447340352378' "+
"--data-urlencode 'From=+441293344601' "+
"--data-urlencode 'Body="+buildStatus.description+"' "+
"-u xxxxx:xxxxxx"
        //sh label: '', returnStdout: true, script: command
        def submitter = input id: 'SystemTestResult', message: buildStatus.description +'.Do you wish to provide the sign off for testing?', submitter: 'admin', submitterParameter: 'message'
        echo submitter + ' has signed off testing'
    }
}
