#! /bin/bash
token=$1
user=$2
repository=$3
artifact=$4
version=$5

FILE=target/${artifact}

curl \
    -H "Authorization: token ${token}" \
    -H "Content-Type: $(file -b --mime-type $FILE)" \
    --data-binary @$FILE \
    "https://uploads.github.com/repos/${user}/${repository}/releases/${version}/assets?name=$(basename $FILE)"