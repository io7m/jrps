#!/bin/sh -ex

FAKETIME="faketime 2000-01-01T00:00:00Z"
JAR_FILE="${PWD}/com.io7m.tests.triple_c.jar"

rm -rfv com.io7m.tests.triple_c
rm -f "${JAR_FILE}"
${FAKETIME} mkdir -p com.io7m.tests.triple_c
${FAKETIME} mkdir -p com.io7m.tests.triple_c/META-INF
${FAKETIME} mkdir -p com.io7m.tests.triple_c/com/io7m/tests/triple_c

(cat <<EOF
module com.io7m.tests.triple_c
{
  requires com.io7m.tests.triple_a;
  requires com.io7m.tests.triple_b;

  exports com.io7m.tests.triple_c;
  opens com.io7m.tests.triple_c;
}
EOF
) | ${FAKETIME} tee com.io7m.tests.triple_c/module-info.java

(cat <<EOF
package com.io7m.tests.triple_c;

public class A
{

}
EOF
) | ${FAKETIME} tee com.io7m.tests.triple_c/com/io7m/tests/triple_c/A.java

(cat <<EOF
Manifest-Version: 1
JRPS-Resources: /simple.xml
EOF
) | ${FAKETIME} tee com.io7m.tests.triple_c/META-INF/MANIFEST.MF

(cat <<EOF
<?xml version="1.0" encoding="UTF-8" ?>
<r:resources xmlns:r="urn:com.io7m.jrps:1.0">
  <r:resource id="com.io7m.x" type="text" path="/com/io7m/tests/triple_c/A.java"/>
  <r:resource id="com.io7m.y" type="class" path="/com/io7m/tests/triple_c/A.class"/>
  <r:resource id="com.io7m.z" type="class" path="/com/io7m/tests/triple_c/nonexistent.txt"/>
</r:resources>
EOF
) | ${FAKETIME} tee com.io7m.tests.triple_c/simple.xml

${FAKETIME} javac --module-path . com.io7m.tests.triple_c/module-info.java com.io7m.tests.triple_c/com/io7m/tests/triple_c/A.java

hexdump com.io7m.tests.triple_c/com/io7m/tests/triple_c/A.class

(cd com.io7m.tests.triple_c; faketime '2000-01-01T00:00:00Z'  zip -r "${JAR_FILE}" .)

rm -rfv com.io7m.tests.triple_c
