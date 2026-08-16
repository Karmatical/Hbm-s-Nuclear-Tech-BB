
import java.net.URI

plugins {
	id("maven-publish")
	id("fabric-loom") version libs.versions.loom.get()
	id("babric-loom-extension") version libs.versions.loom.get()
}

base.archivesName = project.property("archives_base_name") as String
version = project.property("mod_version") as String
group = project.property("maven_group") as String

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(libs.versions.java.get().toInt())
	}
	// generates sources
	withSourcesJar()
}

loom {
	// If you want to make a testmod for your mod, right click on src, and create a new folder with the same name as source() below.
	// Intellij should give suggestions for testmod folders.
	runs {
		register("testClient") { source("test"); client(); configurations.transitiveImplementation }
		register("testServer") { source("test"); server(); configurations.transitiveImplementation }
	}
}

dependencies {
	// minecraft
	minecraft("com.mojang:minecraft:${libs.versions.minecraft.get()}")

	// development
	mappings("net.glasslauncher:biny:${libs.versions.stationapiYarnmappings.get()}:v2")
	modImplementation("net.fabricmc:fabric-loader:${libs.versions.stationapiLoader.get()}")

	// logging
	implementation("org.apache.logging.log4j:log4j-core:${libs.versions.log4jCore.get()}")
	implementation("org.slf4j:slf4j-api:${libs.versions.stationapiSlf4jApi.get()}")
	implementation("org.apache.logging.log4j:log4j-slf4j18-impl:${libs.versions.log4jSlf4j18Impl.get()}")

	// convenience
	compileOnly("org.projectlombok:lombok:${libs.versions.lombok.get()}")
	annotationProcessor("org.projectlombok:lombok:${libs.versions.lombok.get()}")

	// adds some useful annotations for miscellaneous uses. does not add any dependencies, though people without the lib will be missing some useful context hints.
	implementation("org.jetbrains:annotations:${libs.versions.jetbrainsAnnotations.get()}")
	implementation("com.google.guava:guava:${libs.versions.guava.get()}")

	// stationapi
	// transitiveImplementation tells babric loom that you want this dependency to be pulled into other mod's development workspaces. Best used ONLY for required dependencies.
	modImplementation("net.modificationstation:StationAPI:${libs.versions.stationapi.get()}")

	// development environment mods
	// https://github.com/calmilamsy/glass-config-api
	modImplementation("net.glasslauncher.mods:GlassConfigAPI:${libs.versions.stationapiGcapi.get()}")
	// https://github.com/calmilamsy/modmenu
	modImplementation("net.danygames2014:modmenu:${libs.versions.stationapiModmenu.get()}")
	// https://github.com/Glass-Series/Always-More-Items
	modImplementation("net.glasslauncher.mods:AlwaysMoreItems:${libs.versions.stationapiAlwaysmoreitems.get()}")
}

repositories {
	mavenCentral()
	exclusiveContent {
		forRepository { maven("https://api.modrinth.com/maven") }
		filter { includeGroup("maven.modrinth") }
	}
	maven("https://maven.glass-launcher.net/snapshots/")
	maven("https://maven.glass-launcher.net/releases/")
	maven("https://maven.glass-launcher.net/babric")
	maven("https://maven.minecraftforge.net/")
	maven("https://jitpack.io/")
}

publishing {
	repositories {
		mavenLocal()
		if (project.hasProperty("my_maven_username")) {
			maven {
				url = URI("https://maven.example.com")
				credentials {
					username = "${project.property("my_maven_username")}"
					password = "${project.property("my_maven_password")}"
				}
			}
		}
	}

	publications {
		register("mavenJava", MavenPublication::class) {
			artifactId = project.property("archives_base_name") as String
			from(components["java"])
		}
	}
}

configurations.all {
	exclude("babric")
}

tasks.withType<ProcessResources> {
	inputs.property("version", project.property("version"))

	filesMatching("fabric.mod.json") {
		expand(mapOf("version" to project.property("version")))
	}
}

tasks.withType<JavaCompile> {
	// ensure that the encoding is set to UTF-8, no matter what the system default is
	// this fixes some edge cases with special characters not displaying correctly
	// see http://yodaconditions.net/blog/fix-for-java-file-encoding-problems-with-gradle.html
	options.encoding = "UTF-8"
}

tasks.withType<Jar> {
	from("LICENSE") {
		rename { "${it}_${project.property("archivesBaseName")}" }
	}
}

tasks.withType<GenerateModuleMetadata> {
	// Tells gradle to not generate module files for maven.
	// They aren't standard and the documentation is abysmal. Stop it.
	enabled = false
}

