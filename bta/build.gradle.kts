
plugins {
    alias(libs.plugins.loom)
    java
}

val licenseFile = run {
    val rootLicense = layout.projectDirectory.file("LICENSE")
    val parentLicense = layout.projectDirectory.file("../LICENSE")
    when {
        rootLicense.asFile.exists() -> {
            logger.lifecycle("Using LICENSE from project root: {}", rootLicense.asFile)
            rootLicense
        }
        parentLicense.asFile.exists() -> {
            logger.lifecycle("Using LICENSE from parent directory: {}", parentLicense.asFile)
            parentLicense
        }
        else -> {
            logger.warn("No LICENSE file found in project or parent directory.")
            null
        }
    }
}

val lwjglNatives = resolveLwjglNatives()

val modVersion = "${providers.gradleProperty("mod_version").get()}+${libs.versions.bta.get()}"
val modGroup: Provider<String> = providers.gradleProperty("mod_group")
val modName: Provider<String> = providers.gradleProperty("mod_name")

val javaVersion: Provider<Int> = libs.versions.java.map { it.toInt() }

group = modGroup.get()
version = modVersion

fun resolveLwjglNatives(): String {
    // Sourced from https://www.lwjgl.org/
    return Pair(System.getProperty("os.name")!!, System.getProperty("os.arch")!!).let { (name, arch) ->
        when {
            arrayOf("Linux", "SunOS", "Unit").any { name.startsWith(it) } ->
                if (arrayOf("arm", "aarch64").any { arch.startsWith(it) })
                    "natives-linux${if (arch.contains("64") || arch.startsWith("armv8")) "-arm64" else "-arm32"}"
                else
                    "natives-linux"
            arrayOf("Mac OS X", "Darwin").any { name.startsWith(it) } ->
                "natives-macos${if (arch.startsWith("aarch64")) "-arm64" else ""}"
            arrayOf("Windows").any { name.startsWith(it) } ->
                if (arch.contains("64"))
                    "natives-windows${if (arch.startsWith("aarch64")) "-arm64" else ""}"
                else
                    "natives-windows-x86"
            else ->
                throw Error("Unrecognized or unsupported platform. Please set \"lwjglNatives\" manually")
        }
    }
}

loom {
    val btaChannel = libs.versions.btaChannel.get()
    val btaVersion = (if (btaChannel == "nightly") "" else "v") + libs.versions.bta.get()
    customMinecraftMetadata.set("https://downloads.betterthanadventure.net/bta-client/${btaChannel}/$btaVersion/manifest.json")
}

dependencies {
    // minecraft
    minecraft("::${libs.versions.bta.get()}")

    // runtime
    implementation(libs.btaLoader)
    implementation(libs.btaHalplibe)

    // compilation
    compileOnly(libs.bundles.btaLwjgl)
    compileOnly(libs.btaJoml)
    compileOnly(libs.btaJoml.primitives)
    compileOnly(libs.btaSlf4jApi)

    // development
    localRuntime(libs.btaModMenu)
    runtimeClasspath(libs.btaClientJar)
    val lwjglVer = libs.versions.btaLwjgl.get()
    localRuntime(platform("org.lwjgl:lwjgl-bom:${lwjglVer}"))
    localRuntime("org.lwjgl:lwjgl::$lwjglNatives")
    localRuntime("org.lwjgl:lwjgl-glfw::$lwjglNatives")
    localRuntime("org.lwjgl:lwjgl-openal::$lwjglNatives")
    localRuntime("org.lwjgl:lwjgl-opengl::$lwjglNatives")
    localRuntime("org.lwjgl:lwjgl-stb::$lwjglNatives")
}

repositories {
    mavenCentral()
    ivy("https://piston-data.mojang.com") {
        patternLayout { artifact("v1/[organisation]/[revision]/[module].jar") }
        metadataSources { artifact() }
    }
    maven("https://maven.fabricmc.net/") { name = "Fabric" }
    maven("https://maven.danygames2014.net/signalum") { name = "SignalumMavenMirror1" }
    maven("https://maven.thesignalumproject.net/infrastructure") { name = "SignalumMavenInfrastructure" }
    maven("https://maven.thesignalumproject.net/releases") { name = "SignalumMavenReleases" }
    maven("https://maven.thesignalumproject.net/nightly") { name = "SignalumMavenNightly" }
}

configurations.configureEach {
    exclude(group = "org.lwjgl.lwjgl")
    exclude(group = "net.java.jutils")
    exclude(group = "net.java.jinput")
    exclude(group = "net.sf.jopt-simple")
    exclude(group = "net.minecraft", module = "launchwrapper")
}

