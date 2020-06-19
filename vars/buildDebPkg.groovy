def call( String arch, String distro, String repoHook = "" ){
	node {
		stage('Clean'){
				cleanWs()
		}
		stage('Checkout'){
				fileOperations([folderCreateOperation('source')])
				dir('source'){
					checkout scm
				}
		}
		stage("Build-${arch}-${distro}"){
			if( repoHook.length() > 0 ){
				configFileProvider([configFile(fileId: ${repoHook}, targetLocation: 'hookdir/D21-repo-hook')]){
					buildDebPkg_fn( arch, distro )
				}
			}else{
				buildDebPkg_fn( arch, distro )
			}
		} //stage
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

