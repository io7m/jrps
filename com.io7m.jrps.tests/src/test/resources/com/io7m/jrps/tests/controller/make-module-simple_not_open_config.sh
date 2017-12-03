#!/bin/sh -ex

FAKETIME="faketime 2000-01-01T00:00:00Z"
JAR_FILE="${PWD}/com.io7m.tests.simple_not_open.jar"

rm -rfv com.io7m.tests.simple_not_open
rm -f "${JAR_FILE}"
${FAKETIME} mkdir -p com.io7m.tests.simple_not_open
${FAKETIME} mkdir -p com.io7m.tests.simple_not_open/META-INF
${FAKETIME} mkdir -p com.io7m.tests.simple_not_open/com/io7m/tests/simple_not_open

(cat <<EOF
module com.io7m.tests.simple_not_open
{
  exports com.io7m.tests.simple_not_open;
}
EOF
) | ${FAKETIME} tee com.io7m.tests.simple_not_open/module-info.java

(cat <<EOF
package com.io7m.tests.simple_not_open;

public class A
{

}
EOF
) | ${FAKETIME} tee com.io7m.tests.simple_not_open/com/io7m/tests/simple_not_open/A.java

(cat <<EOF
Manifest-Version: 1
JRPS-Resources: /simple.xml
EOF
) | ${FAKETIME} tee com.io7m.tests.simple_not_open/META-INF/MANIFEST.MF

(cat <<EOF
<?xml version="1.0" encoding="UTF-8" ?>
<r:resources xmlns:r="urn:com.io7m.jrps:1.0">
  <r:resource id="com.io7m.x" type="text" path="/com/io7m/tests/simple_not_open/A.java"/>
  <r:resource id="com.io7m.y" type="class" path="/com/io7m/tests/simple_not_open/A.class"/>
</r:resources>
EOF
) | ${FAKETIME} tee com.io7m.tests.simple_not_open/simple.xml

${FAKETIME} javac com.io7m.tests.simple_not_open/module-info.java com.io7m.tests.simple_not_open/com/io7m/tests/simple_not_open/A.java

hexdump com.io7m.tests.simple_not_open/com/io7m/tests/simple_not_open/A.class

(cd com.io7m.tests.simple_not_open; faketime '2000-01-01T00:00:00Z'  zip -r "${JAR_FILE}" .)

rm -rfv com.io7m.tests.simple_not_open
