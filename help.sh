#!/bin/bash
function conversion {
	count=`ls -1 *.md 2>/dev/null | wc -l`
	if [ $count != 0 ]; then
		echo "   Found .md file. Converting..."
		rm -f *.md.html
		for f in *.md; do pandoc "${f}" -f markdown_github -t html -s -o "${f}.html"; done
		if [ $(uname -s) == "Darwin" ]; then
			for f in *.md.html; do 
				sed -i "" s/.md/.md.html/g "${f}"; 
			done
		else
			for f in *.md.html; do 
				sed -i s/.md/.md.html/g "${f}"; 
			done
		fi
		echo "   done"
	else
		echo "   No .md files found."
	fi
}

function recursive_search {
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

start=src/main/resources/help
recursive_search $start
echo "All converting done"
