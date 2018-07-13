#!groovy
pipeline {
  agent { label 'docker-slave' }

  stages {
    stage('Get Code') {
      steps {
        deleteDir()
        checkout scm
      }
    }
    stage('GetOpsScripts') {
      steps {
        slackSend (color: '#ddaa00', message: ":construction_worker: kf-key-management GETTING SCRIPTS:")
        sh '''
        git clone git@github.com:kids-first/kf-key-management-config.git
        '''
      }
    }

    stage('Build') {
      steps {
        sh '''
        kf-key-management-config/ci-scripts/build/build.sh
        '''
      }
    }
    
    stage('Deploy to Dev') {
      steps {
        sh '''
        kf-key-management-config/ci-scripts/deploy/deploy.sh dev
        '''
      }
    }

    stage('Deploy to QA') {
      when {
        expression {
          return env.BRANCH_NAME == 'master';
        }
      }
      steps {
        sh '''
        kf-key-management-config/ci-scripts/deploy/deploy.sh qa
        '''
      }
    }
  }
}