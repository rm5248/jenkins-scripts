def buildingTag = env.TAG_NAME != null

def call( String arch, String distro, String repoHook = "" ){
	node {
		stage('Clean'){
				cleanWs()
		}
		stage('Checkout'){
				fileOperations([folderCreateOperation('source')])
				dir('source'){
					def scmVars = checkout scm
					env.GIT_COMMIT = scmVars.GIT_COMMIT
				}
		}
		stage("Build-${arch}-${distro}"){
			if( repoHook.length() > 0 ){
				configFileProvider([configFile(fileId: "${repoHook}", targetLocation: 'hookdir/D21-repo-hook')]){
					buildDebPkg_fn( arch, distro, buildingTag )
				}
			}else{
				buildDebPkg_fn( arch, distro, buildingTag )
			}
		} //stage

		stage('Upload to nightly repo'){
			if( env.BRANCH_NAME == 'master' ){
				rtUpload (
					serverId: 'rm5248-jfrog',
					specPath: 'artifactory-spec-debian-pbuilder/debian-pbuilder.spec'
				)

				rtBuildInfo (
					// Optional - Maximum builds to keep in Artifactory.
					maxBuilds: 1,
					deleteBuildArtifacts: true,
				)

				rtPublishBuildInfo (
					serverId: 'rm5248-jfrog'
				)

			}
		}

		stage('Upload to release repo'){
			if( env.TAG_NAME != null ){
				rtUpload (
					serverId: 'rm5248-jfrog',
					specPath: 'artifactory-spec-debian-pbuilder/debian-pbuilder.spec'
				)

				rtBuildInfo (
				)

				rtPublishBuildInfo (
					serverId: 'rm5248-jfrog'
				)

			}
		}
	}
}

void buildDebPkg_fn(String arch, String distro, boolean isTag){
	debianPbuilder additionalBuildResults: '', 
			architecture: arch, 
			components: '', 
			distribution: distro, 
			keyring: '', 
			mirrorSite: 'http://deb.debian.org/debian', 
			pristineTarName: '',
			buildAsTag: isTag,
			generateArtifactorySpecFile: true,
			artifactoryRepoName: isTag ? 'test-repo-debian-release' : 'test-repo-debian-local'
}
