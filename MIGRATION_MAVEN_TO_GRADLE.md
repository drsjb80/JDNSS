# Maven to Gradle Migration Guide

## Summary

Your JDNSS project has been successfully migrated from Apache Maven to Gradle. All functionality, dependencies, plugins, and build configurations have been preserved.

## Files Created/Modified

### New Gradle Files
- **`build.gradle.kts`** - Main build configuration (Kotlin DSL format)
- **`settings.gradle.kts`** - Project settings and name configuration
- **`gradle/wrapper/gradle-wrapper.jar`** - Gradle wrapper executable
- **`gradle/wrapper/gradle-wrapper.properties`** - Gradle wrapper configuration
- **`gradlew`** - Unix/Linux/Mac Gradle wrapper script
- **`gradlew.bat`** - Windows Gradle wrapper script

### Files to Archive/Remove
The following Maven files are no longer needed and can be removed:
- `pom.xml` - Main Maven build file
- `dependency-reduced-pom.xml` - Generated Maven file
- `.mvn/` - Maven configuration directory (if present)

## Feature Mapping: Maven → Gradle

| Maven Feature | Gradle Equivalent | Status |
|---------------|-------------------|--------|
| Dependencies (compile) | `implementation()` | ✅ Migrated |
| Dependencies (provided) | `compileOnly()` | ✅ Migrated |
| Dependencies (test) | `testImplementation()` | ✅ Migrated |
| Annotation processors | `annotationProcessor()` | ✅ Migrated |
| Maven repositories | `repositories { mavenCentral() }` | ✅ Migrated |
| Java compiler version | `java.toolchain.languageVersion` | ✅ Migrated (Java 21) |
| JAR manifest configuration | `tasks.jar { manifest {} }` | ✅ Migrated |
| Maven Shade Plugin | Custom `fatJar` task | ✅ Migrated |
| Maven Assembly Plugin | (replaced by fatJar) | ✅ Migrated |
| JaCoCo code coverage | `jacoco` plugin + `jacocoTestReport` | ✅ Migrated |
| Maven Surefire Plugin | JUnit configuration in test task | ✅ Migrated |
| Resource filtering | `processResources` task | ✅ Migrated |
| Maven POM metadata | Publishing configuration | ✅ Migrated |
| Project metadata | Group, version, description | ✅ Migrated |
| Developers section | POM in publishing block | ✅ Migrated |
| Issue management | POM in publishing block | ✅ Migrated |
| Licenses | MIT License in publishing block | ✅ Migrated |

## Key Dependencies Preserved

All dependencies and versions are identical to the Maven configuration:

### Core Dependencies
- `com.google.inject:guice:7.0.0`
- `edu.msudenver.cs:JCLO:1.3.6`

### Logging
- `org.apache.logging.log4j:log4j-api:2.24.1`
- `org.apache.logging.log4j:log4j-core:2.25.4`

### Annotations
- `org.projectlombok:lombok:1.18.42` (with annotation processor)
- `org.jetbrains:annotations:26.0.2-1`
- `com.github.spotbugs:spotbugs-annotations:4.9.6`

### Testing
- `junit:junit:4.13.2`
- `org.mockito:mockito-core:5.20.0`

## Common Gradle Commands

Replace Maven commands with these Gradle equivalents:

| Maven Command | Gradle Command | Purpose |
|---------------|----------------|---------|
| `mvn clean` | `./gradlew clean` | Clean build artifacts |
| `mvn compile` | `./gradlew classes` | Compile source code |
| `mvn test` | `./gradlew test` | Run tests |
| `mvn package` | `./gradlew build` | Build JAR and run tests |
| `mvn install` | `./gradlew publishToMavenLocal` | Publish to local Maven cache |
| `mvn clean package` | `./gradlew clean build` | Clean and build |
| `mvn verify` | `./gradlew check` | Run all checks/tests |
| `mvn site` | (Not available) | Site generation not configured |

## Additional Gradle Tasks

These custom tasks are available in addition to standard tasks:

- **`./gradlew fatJar`** - Create a fat JAR with all dependencies (similar to Maven Shade plugin)
  - Output: `build/libs/jdnss-all.jar`
  - This is a convenient all-in-one JAR file that can be run directly

- **`./gradlew jacocoTestReport`** - Generate JaCoCo code coverage reports
  - Output: `build/reports/jacoco/test/html/index.html`
  - Reports are automatically generated after running tests

## Project Structure

Your existing project structure is unchanged and compatible with Gradle:

```
JDNSS/
├── src/
│   ├── main/
│   │   ├── java/edu/msudenver/cs/jdnss/
│   │   └── resources/
│   └── test/
│       ├── java/edu/msudenver/cs/jdnss/
│       └── resources/
├── build.gradle.kts
├── settings.gradle.kts
├── gradlew
├── gradlew.bat
├── gradle/
│   └── wrapper/
├── build/ (generated)
└── target/ (Maven - can be deleted)
```

## Build Output Locations

Gradle places build output in the `build/` directory (not `target/`):

- **Compiled classes**: `build/classes/`
- **Generated JAR**: `build/libs/jdnss-3.2.1.jar`
- **Fat JAR**: `build/libs/jdnss-3.2.1-all.jar`
- **Test results**: `build/test-results/`
- **Coverage reports**: `build/reports/jacoco/`

## IDE Integration

### IntelliJ IDEA
- Open the project root directory
- IDEA will automatically detect `build.gradle.kts` and configure the project
- Run → Edit Configurations... to set up run configurations if needed

### Eclipse
- Use Gradle buildship plugin (usually pre-installed)
- Right-click project → Configure → Convert to Gradle Project (if not auto-detected)

### VS Code
- Install "Extension Pack for Java" or equivalent
- Open the project root - VS Code will auto-configure Gradle

### Command Line
- All `./gradlew` commands work from the project root on Mac/Linux
- All `gradlew.bat` commands work from the project root on Windows

## CI/CD Integration

If you have CI/CD pipelines using Maven, update them to use Gradle:

**Before (Maven):**
```bash
mvn clean package
```

**After (Gradle):**
```bash
./gradlew build
```

Gradle automatically downloads its dependencies (via the wrapper), so no separate Gradle installation is needed on CI systems.

## Troubleshooting

### "Command not found: gradlew"
Make sure you're in the project root directory where `gradlew` is located.

### Gradle daemon issues
Run `./gradlew --stop` to stop the daemon, then try again.

### Stale cache
Run `./gradlew clean build` to clear all build artifacts and rebuild.

### Test failures
Run `./gradlew test --info` for detailed test output.

### Java version issues
The build is configured for Java 21. Verify your JDK version:
```bash
java -version
```

## Next Steps

1. **Delete Maven files** (optional but recommended):
   ```bash
   rm pom.xml dependency-reduced-pom.xml
   rm -rf .mvn/
   ```

2. **Delete old build artifacts**:
   ```bash
   rm -rf target/
   ```

3. **Update any build documentation** to reference Gradle instead of Maven

4. **Update CI/CD pipelines** to use `./gradlew build` instead of `mvn clean package`

5. **Test the build** locally:
   ```bash
   ./gradlew clean build
   ```

## Questions?

For Gradle-specific questions, refer to the official documentation:
- https://docs.gradle.org/8.9/userguide/userguide.html
- https://docs.gradle.org/8.9/samples/sample_java_library.html

For project-specific issues, consult the JDNSS project documentation or submit issues on GitHub.
