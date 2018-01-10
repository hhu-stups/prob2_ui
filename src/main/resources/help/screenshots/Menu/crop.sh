#!/bin/sh
for f in *.png; do convert ${f} -crop 800x225+0+0 ${f}; done 
