#### Fuzzyble

A simple try to add fuzzy search in sqlite database. [![](https://jitpack.io/v/fcat97/fuzzyble-java.svg)](https://jitpack.io/#fcat97/fuzzyble-java)

> #### Setup
**Step 1.** Add the JitPack repository to your build file

Add it in your root build.gradle at the end of repositories:

```groovy
dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
  }
}
```

**Step 2.** Add the dependency

```groovy
dependencies {
  implementation 'com.github.fcat97:fuzzyble-java:Tag'
}
```

> #### Use
There are two main components that are exposed to the users of this library.

1.  **`Fuzzyble`**: To make a database fuzzy searchable, implement the `Fuzzyble` interface on it.
2. **`FuzzybleCursor`:** To manipulate the fuzzy utilities use get an instance of `FuzzyCursor` object.
3. The rest are documented well.
 
