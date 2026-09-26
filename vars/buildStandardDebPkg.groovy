def call(){
	def cfg = aptlyConfig()

	for(dist in cfg.distributions){
		for(arch in cfg.architectures){
			buildDebPkg( "${arch}", "${dist}" )
		}
	}

	publishDebPkg( cfg.distributions, cfg.architectures )
}
