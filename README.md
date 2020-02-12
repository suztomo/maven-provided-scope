# Problem

The dependency graph from RepositorySystem.collectDependencies does
not have direct provided dependencies. I expect provided dependencies
should be included in the graph when they are direct dependencies.

# Failure on TestDefaultDependencyCollector

You can reproduce the problem by `mvn test`:

``` 
[ERROR]   TestDefaultDependencyCollector.shouldIncludeProvidedScope:72 value of    : iterable.size()
expected    : 5
but was     : 3
iterable was: [dom4j:dom4j:jar:1.6.1 (compile?), jdom:jdom:jar:1.0 (compile?), xom:xom:jar:1.0 (compile?)]
```

## Analysis

[MavenRepositorySystemUtils.newSession][3] creates
ScopedDependencySelector("test", "provided")

[ScopeDependencySelector.deriveChildSelector][4] creates clone of
itself with transitive=true. When transitive=true, it does not select
"provided" dependency.

[DefaultDependencyCollector.collectDependencies][5] always call
deriveChildSelector before using the selector from the session.

Therefore, ScopeDependencySelector always has transitive=true ( to
exclude provided dependencies) when it's used with
DefaultDependencyCollector.

# Success on TestProjectDependencyResolver

This succeeds by providing all 6 dependencies listed in jaxen:1.1.6.

# Comparison between DefaultDependencyCollector and ProjectDependencyResolver

![image](https://user-images.githubusercontent.com/28604/74356907-19650600-4d8d-11ea-82cb-3a4a9942291a.png)

[1]: https://github.com/GoogleCloudPlatform/cloud-opensource-java/blob/50da79355e042c5ff14ec72230cc6edbddcf8436/dependencies/src/main/java/com/google/cloud/tools/opensource/dependencies/RepositoryUtility.java#L112
[2]: https://github.com/GoogleCloudPlatform/cloud-opensource-java/blob/50da79355e042c5ff14ec72230cc6edbddcf8436/dependencies/src/main/java/com/google/cloud/tools/opensource/dependencies/DependencyGraphBuilder.java#L179
[3]: https://github.com/apache/maven/blob/master/maven-resolver-provider/src/main/java/org/apache/maven/repository/internal/MavenRepositorySystemUtils.java#L102
[4]: https://github.com/apache/maven-resolver/blob/47edcfe69c4e52ced4cb93d65b7348b5645cdd68/maven-resolver-util/src/main/java/org/eclipse/aether/util/graph/selector/ScopeDependencySelector.java#L119
[5]: https://github.com/apache/maven-resolver/blob/18dd2b5cde851256a9f44db25097efee0691d6b4/maven-resolver-impl/src/main/java/org/eclipse/aether/internal/impl/collect/DefaultDependencyCollector.java#L255