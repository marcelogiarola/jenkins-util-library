#!/usr/bin/env groovy
package com.redhat

import com.redhat.Version
import java.util.LinkedHashMap
import java.util.List

def isDebugEnabled

def enableDebug(Boolean isDebugOnOrOff){
	isDebugEnabled = isDebugOnOrOff
}

def init(){
	echo "Pipeline Version: ${Version.PIPELINE_VERSION}"
}

def createBuild(String projectName, String buildName, String appImageStream, String appBinaryBuildPath, Boolean isFromFile){
	echo "Executando util.createBuild(${projectName}, ${buildName}, ${appImageStream}, ${appBinaryBuildPath}, ${isFromFile})"
	/*if (isDebugEnabled)
		echo "START createBuild"

	openshift.withCluster() {
		openshift.withProject(projectName) {
			openshift.newBuild("--name=${buildName} --image-stream=${appImageStream} -l app=${buildName}", "--binary=true")
			buildApp(projectName, buildName, appBinaryBuildPath, isFromFile)
		}
	}

	if (isDebugEnabled)
		echo "FINISH createBuild"*/
}

def buildApp(String projectName, String buildName, String appBinaryBuildPath, Boolean isFromFile){ 
	if (isDebugEnabled)
		echo "START buildApp"

	openshift.withCluster() {
		openshift.withProject(projectName) {
			if(isFromFile){
				echo "Build started from file."
				openshift.selector("bc", buildName).startBuild("--from-file=${appBinaryBuildPath}", "--wait=true").logs('-f')
			} else {
				echo "Build started from dir."
				openshift.selector("bc", buildName).startBuild("--from-dir=${appBinaryBuildPath}", "--wait=true").logs('-f')
			}
		}
	}

	if (isDebugEnabled)
		echo "FINISH buildApp"
}

def newApp(String projectName, String envName, String appName, String appTemplateName, String readinessUrl, String livenessUrl, String version){		
	if (isDebugEnabled)
		echo "START newApp"
	
	openshift.withCluster() {
		openshift.withProject(projectName) {
			def param = parametersToTemplate(appName, projectName, readinessUrl, livenessUrl, envName, version)
			echo "Parâmetros utilizados para o new-app: ${param}"
			if ("".equals(appTemplateName)) {
				openshift.newApp("--image-stream=${appName}", param)
			} else {
				openshift.newApp("--template=${appTemplateName}", param)
			}				
		}
	}

	if (isDebugEnabled)
		echo "FINISH newApp"
}

private def parametersToTemplate(String appName, String projectName, String readinessUrl, String livenessUrl, String envName, String version){
	def parameters = "-p=APP_NAME=${appName}"
					.concat(" -p=IMAGE_TAG=${version}")
					.concat(" -p=PROJECT_NAME=${projectName}")
					.concat(" -p=READINESS_URL=${readinessUrl}")
					.concat(" -p=LIVENESS_URL=${livenessUrl}")

	if (envName == "prod"){
		parameters = parameters.concat(" -p=HOSTNAME=${appName}.paas.celepar.parana")
	} else {
		parameters = parameters.concat(" -p=HOSTNAME=${appName}-${envName}.paas.celepar.parana")
	}

	return parameters
}

def verifyDeployment(String projectName, String appName) { 
	if (isDebugEnabled)
		echo "START verifyDeployment"

	openshift.withCluster() {
		openshift.withProject(projectName) {
			timeout(10) {
				def dc = openshift.selector('dc', "${appName}")
				dc.rollout().cancel()
				//waitForDeploy(dc)
				sleep(10)
				dc.rollout().latest()
				waitForDeploy(dc)
			}
		}
	}
	
	if (isDebugEnabled)
		echo "FINISH verifyDeployment"
}

private def waitForDeploy(Object dc){
	echo "Waiting for deployment..."
	dc.rollout().status()
}

def defineEnvVariables(String projectName, String appName, Map envvars){
	if (isDebugEnabled)
		echo "START defineEnvVariables"
	
	if (!envvars.isEmpty()){
		openshift.withCluster() {
			openshift.withProject(projectName){
				def envvarsString = ""
				envvars.each{
					entry -> envvarsString+="${entry.key}=${entry.value.trim()} "
				}
				echo "Env vars: ${envvarsString}"
				openshift.set("env dc/${appName} " + envvarsString)
			}
		}
	}
	
	if (isDebugEnabled)
		echo "FINISH defineEnvVariables"
}

def addSecretAsEnvvar(String projectName, String appName, String secretName){
	if (isDebugEnabled)
		echo "START addSecretAsEnvvar"
	
	openshift.withCluster() {
		openshift.withProject(projectName){
			openshift.set("env dc/${appName} --from=secret/${secretName} --overwrite")
		}
	}
	
	if (isDebugEnabled)
		echo "FINISH addSecretAsEnvvar"
}

def addConfigMapAsEnvvar(String projectName, String appName, String configmapName){
	if (isDebugEnabled)
		echo "START addConfigMapAsEnvvar"
	
	openshift.withCluster() {
		openshift.withProject(projectName){
			openshift.set("env dc/${appName} --from=configmap/${configmapName} --overwrite")
		}
	}
	
	if (isDebugEnabled)
		echo "FINISH addConfigMapAsEnvvar"
}

def stringToMap(String mapAsString){
	if("".equals(mapAsString) || "[]".equals(mapAsString)) return new LinkedHashMap();
	return mapAsString[1..-2]
			.split(', ')
			.collectEntries { entry ->
	            def pair = entry.split(':')
	            [(pair.first()): pair.last()]
	        }
}

def createOrReplace(String projectName, String resourceType, String resourceName, String filePath){
	if (isDebugEnabled)
		echo "START createOrReplace"
	
	openshift.withCluster() {
		openshift.withProject(projectName){
			if(openshift.selector(resourceType, resourceName).exists()){
				openshift.replace('-f', filePath)
			} else {
				openshift.create('-f', filePath)
			}
		}
	}
	
	if (isDebugEnabled)
		echo "FINISH createOrReplace"
}

def changeTriggersToManualMode(String projectName, String appName){
	if (isDebugEnabled)
		echo "START changeTriggersToManualMode"

	openshift.withCluster() {
		openshift.withProject(projectName){
			openshift.set("triggers dc/${appName} --manual")
		}
	}

	if (isDebugEnabled)
		echo "FINISH changeTriggersToManualMode"
}

def changeTriggersToAutomaticMode(String projectName, String appName){
	if (isDebugEnabled)
		echo "START changeTriggersToAutomaticMode"

	openshift.withCluster() {
		openshift.withProject(projectName){
			openshift.set("triggers dc/${appName} --auto")
		}
	}

	if (isDebugEnabled)
		echo "FINISH changeTriggersToAutomaticMode"
}

def removeAllEnvvars(String projectName, String appName){
	if (isDebugEnabled)
		echo "START removeAllEnvvars"

	openshift.withCluster() {
		openshift.withProject(projectName){
			def dc = openshift.selector('dc', appName).object()
			def envvars = dc.spec.template.spec.containers[0].env
			if(envvars != null && !envvars.isEmpty()){
				def cmd = ""			
				for(envvar in envvars){
					cmd += envvar.name+"- "
				}
				echo("Removing envvars: ${cmd}")
				openshift.set("env dc/${appName} " + cmd)
			}
		}
	}

	if (isDebugEnabled)
		echo "FINISH removeAllEnvvars"
}

def createAndProcess(String projectName, String templateName, List parameters){
	if (isDebugEnabled)
		echo "START createAndProcess"

	def parametersAsString = ""
	for(String parameter : parameters){
		parametersAsString = parametersAsString.concat(parameter).concat(" ")
	}
	
	openshift.withCluster() {
		openshift.withProject(projectName) {
			openshift.create(
				openshift.process(templateName, parametersAsString)
			)
		}
	}

	if (isDebugEnabled)
		echo "FINISH createAndProcess"
}

return this
