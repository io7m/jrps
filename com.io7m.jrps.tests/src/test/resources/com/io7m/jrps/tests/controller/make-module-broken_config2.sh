#!/bin/sh -ex

FAKETIME="faketime 2000-01-01T00:00:00Z"
JAR_FILE="${PWD}/com.io7m.tests.broken_config2.jar"

rm -rfv com.io7m.tests.broken_config2
rm -f "${JAR_FILE}"
${FAKETIME} mkdir -p com.io7m.tests.broken_config2
${FAKETIME} mkdir -p com.io7m.tests.broken_config2/META-INF
${FAKETIME} mkdir -p com.io7m.tests.broken_config2/com/io7m/tests/broken_config2

(cat <<EOF
module com.io7m.tests.broken_config2
{
  exports com.io7m.tests.broken_config2;
}
EOF
) | ${FAKETIME} tee com.io7m.tests.broken_config2/module-info.java

(cat <<EOF
package com.io7m.tests.broken_config2;

public class A
{

}
EOF
) | ${FAKETIME} tee com.io7m.tests.broken_config2/com/io7m/tests/broken_config2/A.java

(cat <<EOF
Manifest-Version: 1
JRPS-Resources: /broken.xml
EOF
) | ${FAKETIME} tee com.io7m.tests.broken_config2/META-INF/MANIFEST.MF

(cat <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<r:resources xmlns:r="urn:com.io7m.jrps:1.0">
  <what/>
</r:resources>
EOF
) | ${FAKETIME} tee com.io7m.tests.broken_config2/broken.xml

${FAKETIME} javac com.io7m.tests.broken_config2/module-info.java com.io7m.tests.broken_config2/com/io7m/tests/broken_config2/A.java

(cd com.io7m.tests.broken_config2; faketime '2000-01-01T00:00:00Z'  zip -r "${JAR_FILE}" .)

rm -rfv com.io7m.tests.broken_config2
