#!/bin/bash
#
# Add packages to an aptly repository and publish it.
#
# Environment:
#   APTLY_PREFIX           repo name and publish prefix(master or releases).
#                          Each distribution is published to <prefix>/<dist>
#                          since a prefix has a single pool, and the same
#                          package version will differ between distributions
#   APTLY_KEEP_LATEST_ONLY if 'true', remove all other versions of the packages
#                          being added
#   APTLY_DISTRIBUTIONS    space-separated list of distributions
#   APTLY_ARCHITECTURES    comma-separated list of architectures to publish
#   APTLY_UPLOAD_DIR       directory containing a subdirectory per distribution
#                          with the packages to add
#   APTLY_CONFIG           optional aptly config file
#   APTLY_PUBLIC_DIR       aptly public directory(<rootDir>/public)
#   APTLY_GPG_KEY          key to sign with, or 'none' to not sign
#   APT_REPO_RSYNC_DEST    optional rsync destination for the public directory

set -euo pipefail

aptly_cmd() {
	aptly ${APTLY_CONFIG:+-config="$APTLY_CONFIG"} "$@"
}

# A rebuild of the same version will not be byte-for-byte identical, so
# allow it to replace the file that was published before
publish_args=(-force-overwrite)
if [ "$APTLY_GPG_KEY" = none ]; then
	publish_args+=(-skip-signing)
else
	publish_args+=(-gpg-key="$APTLY_GPG_KEY" -batch)
fi

# Names of the source packages that the given files were built from
source_names() {
	local f name
	for f in "$@"; do
		case "$f" in
		*.dsc)
			sed -n 's/^Source: *//p' "$f"
			;;
		*)
			name=$(dpkg-deb -f "$f" Source | cut -d' ' -f1)
			[ -n "$name" ] || name=$(dpkg-deb -f "$f" Package)
			echo "$name"
			;;
		esac
	done | sort -u
}

# aptly is not safe to run concurrently, so only let one job publish at a time
exec 9> "$HOME/.aptly-publish.lock"
flock 9

for dist in $APTLY_DISTRIBUTIONS; do
	pkg_dir="$APTLY_UPLOAD_DIR/$dist"
	if [ ! -d "$pkg_dir" ]; then
		echo "No packages for $dist"
		continue
	fi
	# Every architecture includes the same source package and arch:all
	# packages, so only add one copy of each file
	files=()
	declare -A seen=()
	while IFS= read -r -d '' f; do
		base=$(basename "$f")
		if [ -z "${seen[$base]:-}" ]; then
			seen[$base]=1
			files+=("$f")
		fi
	done < <(find "$pkg_dir" -type f \( -name '*.deb' -o -name '*.udeb' -o -name '*.dsc' \) -print0 | sort -z)
	unset seen
	if [ "${#files[@]}" -eq 0 ]; then
		echo "No packages for $dist"
		continue
	fi

	repo="$APTLY_PREFIX-$dist"
	prefix="$APTLY_PREFIX/$dist"
	if ! aptly_cmd repo show "$repo" > /dev/null 2>&1; then
		aptly_cmd repo create -distribution="$dist" -component=main "$repo"
	fi

	if [ "$APTLY_KEEP_LATEST_ONLY" = true ]; then
		query=""
		for name in $(source_names "${files[@]}"); do
			query="${query:+$query | }\$Source ($name) | Name ($name)"
		done
		echo "Removing old packages: $query"
		aptly_cmd repo remove "$repo" "$query"
	fi

	aptly_cmd repo add -force-replace "$repo" "${files[@]}"

	published=$(aptly_cmd publish list -raw)
	if grep -qx "$prefix $dist" <<< "$published"; then
		aptly_cmd publish update "${publish_args[@]}" "$dist" "$prefix"
	else
		aptly_cmd publish repo "${publish_args[@]}" \
			-architectures="$APTLY_ARCHITECTURES,source" \
			-distribution="$dist" -component=main \
			"$repo" "$prefix"
	fi
done

if [ "$APTLY_KEEP_LATEST_ONLY" = true ]; then
	# Remove the old versions from aptly's internal package pool
	aptly_cmd db cleanup
fi

if [ "$APTLY_GPG_KEY" != none ]; then
	gpg --armor --export "$APTLY_GPG_KEY" > "$APTLY_PUBLIC_DIR/archive-key.asc"
fi

if [ -n "${APT_REPO_RSYNC_DEST:-}" ]; then
	# Copy the new packages before the indexes that reference them, then
	# sync everything and delete what has been removed
	rsync -rlt --exclude=dists "$APTLY_PUBLIC_DIR/" "$APT_REPO_RSYNC_DEST"
	rsync -rlt --delete-after "$APTLY_PUBLIC_DIR/" "$APT_REPO_RSYNC_DEST"
fi
