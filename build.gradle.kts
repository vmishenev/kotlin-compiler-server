import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsCompilerAttribute
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.bundling.BootJar

val kotlinVersion: String by System.getProperties()
val kotlinIdeVersion: String by System.getProperties()
val policy: String by System.getProperties()
val indexes: String by System.getProperties()
val indexesJs: String by System.getProperties()

group = "com.compiler.server"
version = "$kotlinVersion-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_1_8

val kotlinDependency: Configuration by configurations.creating {
    isTransitive = false
}
val kotlinJsDependency: Configuration by configurations.creating {
    isTransitive = false
    attributes {
        attribute(
            KotlinPlatformType.attribute,
            KotlinPlatformType.js
        )
        attribute(
            KotlinJsCompilerAttribute.jsCompilerAttribute,
            KotlinJsCompilerAttribute.legacy
        )
    }
}
val libJSFolder = "$kotlinVersion-js"
val libJVMFolder = kotlinVersion
val propertyFile = "application.properties"

val copyDependencies by tasks.creating(Copy::class) {
    from(kotlinDependency)
    into(libJVMFolder)
}
val copyJSDependencies by tasks.creating(Copy::class) {
    from(files(Callable { kotlinJsDependency.map { zipTree(it) } }))
    into(libJSFolder)
}

plugins {
    id("org.springframework.boot") version "2.4.5"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    kotlin("jvm") version "1.5.0"
    kotlin("plugin.spring") version "1.5.0"
}

allprojects {
    repositories {
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide")
        maven("https://kotlin.bintray.com/kotlin-ide-plugin-dependencies")
        maven("https://cache-redirector.jetbrains.com/jetbrains.bintray.com/intellij-third-party-dependencies")
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide-plugin-dependencies")
    }
    afterEvaluate {
        dependencies {
            dependencies {
                implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.3")
                implementation("org.jetbrains.kotlin:idea:202-$kotlinIdeVersion-IJ8194.7") {
                    isTransitive = false
                }
            }
        }
    }
}

dependencies {
    kotlinDependency("junit:junit:4.12")
    kotlinDependency("org.hamcrest:hamcrest:2.2")
    kotlinDependency("com.fasterxml.jackson.core:jackson-databind:2.12.1")
    kotlinDependency("com.fasterxml.jackson.core:jackson-core:2.12.1")
    kotlinDependency("com.fasterxml.jackson.core:jackson-annotations:2.12.1")
    // Kotlin libraries
    kotlinDependency("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    kotlinDependency("org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlinVersion")
    kotlinDependency("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    kotlinDependency("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
    kotlinDependency("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.5.0")
    kotlinJsDependency("org.jetbrains.kotlin:kotlin-stdlib-js:$kotlinVersion")

    annotationProcessor("org.springframework:spring-context-indexer")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.amazonaws.serverless:aws-serverless-java-container-springboot2:1.5.2")
    implementation("junit:junit:4.12")
    implementation("org.jetbrains.intellij.deps:trove4j:1.0.20200330")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-compiler:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-script-runtime:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-js:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-compiler-for-ide:$kotlinIdeVersion")
    implementation("org.jetbrains.kotlin:common:202-$kotlinIdeVersion-IJ8194.7")
    implementation("org.jetbrains.kotlin:core:202-$kotlinIdeVersion-IJ8194.7")
    implementation(project(":executors", configuration = "default"))
    implementation(project(":common", configuration = "default"))

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.5.0")
}

fun buildPropertyFile() {
    rootDir.resolve("src/main/resources/${propertyFile}").apply {
        println("Generate properties into $absolutePath")
        parentFile.mkdirs()
        writeText(generateProperties())
    }
}

fun generateProperties(prefix: String = "") = """
    # this file is autogenerated by build.gradle.kts
    kotlin.version=${kotlinVersion}
    policy.file=${prefix + policy}
    indexes.file=${prefix + indexes}
    indexesJs.file=${prefix + indexesJs}
    libraries.folder.jvm=${prefix + libJVMFolder}
    libraries.folder.js=${prefix + libJSFolder}
""".trimIndent()

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict", "-Xskip-metadata-version-check")
        jvmTarget = "1.8"
    }
    dependsOn(copyDependencies)
    dependsOn(copyJSDependencies)
    dependsOn(":executors:jar")
    dependsOn(":indexation:run")
    buildPropertyFile()
}

tasks.withType<BootJar> {
    requiresUnpack("**/kotlin-*.jar")
    requiresUnpack("**/kotlinx-*.jar")
}

val buildLambda by tasks.creating(Zip::class) {
    val lambdaWorkDirectoryPath = "/var/task/"
    from(tasks.compileKotlin)
    from(tasks.processResources) {
        eachFile {
            if (name == propertyFile) { file.writeText(generateProperties(lambdaWorkDirectoryPath)) }
        }
    }
    from(policy)
    from(indexes)
    from(indexesJs)
    from(libJSFolder) { into(libJSFolder) }
    from(libJVMFolder) { into(libJVMFolder) }
    into("lib") {
        from(configurations.compileClasspath) { exclude("tomcat-embed-*") }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
