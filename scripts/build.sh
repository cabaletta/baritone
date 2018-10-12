#!/bin/sh
set -e # this makes the whole script fail immediately if any one of these commands fails
./gradlew build
export VERSION=$(cat build.gradle | grep "version '" | cut -d "'" -f 2-2)

unzip -p build/libs/baritone-$VERSION.jar "mixins.baritone.refmap.json" | jq --sort-keys -c -M '.' > mixins.baritone.refmap.json
zip -u build/libs/baritone-$VERSION.jar mixins.baritone.refmap.json
rm mixins.baritone.refmap.json

cd scripts
javac Determinizer.java
java Determinizer ../build/libs/baritone-$VERSION.jar temp.jar
mv temp.jar ../build/libs/baritone-$VERSION.jar
cd ..


wget -nv https://downloads.sourceforge.net/project/proguard/proguard/6.0/proguard6.0.3.zip
unzip proguard6.0.3.zip 2>&1 > /dev/null
cd build/libs
rm -rf api.pro
echo "-injars 'baritone-$VERSION.jar'" >> api.pro # insert current version
cat ../../scripts/proguard.pro | grep -v "this is the rt jar" | grep -v "\-injars" >> api.pro # remove default rt jar and injar lines
echo "-libraryjars '$(java -verbose 2>/dev/null | sed -ne '1 s/\[Opened \(.*\)\]/\1/p')'" >> api.pro # insert correct rt.jar location
tail api.pro # debug, print out what the previous two commands generated
cat api.pro | grep -v "this is the keep api" > standalone.pro # standalone doesn't keep baritone api

#instead of downloading these jars from my dropbox in a zip, just assume gradle's already got them for us
mkdir -p tempLibraries
cat ../../scripts/proguard.pro | grep tempLibraries | grep .jar | cut -d "/" -f 2- | cut -d "'" -f -1 | xargs -n1 -I{} bash -c "find ~/.gradle -name {}" | tee /dev/stderr | xargs -n1 -I{} cp {} tempLibraries

rm -rf ../../dist
mkdir ../../dist
java -jar ../../proguard6.0.3/lib/proguard.jar @api.pro
cd ../../scripts
java Determinizer ../build/libs/Obfuscated/baritone-$VERSION.jar ../dist/baritone-api-$VERSION.jar
cd ../build/libs
rm -rf Obfuscated/*
java -jar ../../proguard6.0.3/lib/proguard.jar @standalone.pro
cd ../../scripts
java Determinizer ../build/libs/Obfuscated/baritone-$VERSION.jar ../dist/baritone-standalone-$VERSION.jar
cd ../build/libs
mv baritone-$VERSION.jar ../../dist/baritone-unoptimized-$VERSION.jar
cd ../../dist
shasum * | tee checksums.txt
cd ..