PROB2_HOME=$(HOME)/git_root/prob_prolog/ 
LOCAL=--include-build ../parsers_prob/
LOCAL2= $(LOCAL) --include-build ../sablecc-stups/
LOCAL3= $(LOCAL) --include-build ../java_prob2/

run-with-my-probcli:
	./gradlew -PprobHome=$(PROB2_HOME) $(LOCAL2) run
runsrc:
	./gradlew -PprobHome=$(PROB2_HOME) $(LOCAL3) run
offline:
	./gradlew -PprobHome=$(PROB2_HOME) $(LOCAL) run --offline
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
	@echo "For signing execute make macos_sign; make check"
	@echo "If successful do make notarize-app"
clean:
	rm out.prop

# version of ProB2UI without optional SNAPSHOT suffixes:
PROB2UI_VERSION=1.3.1
SNAPSH=
# comment in next line for SNAPSHOT builds
SNAPSH=-SNAPSHOT
# this is the name of the App in the DMG as created by jpackage
# it is set in the build.gradle file (applicationName)
# if the name differs from the internal app name we get an error
#  (unsealed contents present in the bundle root) when signing the app
DMGPROB2=ProB\ 2\ UI
# internal name within Contents/MacOs:
APPRPOB2=ProB\ 2\ UI
DMGFILE=build/distributions/$(DMGPROB2)-$(PROB2UI_VERSION).dmg
ZIP_FILE=build/distributions/ProB2-UI-ForNotarization.zip
# app file available after mounting dmg; could be done by command-line command
VOLUME=/Volumes/$(DMGPROB2)/
DMGAPPFILE=$(VOLUME)$(DMGPROB2).app
BUILDDIR=build/distributions/
#Name of the .app folder:
APPFILE=$(BUILDDIR)ProB\ 2\ UI.app
AC_USERNAME = "michael.leuschel@hhu.de"
ADC_CERTIFICATE_NAME = "Michael Leuschel (794LFG5T52)"

dmg: $(DMGFILE)
	@echo "Trying to create macOS DMG file"

$(DMGFILE): build.gradle src/main/java/de/prob2/ui/*.java src/main/java/de/prob2/ui/*/*.java src/main/java/de/prob2/ui/*/*/*.java
	@echo "Step 0: creating a DMG including the macOS App"
	./gradlew jpackage

$(APPFILE): $(DMGFILE)
	@echo "Step 1 after running make gradlew jpackage: 1a) Mounting the DMG"
	sudo hdiutil attach $(DMGFILE)
	@echo " 1b) Copying APP ($DMGAPPFILE) to $(BUILDDIR)"
	cp -R $(DMGAPPFILE) $(APPFILE)
	@echo " 1c) Unmounting $(VOLUME)"
	sudo hdiutil unmount $(VOLUME)

# Version which is used for JAR inside App:
#VERSION=$(PROB2UI_VERSION)-SNAPSHOT
VERSION=$(PROB2UI_VERSION)
PROB2APP_CONTENTS = $(APPFILE)/Contents/
JAR_TO_SIGN = $(BUILDDIR)jar-to-sign
$(JAR_TO_SIGN): $(APPFILE)
	@echo "Step 2: Unpacking JAR in APP so that we can sign the components"
	#unzip -l $(PROB2APP_CONTENTS)app/prob2-ui-$(VERSION)-mac.jar
	@echo " Unpacking to $(JAR_TO_SIGN)"
	unzip $(PROB2APP_CONTENTS)app/prob2-ui-$(VERSION)$(SNAPSH)-mac.jar -d $(JAR_TO_SIGN)

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
	$(CODESIGNRT2) $(PROB2APP_CONTENTS)MacOS/$(APPRPOB2)
makejar:
	@echo "Step 3b: Repacking the JAR with signed components"
	rm -f $(PROB2APP_CONTENTS)app/prob2-ui-$(VERSION)-mac.jar
	jar cvf $(PROB2APP_CONTENTS)app/prob2-ui-$(VERSION)-mac.jar -C $(JAR_TO_SIGN)/ .

check:
	@echo "4: Check signing (of $(JAR_TO_SIGN))"
	codesign -dvvv $(JAR_TO_SIGN)/com/sun/jna/darwin/libjnidispatch.jnilib
	codesign -vv --deep-verify $(JAR_TO_SIGN)/com/sun/jna/darwin/libjnidispatch.jnilib
	codesign -d --entitlements :- $(JAR_TO_SIGN)/com/sun/jna/darwin/libjnidispatch.jnilib
	@echo "Check signing of Java and JavaFX dylibs"
	for file in $(libs); do codesign -vv --deep-verify $(JAR_TO_SIGN)/$$file ; done
	codesign -vv --deep-verify $(PROB2APP_CONTENTS)MacOS/$(APPRPOB2)


$(ZIP_FILE): $(PROB2APP_CONTENTS)MacOS/$(APPRPOB2)
	@echo "Step 5: Putting APP into a zipfile for Apple's notarization (into $(ZIP_FILE))"
	#/usr/bin/ditto -c -k --keepParent "$(APPFILE)" $(ZIP_FILE)
	zip -vr $(ZIP_FILE) $(APPFILE)
	
NOTVERS = 1.3.1
notarize-app-old: $(ZIP_FILE)
	@echo "Step 6: Sending Notarization request to Apple for APP (in zipfile $(ZIP_FILE))"
	xcrun altool --notarize-app\
               --primary-bundle-id "de.hhu.stups.prob2ui.$(NOTVERS).zip"\
               --username $(AC_USERNAME)\
               --password "@keychain:AC_PASSWORD"\
               --file $(ZIP_FILE)

notarize-app: $(ZIP_FILE)
	@echo "Step 6: Sending Notarization request to Apple for APP (in zipfile $(ZIP_FILE))"
	xcrun notarytool submit $(ZIP_FILE) \
                   --keychain-profile "notarytool" \
                   --wait
	echo "In case of errors run:"
	echo " xcrun notarytool log --keychain-profile \"notarytool\"  HASH"

HASH=241970b9-c590-46c9-8d4c-71935772aaea
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
	@echo "Step 9: You can now zip and upload $(APPFILE) to the download area /stupshome/stups/downloads/prob2/ProB2-UI-$(PROB2UI_VERSION)-notarized.zip"




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
