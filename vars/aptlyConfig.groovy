/*
 * Central configuration for building debian packages and publishing them
 * to apt repositories with aptly.
 *
 * Most values may be overridden by setting a global environment variable
 * in Jenkins (Manage Jenkins -> System -> Global properties).
 *
 * Two apt repositories are maintained, each published under its own prefix:
 *   master   - builds from the master/main branch; only the latest version
 *              of each package is kept
 *   releases - builds from tags; all previous versions are kept
 */
def call(){
	def cfg = [:]

	// Debian stable and oldstable
	cfg.distributions = splitList(env.DEB_DISTRIBUTIONS, [ "trixie", "bookworm" ])
	cfg.architectures = splitList(env.DEB_ARCHITECTURES, [ "amd64", "arm64", "armhf", "i386" ])

	// Label of the node that holds the aptly database and repos.  Packages
	// are built on this node too, since the repos are bind mounted into the
	// build chroot.  Empty means any node, which is only correct if there is
	// a single build node.
	cfg.nodeLabel = env.APTLY_NODE ?: ''
	// Optional path to an aptly config file; aptly defaults to ~/.aptly.conf
	cfg.aptlyConfig = env.APTLY_CONFIG ?: ''
	// Directory that aptly publishes to.  If not set, this is found from
	// aptly's rootDir; see publicDir() below.
	cfg.publicDir = env.APTLY_PUBLIC_DIR ?: ''
	// GPG key used to sign the published repositories.  Set to 'none' to
	// publish unsigned repositories.
	cfg.gpgKey = env.APTLY_GPG_KEY ?: 'F49C33C8D9C76BAB5161C8C12C9A7D870227B75F'
	// ID of a Jenkins 'Secret file' credential holding the passphrase for
	// the GPG key.  Set to 'none' if the key has no passphrase.
	cfg.gpgPassphraseCredential = env.APTLY_GPG_PASSPHRASE_CREDENTIAL ?: 'b2f9f030-e1cf-405d-8aac-d15fe0efcdc1'
	// Optional rsync destination for the public directory, e.g.
	// user@www.example.com:/var/www/apt/
	cfg.rsyncDest = env.APT_REPO_RSYNC_DEST ?: ''

	cfg.channel = repoChannel()
	// Repository used to satisfy build dependencies.  Branches other than
	// master/main use the master repo.
	cfg.dependencyChannel = cfg.channel ?: 'master'

	return cfg
}

/*
 * The aptly public directory on the current node.  Must be called from
 * within a node block.
 */
String publicDir(Map cfg){
	if( cfg.publicDir.length() > 0 ){
		return cfg.publicDir
	}
	withEnv(["APTLY_CONFIG=${cfg.aptlyConfig}"]){
		return sh(script: libraryResource('rm5248/aptly-public-dir.sh'), returnStdout: true).trim()
	}
}

/*
 * The apt repository that this build publishes to: 'releases' for tags,
 * 'master' for the master/main branch, or null if it should not be published.
 */
String repoChannel(){
	if( env.TAG_NAME != null ){
		return 'releases'
	}
	if( env.BRANCH_NAME == 'master' || env.BRANCH_NAME == 'main' ){
		return 'master'
	}
	return null
}

List splitList(String value, List defaultValue){
	if( value == null || value.trim().length() == 0 ){
		return defaultValue
	}
	return value.trim().split(/[\s,]+/) as List
}
