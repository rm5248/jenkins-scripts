def call(){
	def distributions = [ "bullseye", "bookworm" ]

	for(dist in distributions){
		buildDebPkg( "amd64", "${dist}" )
		if( env.BRANCH_NAME == 'master' ){
			buildDebPkg( "arm64", "${dist}" )
			buildDebPkg( "armhf", "${dist}" )
			buildDebPkg( "i386", "${dist}" )
		}
	}
}
