def call( String arch, String distro ){
	def buildingTag = env.TAG_NAME != null
	def cfg = aptlyConfig()
	node( cfg.nodeLabel ){
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
			String repoDir = localAptRepo( cfg, distro )
			buildDebPkg_fn( arch, distro, buildingTag, repoDir )
		} //stage
		stage("Stash-${arch}-${distro}"){
			stashDebPkg( arch, distro )
		}
	}
}

void buildDebPkg_fn(String arch, String distro, boolean isTag, String repoDir){
	debianPbuilder additionalBuildResults: '', 
			architecture: arch, 
			components: '', 
			distribution: distro, 
			keyring: '', 
			mirrorSite: 'http://deb.debian.org/debian', 
			pristineTarName: '',
			buildAsTag: isTag,
			generateArtifactorySpecFile: true,
			extraPackages: 'ca-certificates',
			pbuilderType: 'PBuilder',
			binariesDir: 'binaries',
			bindMounts: repoDir,
			binariesSeparateFolders: true
}

/*
 * Add our local apt repository(master or releases) to the chroot so that
 * build dependencies may be installed from it.  The published repo is bind
 * mounted into the chroot, and a hook adds it to apt.
 *
 * pbuilder's OTHERMIRROR is not used, as it is only applied when the base
 * tarball is created or with --override-config.  The base tarball is shared
 * between all jobs, so the repo would leak into builds that don't want it.
 *
 * Returns the directory to bind mount, or an empty string if the repo has
 * not been published yet.
 */
String localAptRepo(Map cfg, String distro){
	String repoDir = "${aptlyConfig.publicDir(cfg)}/${cfg.dependencyChannel}/${distro}"
	if( sh(script: "test -f '${repoDir}/dists/${distro}/Release'", returnStatus: true) != 0 ){
		echo "No ${cfg.dependencyChannel} apt repo for ${distro} at ${repoDir}, building without it"
		return ''
	}

	// The repo is local and bind mounted from the host, so trust it rather
	// than needing the signing key in the chroot
	String hook = "#!/bin/sh\n"
	hook += "set -e\n"
	hook += "echo \"deb [trusted=yes] file://${repoDir} ${distro} main\" > /etc/apt/sources.list.d/local-${cfg.dependencyChannel}.list\n"
	hook += "apt-get update\n"

	writeFile file: 'hookdir/D20-local-apt-repo', text: hook
	return repoDir
}

/*
 * Stash the built packages so that they can be published to the apt repo
 * by publishDebPkg.  The plugin puts them in binaries/<distro>/<arch>.
 */
void stashDebPkg(String arch, String distro){
	String dir = "binaries/${distro}/${arch}"
	def patterns = []
	for( pattern in [ '*.deb', '*.udeb', '*.dsc', '*.tar.*', '*.diff.gz' ] ){
		patterns.add( "${dir}/${pattern}" )
	}
	String includes = patterns.join(',')
	stash name: "aptly-${distro}-${arch}", includes: includes, allowEmpty: true
}
