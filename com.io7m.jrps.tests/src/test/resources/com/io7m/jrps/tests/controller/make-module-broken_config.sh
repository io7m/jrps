#!/bin/sh -ex

FAKETIME="faketime 2000-01-01T00:00:00Z"
JAR_FILE="${PWD}/com.io7m.tests.broken_config.jar"

rm -rfv com.io7m.tests.broken_config
rm -f "${JAR_FILE}"
${FAKETIME} mkdir -p com.io7m.tests.broken_config
${FAKETIME} mkdir -p com.io7m.tests.broken_config/META-INF
${FAKETIME} mkdir -p com.io7m.tests.broken_config/com/io7m/tests/broken_config

(cat <<EOF
module com.io7m.tests.broken_config
{
  exports com.io7m.tests.broken_config;
}
EOF
) | ${FAKETIME} tee com.io7m.tests.broken_config/module-info.java

(cat <<EOF
package com.io7m.tests.broken_config;

public class A
{

}
EOF
) | ${FAKETIME} tee com.io7m.tests.broken_config/com/io7m/tests/broken_config/A.java

(cat <<EOF
Manifest-Version: 1
JRPS-Resources: /broken.xml
EOF
) | ${FAKETIME} tee com.io7m.tests.broken_config/META-INF/MANIFEST.MF

(cat <<EOF
I AM NOT XML.
EOF
) | ${FAKETIME} tee com.io7m.tests.broken_config/broken.xml

${FAKETIME} javac com.io7m.tests.broken_config/module-info.java com.io7m.tests.broken_config/com/io7m/tests/broken_config/A.java

(cd com.io7m.tests.broken_config; faketime '2000-01-01T00:00:00Z'  zip -r "${JAR_FILE}" .)

rm -rfv com.io7m.tests.broken_config
