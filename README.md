# jenkins-scripts

Jenkins shared library for building Debian packages.

## buildStandardDebPkg

Builds the package with pbuilder for the current Debian stable and oldstable
(trixie, bookworm) on amd64, arm64, armhf and i386, then publishes the
results to an apt repository with aptly:

| Build from      | apt repo   | Contents                          |
|-----------------|------------|-----------------------------------|
| `master`/`main` | `master`   | only the latest build             |
| a tag           | `releases` | every release that has been built |
| other branches  | none       | not published                     |

The matching repo is added to the build chroot so that build dependencies
can come from it: `master` for master/main (and other branches), `releases`
for tags. The published repo is bind mounted into the chroot, and a pbuilder
hook adds it as a `file://` source. pbuilder's `OTHERMIRROR` is not used: it
is only applied when the base tarball is created or with `--override-config`,
and the base tarball is shared between all jobs.

Each distribution is published under its own prefix, because the same
package version is built for every distribution and each prefix has a single
pool. Once served over HTTP, the sources lines are:

    deb https://apt.example.com/master/trixie trixie main
    deb https://apt.example.com/releases/bookworm bookworm main

The signing key's public half is exported to `archive-key.asc` at the top
of the public directory.

## Setup

Requires debian-pbuilder plugin 1.13 or later(`bindMounts`, `binariesDir`, `binariesSeparateFolders`).

Packages are built on the node that holds the apt repos. On that node,
install `aptly`, `gnupg` and `rsync`. Then import the signing key into the Jenkins user's keyring,
without a passphrase or with one cached by gpg-agent. Repos and publishes
are created automatically the first time they are needed.

Set these as global environment variables in Jenkins (Manage Jenkins ->
System -> Global properties) as needed:

| Variable              | Default                   | Purpose |
|-----------------------|---------------------------|---------|
| `APT_REPO_RSYNC_DEST` | *(none)*                  | If set, the public directory is rsync'd here after publishing, e.g. `www@web:/var/www/apt/` |
| `APTLY_NODE`          | any node                  | Label of the node that holds the aptly database, where packages are built and published. Set this if there is more than one build node |
| `APTLY_CONFIG`        | `~/.aptly.conf`           | aptly config file |
| `APTLY_PUBLIC_DIR`    | `<aptly rootDir>/public`  | Directory that aptly publishes to |
| `APTLY_GPG_KEY`       | rm5248 auto-build key     | Key to sign with, or `none` for an unsigned repo |
| `DEB_DISTRIBUTIONS`   | `trixie bookworm`         | Distributions to build for |
| `DEB_ARCHITECTURES`   | `amd64 arm64 armhf i386`  | Architectures to build for |
