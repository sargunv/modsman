#!/usr/bin/env sh

VERSION=$(./gradlew -q :modsman-gui:printVersion)
cd modsman-gui/build/jpackage
echo "Creating modsman-gui-$VERSION.zip"
zip -FSr "modsman-gui-$VERSION.zip" *.app
