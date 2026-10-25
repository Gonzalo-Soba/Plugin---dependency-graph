plugins {
    java
}

group = "dev.taskgraph"
version = "0.1.0-SNAPSHOT"

val paperVersion = "26.3.build.222-stable"
val uaapiVersion = "3.0.0-beta-4"
val junitVersion = "6.1.3"
// Must match the Gson version exposed by paper-api, which provides it at runtime.
val gsonVersion = "2.14.0"
// Adventure version exposed by paper-api; only used to fetch its sources for API lookups.
val adventureVersion = "5.2.0"
// Brigadier version exposed by paper-api; only used to fetch its sources for API lookups.
val brigadierVersion = "1.3.11"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc"
    }
    maven("https://nexus.frengor.com/repository/public/") {
        name = "frengor"
    }
}

val apiSources = configurations.create("apiSources") {
    isTransitive = false
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:$paperVersion")
    compileOnly("com.frengor:ultimateadvancementapi:$uaapiVersion")

    apiSources("io.papermc.paper:paper-api:$paperVersion:sources")
    apiSources("com.frengor:ultimateadvancementapi:$uaapiVersion:sources")
    apiSources("net.kyori:adventure-api:$adventureVersion:sources")
    apiSources("net.kyori:adventure-text-serializer-legacy:$adventureVersion:sources")
    apiSources("com.mojang:brigadier:$brigadierVersion:sources")

    testImplementation(platform("org.junit:junit-bom:$junitVersion"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("com.google.code.gson:gson:$gsonVersion")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
    options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:removal"))
}

tasks.processResources {
    val props = mapOf("version" to project.version)
    inputs.properties(props)
    filesMatching("paper-plugin.yml") {
        expand(props)
    }
}

tasks.test {
    useJUnitPlatform()
}

// Copies the sources jars of the compile-time APIs to build/api-sources for signature lookups.
tasks.register<Sync>("downloadApiSources") {
    from(apiSources)
    into(layout.buildDirectory.dir("api-sources"))
}
