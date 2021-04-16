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
	if (isDebugEnabled)
		echo "START createBuild with parameters projectName: '${projectName}', buildName: '${buildName}', appImageStream: '${appImageStream}', appBinaryBuildPath: '${appBinaryBuildPath}' and isFromFile: ${isFromFile}"

	openshift.withCluster() {
		openshift.withProject(projectName) {
			openshift.newBuild("--name=${buildName} --image-stream=${appImageStream} -l app=${buildName}", "--binary=true")
			internalBuildApp(buildName, appBinaryBuildPath, isFromFile)
		}
	}

	if (isDebugEnabled)
		echo "FINISH createBuild"
}

def buildApp(String projectName, String buildName, String appBinaryBuildPath, Boolean isFromFile){ 
	if (isDebugEnabled)
		echo "START buildApp with parameters projectName: '${projectName}', buildName: '${buildName}', appBinaryBuildPath: '${appBinaryBuildPath}' and isFromFile: ${isFromFile}"

	openshift.withCluster() {
		openshift.withProject(projectName) {
			internalBuildApp(buildName, appBinaryBuildPath, isFromFile)
		}
	}

	if (isDebugEnabled)
		echo "FINISH buildApp"
}

private def internalBuildApp(String buildName, String appBinaryBuildPath, Boolean isFromFile){ 
	if (isDebugEnabled)
	echo "START internalBuildApp with parameters buildName: '${buildName}', appBinaryBuildPath: '${appBinaryBuildPath}' and isFromFile: ${isFromFile}"

	if(isFromFile){
		echo "Build started from file."
		openshift.selector("bc", buildName).startBuild("--from-file=${appBinaryBuildPath}", "--wait=true").logs('-f')
	} else {
		echo "Build started from dir."
		openshift.selector("bc", buildName).startBuild("--from-dir=${appBinaryBuildPath}", "--wait=true").logs('-f')
	}

	if (isDebugEnabled)
		echo "FINISH internalBuildApp"
}

def newApp(String projectName, String envName, String appName, String appTemplateName, String readinessUrl, String livenessUrl, String version, String hostNameDomain){		
	if (isDebugEnabled)
		echo "START newApp with parameters projectName: '${projectName}', envName: '${envName}', appName: '${appName}', appTemplateName: '${appTemplateName}', readinessUrl: '${readinessUrl}', livenessUrl: '${livenessUrl}', version: '${version}' and hostNameDomain: '${hostNameDomain}'"
	
	openshift.withCluster() {
		openshift.withProject(projectName) {
			def param = parametersToTemplate(appName, readinessUrl, livenessUrl, envName, hostNameDomain)
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

private def parametersToTemplate(String appName, String readinessUrl, String livenessUrl, String envName, String hostNameDomain){
	def parameters = "-p=APPLICATION_NAME=${appName}"
					.concat(" -p=READINESS_PROBE=${readinessUrl}")
					.concat(" -p=LIVENESS_PROBE=${livenessUrl}")
					.concat(" --as-deployment-config=true")

	if (envName == "prod"){
		parameters = parameters.concat(" -p=APPLICATION_HOSTNAME=${appName}.${hostNameDomain}")
	} else {
		parameters = parameters.concat(" -p=APPLICATION_HOSTNAME=${appName}-${envName}.${hostNameDomain}")
	}

	return parameters
}

def verifyDeployment(String projectName, String appName) { 
	if (isDebugEnabled)
		echo "START verifyDeployment with parameters projectName: '${projectName}' and appName: '${appName}'"

	openshift.withCluster() {
		openshift.withProject(projectName) {
			timeout(10) {
				def dc = openshift.selector('dc', "${appName}")
				// dc.rollout().latest()
				dc.related('pods').untilEach(1) {
					return (it.object().status.phase == "Running")
				}
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
		echo "START defineEnvVariables with parameters projectName: '${projectName}', appName: '${appName}' and envvars: '${envvars}'"
	
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
		echo "START addSecretAsEnvvar with parameters projectName: '${projectName}', appName: '${appName}' and secretName: '${secretName}'"
	
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
		echo "START addConfigMapAsEnvvar with parameters projectName: '${projectName}', appName: '${appName}' and configmapName: '${configmapName}'"
	
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

def createOrReplace(String projectName, String resourceType, String resourceName, String filePath, String appName){
	if (isDebugEnabled)
		echo "START createOrReplace with parameters projectName: '${projectName}', resourceType: '${resourceType}', resourceName: '${resourceName}' and filePath: '${filePath}'"
	
	openshift.withCluster() {
		openshift.withProject(projectName){
			applyLabel = "${resourceType} ${resourceName} app=${appName}"
			if(openshift.selector(resourceType, resourceName).exists()){
				openshift.replace('-f', filePath)
				openshift.raw('label', applayLabel)
			} else {
				openshift.create('-f', filePath)
				openshift.raw('label', applayLabel)
			}
		}
	}
	
	if (isDebugEnabled)
		echo "FINISH createOrReplace"
}

def changeTriggersToManualMode(String projectName, String appName){
	if (isDebugEnabled)
		echo "START changeTriggersToManualMode with parameters projectName: '${projectName}' and appName: '${appName}'"

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
		echo "START changeTriggersToAutomaticMode with parameters projectName: '${projectName}' and appName: '${appName}'"

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
		echo "START removeAllEnvvars with parameters projectName: '${projectName}' and appName: '${appName}'"

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
		echo "START createAndProcess with parameters projectName: '${projectName}', templateName: '${templateName}' and parameters: '${parameters}'"

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
