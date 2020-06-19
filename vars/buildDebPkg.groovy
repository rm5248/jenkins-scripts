def call( String arch, String distro ){
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
					if( distro == "jessie" ){
					}else if( distro == "stretch" ){
					}else if( distro == "buster" ){
						configFileProvider([configFile(fileId: '42dd2363-51ed-4972-a382-f25ddbe11b3a', targetLocation: 'hookdir/D21-nightly-buster')]){
							buildDebPkg_fn( arch, distro )
						}
					}
			} //stage
	}
/*
	pipeline {
		agent { label 'master' }

		stages {
			stage('Clean'){
				steps{
					cleanWs()
				}
			}
			stage('Checkout'){
				steps{
					fileOperations([folderCreateOperation('source')])
					dir('source'){
						checkout scm
					}
				}
			}
			stage("Build-${arch}-${distro}"){
				steps{
					if( distro == "jessie" ){
					}else if( distro == "stretch" ){
					}else if( distro == "buster" ){
						configFileProvider([configFile(fileId: '42dd2363-51ed-4972-a382-f25ddbe11b3a', targetLocation: 'hookdir/D21-nightly-buster')]){
							buildDebPkg_fn( ARCH, DISTRO )
						}
					}
				}
			} //stage

		} //stages
	} //pipeline
*/
}

void buildDebPkg_fn(String arch, String distro){
	debianPbuilder additionalBuildResults: '', 
			architecture: arch, 
			components: '', 
			distribution: distro, 
			keyring: '', 
			mirrorSite: 'https://deb.debian.net/debian', 
			pristineTarName: ''
}

void foo_fn(String str){
}
