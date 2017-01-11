# Alfresco Calendar Action AMP, creating standard iCal/ics out of Alfresco event metadata

Creating standard iCal/ics file out of Alfresco event metadata

### Usage

#### Create AMP
```
mvn clean install
```
#### Install AMP
```
/opt/alfresco/bin/apply_amps.sh
```
or
```
java -jar /opt/alfresco/bin/alfresco-mmt.jar install rs-make-alfresco-actions-calendar /opt/alfresco/tomcat/webapps/alfresco.war
```

### License
Licensed under the MIT license.
http://www.opensource.org/licenses/mit-license.php
