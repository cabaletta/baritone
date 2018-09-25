set -e # this makes the whole script fail immediately if any one of these commands fails
./gradlew build
export VERSION=$(cat build.gradle | grep "version '" | cut -d "'" -f 2-2)

wget https://downloads.sourceforge.net/project/proguard/proguard/6.0/proguard6.0.3.zip
unzip proguard6.0.3.zip 2>&1 > /dev/null
cd build/libs
echo "-injars 'baritone-$VERSION.jar'" >> api.pro # insert current version
cat ../../scripts/proguard.pro | grep -v "this is the rt jar" | grep -v "\-injars" >> api.pro # remove default rt jar and injar lines
echo "-libraryjars '$(java -verbose 2>/dev/null | sed -ne '1 s/\[Opened \(.*\)\]/\1/p')'" >> api.pro # insert correct rt.jar location
tail api.pro # debug, print out what the previous two commands generated
cat api.pro | grep -v "\-keep class baritone.api" > standalone.pro # standalone doesn't keep baritone api
wget https://www.dropbox.com/s/zmc2l3jnwdvzvak/tempLibraries.zip?dl=1 # i'm sorry
mv tempLibraries.zip?dl=1 tempLibraries.zip
unzip tempLibraries.zip
mkdir ../../dist
java -jar ../../proguard6.0.3/lib/proguard.jar @api.pro
mv Obfuscated/baritone-$VERSION.jar ../../dist/baritone-api-$VERSION.jar
java -jar ../../proguard6.0.3/lib/proguard.jar @standalone.pro
mv Obfuscated/baritone-$VERSION.jar ../../dist/baritone-standalone-$VERSION.jar
mv baritone-$VERSION.jar ../../dist/baritone-unoptimized-$VERSION.jar
cd ../../dist
shasum * | tee checksums.txt
cd ..