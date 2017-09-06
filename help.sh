#!/bin/sh
/usr/bin/wget -r -e robots=off --delete-after --random-wait --spider -A html http://www3.hhu.de/stups/prob/index.php/User_Manual 2>&1 | /usr/bin/grep '^--' | /usr/bin/awk '{ print $3 }' | /usr/bin/grep -v '\.\(css\|js\|png\|gif\|jpg\|JPG\)$' | /usr/bin/grep 'prob2' | /usr/bin/xargs /usr/bin/wget --mirror -np --random-wait -P src/main/resources/help/
/usr/bin/rm -rf www3.hhu.de/
