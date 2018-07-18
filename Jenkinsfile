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

    stage("Promote to PRD") {
      when {
        expression {
          return env.BRANCH_NAME == 'master' || ( tag != '' & env.BRANCH_NAME == tag);
        }
        expression {
          return tag != '';
        }
      }
      steps {
        script {
          env.DEPLOY_TO_PRD = input message: 'User input required',
            submitter: 'eubankj,vermar,andricd',
            parameters: [choice(name: 'Deploy to PRD Environment', choices: 'no\nyes', description: 'Choose "yes" if you want to deploy the PRD tables')]
        }
      }
    }

    stage('Deploy To PRD') {
      when {
        environment name: 'DEPLOY_TO_PRD', value: 'yes'
        expression {
           return env.BRANCH_NAME == 'master' || ( tag != '' & env.BRANCH_NAME == tag);
        }
        expression {
          return tag != '';
        }
      }

      steps {
        slackSend (color: '#FFFF00', message: "Starting to deploy kf-key-management to PRD: Branch '${env.BRANCH} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
        sh '''
          kf-key-management-config/ci-scripts/deploy/deploy.sh prd
        '''
        slackSend (color: '#00FF00', message: ":smile: kf-key-management Deployed to PRD: Branch '${env.BRANCH} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
      }
      post {
        failure {
          slackSend (color: '#ff0000', message: ":frowning: kf-key-management Deployed to PRD` Failed: Branch '${env.BRANCH} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
        }
      }
    }
  }
}