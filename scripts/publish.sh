#! /bin/bash
artifact=$(mvn -q -Dexec.executable=echo -Dexec.args='${project.artifactId}-${project.version}.${project.packaging}' --non-recursive exec:exec)
url=https://api.bitbucket.org/2.0/repositories/${BITBUCKET_REPO_OWNER}/${BITBUCKET_REPO_SLUG}/downloads
echo "Publishing $artifact to $url"
curl -X POST --user "${PUBLISH_CREDENTIALS}" "$url" --form files=@"target/$artifact"
