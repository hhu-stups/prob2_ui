#!/usr/bin/env bash

if [[ "${TRAVIS_OS_NAME}" == osx ]]; then
  # If logging JVM crashes
  # brew update
  # brew upgrade

  brew cask install java
  brew install gradle # Note: this will call `brew update`
  brew unlink python # fixes 'run_one_line' is not defined error in backtrace
fi
