# Problem

The dependency graph from RepositorySystem.collectDependencies does
not have direct provided dependencies. I expect provided dependencies
should be included in the graph when they are direct dependencies.

# Test Failure

You can reproduce the problem by `mvn test`:

``` 
[ERROR] Tests run: 2, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 0.168 s <<< FAILURE! - in suztomo.AppTest
[ERROR] shouldIncludeProvidedScope(suztomo.AppTest)  Time elapsed: 0.156 s  <<< FAILURE!
java.lang.AssertionError: expected:<5> but was:<[dom4j:dom4j:jar:1.6.1 (compile?), jdom:jdom:jar:1.0 (compile?), xom:xom:jar:1.0 (compile?)]>
	at suztomo.AppTest.shouldIncludeProvidedScope(AppTest.java:78)
```

# Analysis

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

[1]: https://github.com/GoogleCloudPlatform/cloud-opensource-java/blob/50da79355e042c5ff14ec72230cc6edbddcf8436/dependencies/src/main/java/com/google/cloud/tools/opensource/dependencies/RepositoryUtility.java#L112
[2]: https://github.com/GoogleCloudPlatform/cloud-opensource-java/blob/50da79355e042c5ff14ec72230cc6edbddcf8436/dependencies/src/main/java/com/google/cloud/tools/opensource/dependencies/DependencyGraphBuilder.java#L179
[3]: https://github.com/apache/maven/blob/master/maven-resolver-provider/src/main/java/org/apache/maven/repository/internal/MavenRepositorySystemUtils.java#L102
[4]: https://github.com/apache/maven-resolver/blob/47edcfe69c4e52ced4cb93d65b7348b5645cdd68/maven-resolver-util/src/main/java/org/eclipse/aether/util/graph/selector/ScopeDependencySelector.java#L119
[5]: https://github.com/apache/maven-resolver/blob/18dd2b5cde851256a9f44db25097efee0691d6b4/maven-resolver-impl/src/main/java/org/eclipse/aether/internal/impl/collect/DefaultDependencyCollector.java#L255