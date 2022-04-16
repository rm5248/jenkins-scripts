def call( String arch, String distro, String repoHook = "" ){
	def buildingTag = env.TAG_NAME != null
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
			writeAptRepo(distro, buildingTag)
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
					specPath: 'artifactory-spec-debian-pbuilder/debian-pbuilder.spec',
					buildName: "${JOB_NAME}-${arch}-${distro}",
				)

				rtBuildInfo (
					// Optional - Maximum builds to keep in Artifactory.
					maxBuilds: 1,
					deleteBuildArtifacts: true,
					buildName: "${JOB_NAME}-${arch}-${distro}",
				)

				rtPublishBuildInfo (
					serverId: 'rm5248-jfrog',
					buildName: "${JOB_NAME}-${arch}-${distro}",
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
			artifactoryRepoName: isTag ? 'test-repo-debian-release' : 'test-repo-debian-local',
			extraPackages: 'ca-certificates'
}

void writeAptRepo(String distro, boolean isTag){
	File dir = new File("${WORKSPACE}/hookdir");
	dir.mkdirs();
	File f = new File("${WORKSPACE}/hookdir/D21-repos");
	f.write "";
	String key = 
"""-----BEGIN PGP PUBLIC KEY BLOCK-----
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
0FW0Eg==
=A5qz
-----END PGP PUBLIC KEY BLOCK-----"""

	f.append "echo \"${key}\" > /etc/apt/trusted.gpg.d/rm5248.asc\n"
	if( !isTag ){
		f.append "echo \"deb https://rm5248.jfrog.io/artifactory/test-repo-debian-local ${distro} main\" > /etc/apt/sources.list.d/rm5248.list\n"
	}else{
		f.append "echo \"deb https://rm5248.jfrog.io/artifactory/test-repo-debian-release ${distro} main\" > /etc/apt/sources.list.d/rm5248.list\n"
	}
	f.append "apt-get update\n"
}
