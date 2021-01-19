PROB2_HOME=$(HOME)/git_root/prob_prolog/ 

run-with-my-probcli:
	./gradlew -PprobHome=$(PROB2_HOME) run
run:
	./gradlew -PprobHome=$(PROB2_HOME) run --offline
refresh:
	./gradlew -PprobHome=$(PROB2_HOME) run --refresh-dependencies
check-fr:
	native2ascii src/main/resources/de/prob2/ui/prob2_fr.properties > out.prop
	bbdiff out.prop src/main/resources/de/prob2/ui/prob2_fr.properties
check-ja:
	native2ascii src/main/resources/de/prob2/ui/prob2_ja.properties > out.prop
	bbdiff out.prop src/main/resources/de/prob2/ui/prob2_ja.properties
.PHONY: build
build:
	@echo "Creating a DMG including the macOS App"
	./gradlew jpackage
clean:
	rm out.prop

# version of ProB2UI without optional SNAPSHOT suffixes:
PROB2UI_VERSION=1.0.1
DMGFILE=build/distributions/ProB2-UI-$(PROB2UI_VERSION).dmg
ZIP_FILE=build/distributions/ProB2-UI-ForNotarization.zip
# app file available after mounting dmg; could be done by command-line command
VOLUME=/Volumes/ProB2-UI/
DMGAPPFILE=$(VOLUME)ProB2-UI.app
BUILDDIR=build/distributions/
APPFILE=$(BUILDDIR)ProB2-UI.app
AC_USERNAME = "EMAIL FOR SIGNING <---------------------- REPLACE"
ADC_CERTIFICATE_NAME = "ADC APP SIGNING CERTIFICATE <--------------- REPLACE"


$(DMGFILE): build.gradle src/main/java/de/prob2/ui/*.java src/main/java/de/prob2/ui/*/*.java src/main/java/de/prob2/ui/*/*/*.java
	@echo "Step 0: creating a DMG including the macOS App"
	./gradlew jpackage

$(APPFILE): $(DMGFILE)
	@echo "Step 1 after running make build: Mounting the DMG"
	sudo hdiutil attach $(DMGFILE)
	@echo "Copying APP to build/distributions"
	cp -R $(DMGAPPFILE) $(BUILDDIR)
	@echo "Unmounting $(VOLUME)"
	sudo hdiutil unmount $(VOLUME)

# Version which is used for JAR inside App:
VERSION=$(PROB2UI_VERSION)-SNAPSHOT
PROB2APP_CONTENTS = $(APPFILE)/Contents/
JAR_TO_SIGN = $(BUILDDIR)jar-to-sign
$(JAR_TO_SIGN): $(APPFILE)
	@echo "Step2: Unpacking JAR in APP so that we can sign the components"
	unzip $(PROB2APP_CONTENTS)app/prob2-ui-$(VERSION)-mac.jar -d $(JAR_TO_SIGN)

libs=libjavafx_iio.dylib libjfxmedia_avf.dylib libglib-lite.dylib libglib-lite.dylib libfxplugins.dylib libglass.dylib libjavafx_font.dylib libgstreamer-lite.dylib libjfxwebkit.dylib libprism_common.dylib libprism_es2.dylib libdecora_sse.dylib libjfxmedia.dylib libprism_sw.dylib

RTIME2 = --options runtime --entitlements probcli.entitlements
CODESIGNRT2 = codesign --timestamp -f $(RTIME2) -s $(ADC_CERTIFICATE_NAME)
CODESIGN = codesign --timestamp -f -s $(ADC_CERTIFICATE_NAME)
SIGNREM = codesign --remove-signature
macos_sign: $(JAR_TO_SIGN)
	@echo "Step 3: Signing"
	@echo "3a: Signing Java and JavaFX dylibs inside unpacked JAR $(JAR_TO_SIGN)"
	for file in $(libs); do $(CODESIGNRT2) $(JAR_TO_SIGN)/$$file ; done
	$(CODESIGNRT2) "$(JAR_TO_SIGN)/com/sun/jna/darwin/libjnidispatch.jnilib"
	make makejar
	@echo "3c: Signing app binary"
	$(CODESIGNRT2) "$(PROB2APP_CONTENTS)MacOS/ProB2-UI"
makejar:
	@echo "Step 3b: Repacking the JAR with signed components"
	rm -f $(PROB2APP_CONTENTS)app/prob2-ui-$(VERSION)-mac.jar
	jar cvf $(PROB2APP_CONTENTS)app/prob2-ui-$(VERSION)-mac.jar -C $(JAR_TO_SIGN)/ .

check:
	@echo "4: Check signing"
	codesign -dvvv $(JAR_TO_SIGN)/com/sun/jna/darwin/libjnidispatch.jnilib
	codesign -vv --deep-verify $(JAR_TO_SIGN)/com/sun/jna/darwin/libjnidispatch.jnilib
	codesign -d --entitlements :- $(JAR_TO_SIGN)/com/sun/jna/darwin/libjnidispatch.jnilib
	@echo "Check signing of Java and JavaFX dylibs"
	for file in $(libs); do codesign -vv --deep-verify $(JAR_TO_SIGN)/$$file ; done
	codesign -vv --deep-verify $(PROB2APP_CONTENTS)MacOS/ProB2-UI


$(ZIP_FILE): $(PROB2APP_CONTENTS)MacOS/ProB2-UI
	@echo "Step 5: Putting APP into a zipfile for Apple's notarization"
	/usr/bin/ditto -c -k --keepParent "$(APPFILE)" "$(ZIP_FILE)"
	
NOTVERS = 1.10.0-final
notarize-app: $(ZIP_FILE)
	@echo "Step 6: Sending Notarization request to Apple for APP"
	xcrun altool --notarize-app\
               --primary-bundle-id "de.hhu.stups.prob2ui.$(NOTVERS).zip"\
               --username $(AC_USERNAME)\
               --password "@keychain:AC_PASSWORD"\
               --file $(ZIP_FILE)


HASH=ed62183a-08eb-41d6-a80d-9d82bba850b0
info:
	@echo "Step 6b: Obtaining information about a particular notarization request"
	xcrun altool --notarization-info $(HASH) -u $(AC_USERNAME)

staple-app:
	@echo "Step 7: after successful notarization: Stapling the APP $(APPFILE)"
	xcrun stapler staple -v $(APPFILE)
verify-app:
	codesign -dvvv $(APPFILE)
	codesign -vv --deep-verify $(APPFILE)
	@echo "Step 8: check stapling of the APP $(APPFILE)"
	spctl --assess --type execute --verbose $(APPFILE)




#  unused stuff for dmg
checkdmg:
	codesign -dvvv $(DMGFILE)
	codesign -vv --deep-verify $(DMGFILE)
	spctl --assess --type execute --verbose $(DMGFILE)

signdmg:
	@echo "Step 2 after running make build (or ./gradlew jpackage) : Signing the DMG"
	codesign --deep --force --verify --verbose --sign $(ADC_CERTIFICATE_NAME) --options runtime $(DMGFILE)
notarize_dmg:
	@echo "Step 3: Sending Notarization request to Apple for DMG"
	xcrun altool --notarize-app\
               --primary-bundle-id "de.hhu.stups.prob2.$(NOTVERS).dmg"\
               --username $(AC_USERNAME)\
               --password "@keychain:AC_PASSWORD"\
               --file $(DMGFILE)
staple_dmg:
	@echo "Stapling"
	xcrun stapler staple -v $(DMGFILE)
