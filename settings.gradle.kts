pluginManagement {
	repositories {
		mavenLocal()
		mavenCentral()
		gradlePluginPortal()
		maven("https://maven.fabricmc.net/") { name = "Fabric" }
		maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
		maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
		maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
		maven("https://maven.parchmentmc.org") { name = "ParchmentMC" }
		maven("https://maven.minecraftforge.net/") { name = "Forge" }
		maven("https://maven.wagyourtail.xyz/releases") { name = "WagYourMaven" }
	}
	includeBuild("build-logic")
}

plugins {
	id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
	id("dev.kikugie.stonecutter") version "0.9.2"
}

stonecutter {
	create(rootProject) {
		fun match(version: String, vararg loaders: String) =
			loaders.forEach { version("$version-$it", version).buildscript = getBuildscript(it, version) }

		match("26.1.2", "fabric", "neoforge")
		match("1.21.11", "fabric", "neoforge")
		match("1.21.10", "fabric", "neoforge")
		match("1.21.8", "fabric", "neoforge")
		match("1.21.5", "fabric", "neoforge")
		match("1.21.4", "fabric", "neoforge")
		match("1.21.1", "fabric", "neoforge")
		match("1.20.1", "fabric", "forge")
		match("1.19.2", "fabric", "forge")
		match("1.18.2", "fabric", "forge")
		match("1.16.5", "fabric")

		vcsVersion = "1.21.1-fabric"
	}
}

val libraryDir = file("../gemini-live-library")
if (libraryDir.exists()) {
	println("Including Gemini Live Library from $libraryDir")
	val geminiVersion = "2.3.4"
	includeBuild(libraryDir) {
		dependencySubstitution {
			val combos = listOf(
				"1.16.5" to "fabric",
				"1.18.2" to "fabric", "1.18.2" to "forge",
				"1.19.2" to "fabric", "1.19.2" to "forge",
				"1.20.1" to "fabric", "1.20.1" to "forge",
				"1.21.1" to "fabric", "1.21.1" to "neoforge",
				"1.21.4" to "fabric", "1.21.4" to "neoforge",
				"1.21.5" to "fabric", "1.21.5" to "neoforge",
				"1.21.8" to "fabric", "1.21.8" to "neoforge",
				"1.21.10" to "fabric", "1.21.10" to "neoforge",
				"1.21.11" to "fabric", "1.21.11" to "neoforge",
				"26.1.2" to "fabric", "26.1.2" to "neoforge"
			)
			combos.forEach { (mc, loader) ->
				substitute(module("me.sshcrack:gemini_live_lib:$geminiVersion-$mc-$loader"))
					.using(project(":$mc-$loader"))
			}
		}
	}
} else {
	println("Warning: Gemini Live Library not found, skipping includeBuild")
}

private fun getBuildscript(loader: String, version: String): String {
	if (loader == "fabric") {
		return if (version.startsWith("1.")) {
			"build.fabric-o.gradle.kts"
		} else {
			"build.fabric-m.gradle.kts"
		}
	}
	if (loader == "forge") {
		return when (version) {
			"26.1.2", "1.21.11", "1.21.10", "1.21.8", "1.21.5", "1.21.4", "1.21.1" -> "build.forge-modern.gradle.kts"
			else -> "build.forge.gradle.kts"
		}
	}
	return "build.$loader.gradle.kts"
}
