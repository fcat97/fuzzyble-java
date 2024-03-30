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

There are four main components that are exposed to the users of this library.

1. **FuzzybleCursor:** To manipulate the fuzzy utilities get an instance of `FuzzyCursor`. It can perform the following functions:
   1. Create required data for fuzzy search.
   2. Get fuzzy suggestion for any text.
2. **Fuzzyble**: To make a database fuzzy searchable, implement the `Fuzzyble` interface on it. 
   Some databases are immutable. For example, android's `Room` database. The schema need to be defined during compile time, so an auxiliary mutable database is needed.
   1. **Immutable Database**: This database works as read-only source database. This contains actual text on which search will be performed.
   2. **Mutable Database**: This database is used to store required data for fuzzy search. If the source database itself `mutable`, there is no need to provide an addition database. Since `mutable` database can be used for both application i.e. source and sink data.
3. **Strategy**: Defines how to generate and store required data for fuzzy matching.
   1. WordLen: Simpler approach to find similar words. 
   2. Trigram: Trigram approach for finding suggestion.
   3. Implement `Strategy` class to provide better solution.
4. **Similarity**: How to calculate if two words are similar? Currently, have:
   1. Levenshtein Distance
 
