#!groovy
@Library('amf-jenkins-library') _

pipeline {
  agent {
    dockerfile {
      filename 'Dockerfile'
      registryCredentialsId 'dockerhub-pro-credentials'
    }
  }
  environment {
    NEXUS = credentials('exchange-nexus')
    GITHUB_ORG = 'aml-org'
    GITHUB_REPO = 'amf-core'
  }
  stages {
    stage('Test') {
      steps {
        wrap([$class: 'AnsiColorBuildWrapper', 'colorMapName': 'XTerm']) {
          sh 'sbt -mem 4096 -Dfile.encoding=UTF-8 clean coverage test coverageReport'
        }
      }
    }
    stage('Coverage') {
      when {
        branch 'develop'
      }
      steps {
        wrap([$class: 'AnsiColorBuildWrapper', 'colorMapName': 'XTerm']) {
          withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'sonarqube-official', passwordVariable: 'SONAR_SERVER_TOKEN', usernameVariable: 'SONAR_SERVER_URL']]) {
            sh 'sbt -Dsonar.host.url=${SONAR_SERVER_URL} sonarScan'
          }
        }
      }
    }
    stage('Publish') {
      when {
        anyOf {
          branch 'master'
          branch 'develop'
          branch 'support/*'
        }
      }
      steps {
        wrap([$class: 'AnsiColorBuildWrapper', 'colorMapName': 'XTerm']) {
          sh '''
              echo "about to publish in sbt"
              sbt publish
              echo "sbt publishing successful"
          '''
        }
      }
    }
    stage('Triggers') {
      when {
        anyOf {
          branch 'develop'
        }
      }
      steps {
        script {
          echo "Triggering amf-aml on develop branch"
          build job: 'application/AMF/amf-aml/develop', wait: false
        }
      }
    }
    stage('Tag version') {
      when {
        anyOf {
          branch 'master'
          branch 'support/*'
        }
      }
      steps {
        withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'github-salt', passwordVariable: 'GITHUB_PASS', usernameVariable: 'GITHUB_USER']]) {
          script {
            def version = sbtArtifactVersion("coreJVM")
            tagCommitToGithub(version)
          }
        }
      }
    }
  }
}
