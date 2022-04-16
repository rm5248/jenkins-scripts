def call(){
	buildDebPkg( "amd64", "bullseye" )
	if( env.BRANCH_NAME == 'master' ){
		buildDebPkg( "armhf", "bullseye" )
		buildDebPkg( "i386", "bullseye" )
	}
}
