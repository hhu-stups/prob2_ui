#!/bin/sh
for f in *.png; do convert ${f} -crop 700x300+0+0 ${f}; done 
