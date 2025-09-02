CheesecakeCEPASReader
========
[![Maven Central](https://img.shields.io/maven-central/v/com.itachi1706.cepaslib/cepaslib)](https://search.maven.org/artifact/com.itachi1706.cepaslib/cepaslib)
[![JIRA Issues](https://img.shields.io/badge/JIRA-Issues-blue)](https://itachi1706.atlassian.net/browse/CCRANDLIB)
[![GitHub Actions](https://github.com/itachi1706/CheesecakeCEPASReader/workflows/Android%20CI/badge.svg)](https://github.com/itachi1706/CheesecakeCEPASReader/actions)
[![GitHub release](https://img.shields.io/github/release/itachi1706/CheesecakeCEPASReader.svg)](https://github.com/itachi1706/CheesecakeCEPASReader/releases) 
[![GitHub license](https://img.shields.io/github/license/itachi1706/CheesecakeCEPASReader.svg)](https://github.com/itachi1706/CheesecakeCEPASReader/blob/master/LICENSE) 
[![Code Climate](https://codeclimate.com/github/itachi1706/CheesecakeCEPASReader/badges/gpa.svg)](https://codeclimate.com/github/itachi1706/CheesecakeCEPASReader) 
[![Test Coverage](https://codeclimate.com/github/itachi1706/CheesecakeCEPASReader/badges/coverage.svg)](https://codeclimate.com/github/itachi1706/CheesecakeCEPASReader/coverage) 
[![Issue Count](https://codeclimate.com/github/itachi1706/CheesecakeCEPASReader/badges/issue_count.svg)](https://codeclimate.com/github/itachi1706/CheesecakeCEPASReader)

This is a slimmed down library for reading CEPAS-based cards such as Singapore EZ-Link cards based off FareBot

## Usage - Maven Central
To use this library in an Android Project, add the following lines into your app-level build.gradle file

```gradle
repositories {
	mavenCentral()
}
…
dependencies {  
  implementation 'com.itachi1706.cepaslib:cepaslib:<latest-version>' // See badge for latest version
}
```

## Usage - Artifactory
To use this library in an Android Project, add the following lines into your app-level build.gradle file

```gradle
repositories {
	maven {
		url "https://itachi1706.jfrog.io/artifactory/ccn-android-libs/"
	}
}
…
dependencies {
  implementation 'com.itachi1706.cepaslib:cepaslib:<latest-version>' // See badge for latest version
}
```

## Usage - Bintray (Deprecated)
To use this library in an Android Project, add the following lines into your app-level build.gradle file

```gradle
repositories {
	maven {
		url  "https://dl.bintray.com/itachi1706/ccn-android-lib"
	}
}
…
dependencies {
  implementation 'com.itachi1706.cepaslib:cepaslib:<latest-version>' // See badge for latest version
}
```

## Usage - JCenter (Deprecated)
To use this library in an Android Project, add the following lines into your app-level build.gradle file

```gradle
dependencies {
  implementation 'com.itachi1706.cepaslib:cepaslib:<latest-version>' // See badge for latest version number
}
```

## How to use

* Add the following lines to your project-level (/) build.gradle dependency
```gradle
buildscript {
    …
    dependencies {
        …
        classpath 'com.squareup.sqldelight:gradle-plugin:1.1.3'
    }
}

plugins {
    id 'com.github.ben-manes.versions' version '0.21.0'
}
```
* Add the following lines to your app-level (/app) build.gradle
```gradle
buildTypes {
        debug {
            multiDexEnabled true
        }
    }
```

Multidex is needed for debug builds as the debug unminified code base exceeds the 64k allowed in a single dex file on Android  
* To invoke the activity add the following code
```java
startActivity(new Intent(this, MainActivity.class));
finish();
```
* Note: If your theme is a dark theme, ensure that AppCompatDelegate has properly updated it. View options possible [here](https://developer.android.com/reference/android/support/v7/app/AppCompatDelegate.html#mode_night_auto)
```java
AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES); // View more at AppCompatDelegate file
```
* In your settings screen activity where you wish to add the settings to, add the following line 
```java
new SettingsHandler(getActivity()).initSettings(this);
```
* Make the Preferences button in the library to point to your own settings screen if you wish for it, add the following to add the class to your SharedPreferences
```java
CEPASLibBuilder.setPreferenceClass(YourPreference.class);
```
* To show the "About" menu option, add the following  

```java
// Java
CEPASLibBuilder.INSTANCE.shouldShowAboutMenuItem(true);
```

```kotlin
// Kotlin
CEPASLibBuilder.shouldShowAboutMenuItem(true)
```

* For more information and other library modification capabilities look at [CEPASLibBuilder.kt](https://github.com/itachi1706/CheesecakeCEPASReader/blob/master/src/main/java/com/itachi1706/cepaslib/CEPASLibBuilder.kt)
