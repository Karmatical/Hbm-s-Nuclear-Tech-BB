
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
	runs {
		register("testClient") { source("test"); client(); configurations.transitiveImplementation }
		register("testServer") { source("test"); server(); configurations.transitiveImplementation }
	}
}

dependencies {
	implementation(project(":common"))

	// minecraft
	minecraft(libs.minecraft)

	// development
	mappings("${libs.stationapiYarnmappings.get()}:v2")
	modImplementation(libs.stationapiLoader)

	// logging
	implementation(libs.log4jCore)
	implementation(libs.stationapiSlf4jApi)
	implementation(libs.log4jSlf4j18Impl)

	// convenience
	compileOnly(libs.lombok)
	annotationProcessor(libs.lombok)

	// adds some useful annotations for miscellaneous uses. does not add any dependencies, though people without the lib will be missing some useful context hints.
	implementation(libs.jetbrainsAnnotations)
	implementation(libs.guava)

	// stationapi
	// transitiveImplementation tells babric loom that you want this dependency to be pulled into other mod's development workspaces. Best used ONLY for required dependencies.
	modImplementation(libs.stationapi)

	// mods
	// https://github.com/calmilamsy/glass-config-api
	modImplementation(libs.stationapiGcapi)
	// https://github.com/calmilamsy/modmenu
	modImplementation(libs.stationapiModmenu)
	// https://github.com/Glass-Series/Always-More-Items
	modImplementation(libs.stationapiAlwaysmoreitems)
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
	from("LICENSE") { rename { "${it}_${project.property("archivesBaseName")}" } }
}

tasks.withType<GenerateModuleMetadata> {
	// Tells gradle to not generate module files for maven.
	// They aren't standard and the documentation is abysmal. Stop it.
	enabled = false
}

