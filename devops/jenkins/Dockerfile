FROM jenkins/jenkins:lts-jdk17

# Skip initial setup wizard
ENV JAVA_OPTS="-Djenkins.install.runSetupWizard=false"

# Tell Jenkins where to find the JCasC config
ENV CASC_JENKINS_CONFIG="/var/jenkins_home/casc.yaml"

# Install essential plugins
RUN jenkins-plugin-cli --plugins configuration-as-code job-dsl git workflow-aggregator docker-workflow sonar nodejs maven-plugin docker-plugin
