plugins {
    java
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

configurations {
    testCompileOnly { extendsFrom(compileOnly.get()) }
    testAnnotationProcessor { extendsFrom(annotationProcessor.get()) }
}

dependencies {
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.flyway)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.mapstruct)

    compileOnly(libs.lombok)

    annotationProcessor(libs.lombok)
    annotationProcessor(libs.mapstruct.processor)

    runtimeOnly(libs.postgresql)
    runtimeOnly(libs.flyway.database.postgresql)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)
    testRuntimeOnly(libs.junit.platform.launcher)
}

// Only produce the executable Spring Boot jar - the extra "-plain.jar" would make the
// Dockerfile's `COPY build/libs/*.jar` ambiguous.
tasks.jar {
    enabled = false
}

tasks.compileJava {
    options.compilerArgs.addAll(
        listOf(
            "-Amapstruct.defaultComponentModel=spring",
            "-Amapstruct.unmappedTargetPolicy=ERROR"
        )
    )
}
