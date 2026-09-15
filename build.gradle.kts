plugins {
    java
    jacoco
    `maven-publish`
}

group = "edu.msudenver.cs.jdnss"
version = "3.2.1"
description = """JDNSS is a small DNS server written in Java. It was written to be both
    |more portable and more secure due to its implementation in Java. It is
    |currently intended for use as a "leaf" server as it does not do iterative
    |or recursive lookups for clients, nor does it do any cacheing. It reads
    |BIND zone files.
""".trimMargin()

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Core dependencies
    implementation("com.google.inject:guice:7.0.0")
    implementation("edu.msudenver.cs:JCLO:1.3.6")

    // Logging
    implementation("org.apache.logging.log4j:log4j-api:2.24.1")
    implementation("org.apache.logging.log4j:log4j-core:2.25.4")

    // Annotations
    compileOnly("org.projectlombok:lombok:1.18.42")
    compileOnly("org.jetbrains:annotations:26.0.2-1")
    compileOnly("com.github.spotbugs:spotbugs-annotations:4.9.6")
    annotationProcessor("org.projectlombok:lombok:1.18.42")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.20.0")
}

tasks.test {
    useJUnit()
    jvmArgs = listOf("-javaagent:${configurations.testRuntimeClasspath.get().find { it.name.contains("mockito-core") }}")
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "edu.msudenver.cs.jdnss.JDNSS",
            "Multi-Release" to "true"
        )
    }
}

// Create a fat jar with all dependencies
tasks.register<Jar>("fatJar") {
    archiveClassifier = "all"
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.isFile }.map { zipTree(it) }
    })

    manifest {
        attributes(
            "Main-Class" to "edu.msudenver.cs.jdnss.JDNSS",
            "Multi-Release" to "true"
        )
    }
}

// JaCoCo configuration
jacoco {
    toolVersion = "0.8.13"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true
        html.required = true
        csv.required = false
    }
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}

// Process resources with filtering enabled
tasks.processResources {
    filesMatching("**/*") {
        val props = project.properties.filterKeys { it.isNotEmpty() }
        expand(props)
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            pom {
                name = "Java DNS Server"
                url = "https://github.com/drsjb80/JDNSS"
                description = project.description

                organization {
                    name = "MSU Denver Computer Science"
                    url = "http://cs.msudenver.edu"
                }

                issueManagement {
                    system = "Github"
                    url = "https://github.com/drsjb80/JDNSS/issues"
                }

                licenses {
                    license {
                        name = "MIT License"
                        url = "https://opensource.org/licenses/MIT"
                        distribution = "repo"
                    }
                }

                developers {
                    developer {
                        id = "beatys"
                        name = "Steve Beaty"
                        email = "beatys@msudenver.edu"
                        url = "http://weba.msudenver.edu/searchchannel/jsp/directoryprofile/profile.jsp?uName=beaty"
                        organization = "MSU Denver Computer Science"
                        organizationUrl = "http://cs.msudenver.edu"
                    }
                }
            }
        }
    }
}
