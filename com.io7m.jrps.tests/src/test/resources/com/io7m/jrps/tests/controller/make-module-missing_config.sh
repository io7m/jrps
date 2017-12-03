#!/bin/sh -ex

FAKETIME="faketime 2000-01-01T00:00:00Z"
JAR_FILE="${PWD}/com.io7m.tests.missing_config.jar"

rm -rfv com.io7m.tests.missing_config
rm -f "${JAR_FILE}"
${FAKETIME} mkdir -p com.io7m.tests.missing_config
${FAKETIME} mkdir -p com.io7m.tests.missing_config/META-INF
${FAKETIME} mkdir -p com.io7m.tests.missing_config/com/io7m/tests/missing_config

(cat <<EOF
module com.io7m.tests.missing_config
{
  exports com.io7m.tests.missing_config;
}
EOF
) | ${FAKETIME} tee com.io7m.tests.missing_config/module-info.java

(cat <<EOF
package com.io7m.tests.missing_config;

public class A
{

}
EOF
) | ${FAKETIME} tee com.io7m.tests.missing_config/com/io7m/tests/missing_config/A.java

(cat <<EOF
Manifest-Version: 1
JRPS-Resources: /missing.xml
EOF
) | ${FAKETIME} tee com.io7m.tests.missing_config/META-INF/MANIFEST.MF

${FAKETIME} javac com.io7m.tests.missing_config/module-info.java com.io7m.tests.missing_config/com/io7m/tests/missing_config/A.java

(cd com.io7m.tests.missing_config; faketime '2000-01-01T00:00:00Z'  zip -r "${JAR_FILE}" .)

rm -rfv com.io7m.tests.missing_config
