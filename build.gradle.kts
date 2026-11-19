plugins {
    java
    id("org.springframework.boot") version "4.1.2" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

allprojects {
    group = "com.iotsensor"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    pluginManager.withPlugin("java") {
        extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion = JavaLanguageVersion.of(25)
            }
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
        }

        // Integration tests (*IT) live in src/test next to the unit tests and need Docker (Testcontainers).
        // `test` runs all tests (and is what `check`/`build` run); `unitTest` and `integrationTest` run one kind.
        val testSourceSet = extensions.getByType<SourceSetContainer>()["test"]

        val unitTest = tasks.register<Test>("unitTest") {
            description = "Runs the unit tests (everything except *IT). No Docker needed."
            group = LifecycleBasePlugin.VERIFICATION_GROUP

            testClassesDirs = testSourceSet.output.classesDirs
            classpath = testSourceSet.runtimeClasspath

            filter.excludeTestsMatching("*IT")
        }

        tasks.register<Test>("integrationTest") {
            description = "Runs the integration tests (*IT). Needs Docker for Testcontainers."
            group = LifecycleBasePlugin.VERIFICATION_GROUP

            testClassesDirs = testSourceSet.output.classesDirs
            classpath = testSourceSet.runtimeClasspath

            filter.includeTestsMatching("*IT")
            filter.isFailOnNoMatchingTests = false
            shouldRunAfter(unitTest)
        }
    }
}

