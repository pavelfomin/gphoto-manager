#! /bin/bash
artifact=$(mvn -q -Dexec.executable=echo -Dexec.args='${project.artifactId}-${project.version}.${project.packaging}' --non-recursive exec:exec)
versioned=${artifact%.*}
artifactVersion=${versioned##*-}
artifactId=${versioned%-*}
echo "::set-output name=maven_artifact::${artifact}"
echo "::set-output name=maven_artifact_id::${artifactId}"
echo "::set-output name=maven_artifact_version::${artifactVersion}"
