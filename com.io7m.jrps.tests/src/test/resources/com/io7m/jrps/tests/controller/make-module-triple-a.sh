#!/bin/sh -ex

FAKETIME="faketime 2000-01-01T00:00:00Z"
JAR_FILE="${PWD}/com.io7m.tests.triple_a.jar"

rm -rfv com.io7m.tests.triple_a
rm -f "${JAR_FILE}"
${FAKETIME} mkdir -p com.io7m.tests.triple_a
${FAKETIME} mkdir -p com.io7m.tests.triple_a/META-INF
${FAKETIME} mkdir -p com.io7m.tests.triple_a/com/io7m/tests/triple_a

(cat <<EOF
module com.io7m.tests.triple_a
{
  exports com.io7m.tests.triple_a;
  opens com.io7m.tests.triple_a;
}
EOF
) | ${FAKETIME} tee com.io7m.tests.triple_a/module-info.java

(cat <<EOF
package com.io7m.tests.triple_a;

public class A
{

}
EOF
) | ${FAKETIME} tee com.io7m.tests.triple_a/com/io7m/tests/triple_a/A.java

(cat <<EOF
Manifest-Version: 1
JRPS-Resources: /simple.xml
EOF
) | ${FAKETIME} tee com.io7m.tests.triple_a/META-INF/MANIFEST.MF

(cat <<EOF
<?xml version="1.0" encoding="UTF-8" ?>
<r:resources xmlns:r="urn:com.io7m.jrps:1.0">
  <r:resource id="com.io7m.x" type="text" path="/com/io7m/tests/triple_a/A.java"/>
  <r:resource id="com.io7m.y" type="class" path="/com/io7m/tests/triple_a/A.class"/>
  <r:resource id="com.io7m.z" type="class" path="/com/io7m/tests/triple_a/nonexistent.txt"/>
</r:resources>
EOF
) | ${FAKETIME} tee com.io7m.tests.triple_a/simple.xml

${FAKETIME} javac com.io7m.tests.triple_a/module-info.java com.io7m.tests.triple_a/com/io7m/tests/triple_a/A.java

hexdump com.io7m.tests.triple_a/com/io7m/tests/triple_a/A.class

(cd com.io7m.tests.triple_a; faketime '2000-01-01T00:00:00Z'  zip -r "${JAR_FILE}" .)

rm -rfv com.io7m.tests.triple_a
