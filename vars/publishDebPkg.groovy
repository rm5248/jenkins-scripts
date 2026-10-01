/*
 * Publish the packages stashed by buildDebPkg to our apt repositories.
 *
 * Builds from master/main go to the 'master' repo, which only keeps the
 * latest version of the package.  Builds from tags go to the 'releases' repo,
 * which keeps all previous versions.  Other branches are not published.
 */
def call( List distributions, List architectures ){
	def cfg = aptlyConfig()
	if( cfg.channel == null ){
		echo "Not publishing packages built from branch ${env.BRANCH_NAME}"
		return
	}

	node( cfg.nodeLabel ){
		stage("Publish-${cfg.channel}"){
			dir('aptly-publish'){
				deleteDir()
				for( distro in distributions ){
					for( arch in architectures ){
						unstash "aptly-${distro}-${arch}"
					}
				}

				writeFile file: 'aptly-publish.sh', text: libraryResource('rm5248/aptly-publish.sh')
				withEnv([
					"APTLY_PREFIX=${cfg.channel}",
					"APTLY_KEEP_LATEST_ONLY=${cfg.channel == 'master'}",
					"APTLY_DISTRIBUTIONS=${distributions.join(' ')}",
					"APTLY_ARCHITECTURES=${architectures.join(',')}",
					"APTLY_BINARIES_DIR=binaries",
					"APTLY_CONFIG=${cfg.aptlyConfig}",
					"APTLY_PUBLIC_DIR=${aptlyConfig.publicDir(cfg)}",
					"APTLY_GPG_KEY=${cfg.gpgKey}",
					"APT_REPO_RSYNC_DEST=${cfg.rsyncDest}",
				]){
					sh 'bash aptly-publish.sh'
				}
			}
		}
	}
}
