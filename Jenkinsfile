import org.folio.eureka.EurekaImage
import org.jenkinsci.plugins.workflow.libs.Library

@Library('pipelines-shared-library') _

node('jenkins-agent-java17-bigmem') {
  stage('Build Docker Image') {
    dir('mgr-tenant-entitlements') {
      EurekaImage image = new EurekaImage(this)
      image.setModuleName('mod-users-keycloak')
      image.makeImage()
    }
  }
}

buildMvn {
  publishModDescriptor = true
  mvnDeploy = true
  buildNode = 'jenkins-agent-java17-bigmem'
}
