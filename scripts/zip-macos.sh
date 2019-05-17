#!/usr/bin/env sh

VERSION=$(git describe --tags)
cd modsman-gui/build/jpackage
echo "Creating modsman-gui-$VERSION.zip"
zip -FSr "modsman-gui-$VERSION.zip" *.app
