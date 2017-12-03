#!/bin/sh -ex

FAKETIME="faketime 2000-01-01T00:00:00Z"
JAR_FILE="${PWD}/com.io7m.tests.simple_config.jar"

rm -rfv com.io7m.tests.simple_config
rm -f "${JAR_FILE}"
${FAKETIME} mkdir -p com.io7m.tests.simple_config
${FAKETIME} mkdir -p com.io7m.tests.simple_config/META-INF
${FAKETIME} mkdir -p com.io7m.tests.simple_config/com/io7m/tests/simple_config

(cat <<EOF
module com.io7m.tests.simple_config
{
  exports com.io7m.tests.simple_config;
  opens com.io7m.tests.simple_config;
}
EOF
) | ${FAKETIME} tee com.io7m.tests.simple_config/module-info.java

(cat <<EOF
package com.io7m.tests.simple_config;

public class A
{

}
EOF
) | ${FAKETIME} tee com.io7m.tests.simple_config/com/io7m/tests/simple_config/A.java

(cat <<EOF
Manifest-Version: 1
JRPS-Resources: /simple.xml
EOF
) | ${FAKETIME} tee com.io7m.tests.simple_config/META-INF/MANIFEST.MF

(cat <<EOF
<?xml version="1.0" encoding="UTF-8" ?>
<r:resources xmlns:r="urn:com.io7m.jrps:1.0">
  <r:resource id="com.io7m.x" type="text" path="/com/io7m/tests/simple_config/A.java"/>
  <r:resource id="com.io7m.y" type="class" path="/com/io7m/tests/simple_config/A.class"/>
  <r:resource id="com.io7m.z" type="class" path="/com/io7m/tests/simple_config/nonexistent.txt"/>
</r:resources>
EOF
) | ${FAKETIME} tee com.io7m.tests.simple_config/simple.xml

${FAKETIME} javac com.io7m.tests.simple_config/module-info.java com.io7m.tests.simple_config/com/io7m/tests/simple_config/A.java

hexdump com.io7m.tests.simple_config/com/io7m/tests/simple_config/A.class

(cd com.io7m.tests.simple_config; faketime '2000-01-01T00:00:00Z'  zip -r "${JAR_FILE}" .)

rm -rfv com.io7m.tests.simple_config
