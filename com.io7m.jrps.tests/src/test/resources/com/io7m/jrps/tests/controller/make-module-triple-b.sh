#!/bin/sh -ex

FAKETIME="faketime 2000-01-01T00:00:00Z"
JAR_FILE="${PWD}/com.io7m.tests.triple_b.jar"

rm -rfv com.io7m.tests.triple_b
rm -f "${JAR_FILE}"
${FAKETIME} mkdir -p com.io7m.tests.triple_b
${FAKETIME} mkdir -p com.io7m.tests.triple_b/META-INF
${FAKETIME} mkdir -p com.io7m.tests.triple_b/com/io7m/tests/triple_b

(cat <<EOF
module com.io7m.tests.triple_b
{
  exports com.io7m.tests.triple_b;
  opens com.io7m.tests.triple_b;
}
EOF
) | ${FAKETIME} tee com.io7m.tests.triple_b/module-info.java

(cat <<EOF
package com.io7m.tests.triple_b;

public class A
{

}
EOF
) | ${FAKETIME} tee com.io7m.tests.triple_b/com/io7m/tests/triple_b/A.java

(cat <<EOF
Manifest-Version: 1
JRPS-Resources: /simple.xml
EOF
) | ${FAKETIME} tee com.io7m.tests.triple_b/META-INF/MANIFEST.MF

(cat <<EOF
<?xml version="1.0" encoding="UTF-8" ?>
<r:resources xmlns:r="urn:com.io7m.jrps:1.0">
  <r:resource id="com.io7m.x" type="text" path="/com/io7m/tests/triple_b/A.java"/>
  <r:resource id="com.io7m.y" type="class" path="/com/io7m/tests/triple_b/A.class"/>
  <r:resource id="com.io7m.z" type="class" path="/com/io7m/tests/triple_b/nonexistent.txt"/>
</r:resources>
EOF
) | ${FAKETIME} tee com.io7m.tests.triple_b/simple.xml

${FAKETIME} javac com.io7m.tests.triple_b/module-info.java com.io7m.tests.triple_b/com/io7m/tests/triple_b/A.java

hexdump com.io7m.tests.triple_b/com/io7m/tests/triple_b/A.class

(cd com.io7m.tests.triple_b; faketime '2000-01-01T00:00:00Z'  zip -r "${JAR_FILE}" .)

rm -rfv com.io7m.tests.triple_b
