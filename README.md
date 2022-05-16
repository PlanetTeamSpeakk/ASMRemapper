# ASMRemapper
Small tool I made to turn classes into ASM dumps and remap them between yarn, intermediary and moj.

## Usage
To use this tool, either download the jar and use it standalone, or consider automating whatever you need it for by adding it to the classpath with the following line:
```gradle
implementation "com.ptsmods:asmremapper:1.0.1"
```
Do note that when using the tool standalone, you must use the shadowed jar (ending in `-all`) and add a Minecraft jar to the classpath when running the jar using the `-cp` VM argument with the `java` command. E.g. `java -cp C:/path/to/minecraft/jar -jar ASMRemapper-1.0.1-all.jar`


Then, if you wish to automate it, make a task for it in your build.gradle file. The task I've made to turn my Compat classes into dumps is the following:  
(this is in a Fabric environment)
```gradle
task genCompatDumps(dependsOn: "classes") {
    doLast { // Run after all dependencies have been resolved.
        def output = sourceSets.main.allJava.srcDirs.iterator().next().toPath().resolve("com/example/dumps")
        def cache = Paths.get(System.getProperty("user.dir"), ".gradle").toAbsolutePath().toString()

        project(":common:Compat").subprojects {
            def projectCP = sourceSets.main.runtimeClasspath
            def mappings = null
            configurations.mappings.forEach { mappings = it } // Should only be one entry
            if (mappings == null) {
                System.err.printf("Project %s has no mappings dependency, skipping.\n", it.name)
                return
            }

            def input = it.buildDir.toPath().resolve("classes/java/main/com/example/classestodump")
            System.out.printf("ASMifying project %s.\n", it.name)
            javaexec {
                classpath = projectCP // All dependencies ASMRemapper has are already present on this classpath, including the necessary Minecraft jar.
                main = "com.ptsmods.asmremapper.ASMRemapper"

                args = [
                        "--cache=\"" + cache + '"',
                        "--package=com.example.classestodump",
                        "--input=\"" + input.toAbsolutePath() + '"',
                        "--output=\"" + output.toAbsolutePath() + '"',
                        "--mappings=\"" + mappings + '"',
                        "--maputil=com.example.ASMDump"
                ]
            }
        }
    }
}
```

The ASMDump class should have a `#map(String, String, String)` method where the first parameter is the intermediary name, the second is the Yarn name and the third is the Moj name. An example using Architectury would be:
```java
public class ASMDump {
    private static final boolean notDevEnv = !Platform.isDevelopmentEnvironment();

    public static String map(String intermediary, String yarn, String moj) {
        return Platform.isForge() ? moj : notDevEnv ? intermediary : yarn;
    }
}
```
