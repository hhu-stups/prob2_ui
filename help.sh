#!/bin/sh
cd src/main/resources/help
for d in */ ; do
    cd "${d}"
    echo "entering ${d}"
    count=`ls -1 *.md 2>/dev/null | wc -l`
		if [ $count != 0 ]; then
			echo "Found .md file. Converting..."
			rm -f *.md.hmtl
			for f in *.md; do pandoc "${f}" -f markdown_github -t html -s -o "${f}.html"; done
			for f in *.md.html; do sed -i s/.md/.md.html/g "${f}"; done
			echo "done"
		else
			echo "No .md files found. Going up"
		fi
		cd ..
done
echo "All converting done"
