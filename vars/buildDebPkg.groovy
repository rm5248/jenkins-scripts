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
					buildDebPkg_fn( arch, distro )
				}
			}else{
				buildDebPkg_fn( arch, distro )
			}
		} //stage

		stage("Add to repo if master"){
			if( env.BRANCH_NAME == "master" ){
				aptlyPublish includeSource: true, removeOldPackages: true, repositoryName: "nightly-${distro}"
			}
		}
	}
}

void buildDebPkg_fn(String arch, String distro){
	debianPbuilder additionalBuildResults: '', 
			architecture: arch, 
			components: '', 
			distribution: distro, 
			keyring: '', 
			mirrorSite: 'http://deb.debian.org/debian', 
			pristineTarName: ''
}

