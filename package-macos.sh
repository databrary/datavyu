#!/usr/bin/env bash
mvn -U -X -Pmac -Dmaven.test.skip=true clean jfx:native

mkdir target/jfx/native/Datavyu
cp -r target/jfx/native/datavyu.app target/jfx/native/Datavyu/
ln -s /Applications target/jfx/native/Datavyu/

echo version number:
read -r version

sudo hdiutil create -fs HFS+ "target/jfx/native/Datavyu-$version-OSX.dmg" -volname "Datavyu" -srcfolder target/jfx/native/Datavyu

rm -rf target/jfx/native/Datavyu
