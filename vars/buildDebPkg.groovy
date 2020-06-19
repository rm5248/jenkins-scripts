def call( String[] distros = [], String[] arches = [] ){
	pipeline {
		agent { label 'master' }

		stages {
			stage('MainBuild'){
				matrix {
					axes {
						axis {
							name 'DISTRO'
							values distros
						}
						axis {
							name 'ARCH'
							values arches
						}
					} // axes

					stages{
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
						stage("Build-${ARCH}-${DISTRO}"){
							steps{
								if( DISTRO == "jessie" ){
								}else if( DISTRO == "stretch" ){
								}else if( DISTRO == "buster" ){
									configFileProvider([configFile(fileId: '42dd2363-51ed-4972-a382-f25ddbe11b3a', targetLocation: 'hookdir/D21-nightly-buster')]){
										buildDebPkg_fn( ARCH, DISTRO )
									}
								}
							}
						}
					} //stages
				} //matrix
			} //stage
		} //stages
	} //pipeline
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
