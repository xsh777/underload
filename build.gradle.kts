plugins {
    `java-library`
    id("io.papermc.paperweight.userdev") version "1.5.5"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "anonymous"
version = "1.0-SNAPSHOT"
description = "get rid of skids from your server"
val apiVersion = "1.20"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

repositories {
    mavenCentral()
}

dependencies {
    paperweight.paperDevBundle("1.20.1-R0.1-SNAPSHOT")
    implementation("org.bstats", "bstats-bukkit", "3.0.2")
}

tasks {
    assemble {
        dependsOn(reobfJar)
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(17)
    }

    processResources {
        filteringCharset = Charsets.UTF_8.name()
        val props = mapOf(
                "name" to project.name,
                "version" to project.version,
                "description" to project.description,
                "apiVersion" to apiVersion
        )
        inputs.properties(props)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    shadowJar {
        fun reloc(pkg: String) = relocate(pkg, "anonymous.underload.dependency.$pkg")

        reloc("org.bstats")
    }
}
