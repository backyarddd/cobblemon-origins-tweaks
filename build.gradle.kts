import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("fabric-loom") version "1.11.8"
    kotlin("jvm") version "2.2.20"
}

val modVersion: String = providers.gradleProperty("mod_version").get()
version = modVersion
group = "dev.cobblemonorigins"

base {
    archivesName.set("cobblemon-origins-tweaks")
}

repositories {
    maven("https://maven.impactdev.net/repository/development/") {
        name = "ImpactDev"
        content { includeGroup("com.cobblemon") }
    }
}

loom {
    accessWidenerPath.set(file("src/main/resources/cobblemon_origins_tweaks.accesswidener"))
}

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    // Compile-only: Cobblemon is a required runtime dependency and is never shaded. Its POM pulls GraalVM/Mongo,
    // which the compiler does not need.
    modCompileOnly("com.cobblemon:fabric:${property("cobblemon_version")}") { isTransitive = false }
    // Client cosmetics: payload networking and the player render layer. The version the server suite and the pack use.
    modImplementation("net.fabricmc.fabric-api:fabric-api:0.116.17+1.21.1")

    testImplementation(kotlin("test"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    withSourcesJar()
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_21) }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
}

tasks.processResources {
    inputs.property("version", modVersion)
    filesMatching("fabric.mod.json") { expand("version" to modVersion) }
}

tasks.jar {
    from("LICENSE") { rename { "${it}_cobblemon-origins-tweaks" } }
}

tasks.test {
    useJUnitPlatform()
}
