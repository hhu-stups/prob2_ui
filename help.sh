#!/bin/sh
cd src/main/resources/help
rm -f *.md.hmtl
for f in *.md; do pandoc ${f} -f markdown_github -t html -s -o ${f}.html; done
for f in *.md.html; do sed -i s/.md/.md.html/g ${f}; done
