#!/bin/bash
function conversion {
	count=`ls -1 *.md 2>/dev/null | wc -l`
	if [ $count != 0 ]; then
		echo "   Found .md file. Converting..."
		rm -f *.html
		rm -f *.adoc
		for f in *.md; do
			pandoc "${f}" -f markdown_github -t html -s | sed "s/\.md/.html/g" > "${f%.md}.html"
			pandoc "${f}" -f markdown_github -t asciidoc -s | sed "s/\.md/.adoc/g" > "${f%.md}.adoc"
		done
		echo "   done"
	else
		echo "   No .md files found."
	fi
}

function recursive_search {
if ! which pandoc &> /dev/null; then
	echo "pandoc is not installed. Aborting..."
	return
elif ! which sed &> /dev/null; then
	echo "sed is not installed. Aborting..."
	return
else
	converting=true
fi
cd "$1"
echo $PWD
conversion
count=`ls -1 */ 2>/dev/null | wc -l`
if [ $count != 0 ]; then
	for d in */ ; do
		recursive_search "${d}"
	done
fi
cd ..
}

converting=false
start=src/main/resources/help
recursive_search $start
if $converting = true; then
	echo "All converting done"
fi
