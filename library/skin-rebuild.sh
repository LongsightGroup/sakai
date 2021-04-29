#!/bin/bash

for i in `grep -l skin-path src/morpheus-master/sass/themes/* | grep -v skin-rebuild | cut -d / -f 5 | sed 's/_//g; s/.scss//g;'`
   do
     echo "---------- Building Skin: $i ----------"
     /home/ubuntu/apache-maven-3.6.3/bin/mvn -Dsakai.skin.target=$i -Dsakai.skin.customization.file=src/morpheus-master/sass/themes/_$i.scss -Dmaven.tomcat.home=/tmp/21 clean install sakai:deploy-exploded
     sleep 5
     cp -r /tmp/21/webapps/library/skin/$i /var/www/html/sakai-library/21/skin/
     sleep 5
   done
