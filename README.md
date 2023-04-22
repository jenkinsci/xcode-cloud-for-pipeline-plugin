# xcode-cloud-for-jenkins

## Installation

1. Install the plugin from the Jenkins Update Center
2. Set a target branch for your project in the Xcode Cloud configuration
3. Add a build step to your project to trigger a build on Xcode Cloud
4. Make sure GIT_USERNAME and GIT_PASSWORD are set as environment variables in Jenkins and have matching target branch
   name on both Jenkins and Xcode Cloud

```
#Example Jenkinsfile

pipeline {
    agent any
    environment {
        GIT_USERNAME="lucas-im"
        GIT_PASSWORD="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"
    }
    stages{
        stage('Modify Something') {
            steps {
                sh """
                    echo "Hello World" > something
                """
            }
        }
        stage('Push to Xcode Cloud') {
            steps {
                step([$class: 'XcodeCloudForPipelineBuilder', branchName: '__jenkins_only__'])
            }
        }
    }
}

```

