#! /bin/bash
token=$1
user=$2

mkdir ~/.m2
echo "<settings><servers><server><id>github</id><username>${user}</username><password>${token}</password></server></servers></settings>" > ~/.m2/settings.xml
mvn -B deploy
