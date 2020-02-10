#!/bin/bash

set -o errexit
set -o nounset
set -o pipefail

convert_single() {
	local file="${1}"
	local format="${2}"
	local extension="${3}"
	
	echo "Converting ${file} to ${format} (.${extension})..."
	
	rm -f "${file}.${extension}"
	pandoc "${file}" -f markdown_github -t "${format}" -s | sed "s/\.md/.${extension}/g" > "${file%.md}.${extension}"
}

if ! which pandoc &> /dev/null; then
	echo "pandoc is not installed. Aborting..."
	exit 1
elif ! which sed &> /dev/null; then
	echo "sed is not installed. Aborting..."
	exit 1
fi

find src/main/resources/help -name "*.md" -print0 | while IFS="" read -r -d "" file; do
	convert_single "${file}" html html
done
