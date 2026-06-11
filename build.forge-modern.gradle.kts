plugins {
	id("mod-platform")
	id("maven-publish")
	id("net.minecraftforge.gradle") version "[7.0.23,8.0)"
}

// Set Java toolchain early so ForgeGradle's Mavenizer uses the correct JDK
java.toolchain.languageVersion = JavaLanguageVersion.of(
	if (stonecutter.eval(sc.current.version, ">=26")) 25
	else if (stonecutter.eval(sc.current.version, ">=1.20.6")) 21
	else 17
)

stonecutter {
	val (version, loader) = current.project.split('-', limit = 2)
	properties.tags(version, loader)

	replacements.string(current.parsed >= "1.21.11") {
		replace("ResourceLocation", "Identifier")
		replace("location()", "identifier()")
	}
}

platform {
	loader = "forge"
	jarTask = "jar"
	sourcesJarTask = "sourcesJar"
	dependencies {
		required("minecraft") {
			forgeLikeVersionRange = prop("deps.minecraft")
		}
		required("forge") {
			forgeLikeVersionRange.set("[1,)")
		}
	}
}

minecraft {
	runs {
		configureEach {
			systemProperty("forge.enabledGameTestNamespaces", prop("mod.id"))
		}
		register("client")
		register("server") {
			args("--nogui")
		}
	}
}

var forgeLoader = sc.current.component1().split("-")[1]
publishing {
	publications {
		create<MavenPublication>("maven") {
			groupId = "me.sshcrack"
			artifactId = prop("mod.id")
			version = "${prop("mod.version")}${prop("mod.channel_tag")}-${prop("deps.minecraft")}-${forgeLoader}"

			artifact(tasks.named("jar"))
			tasks.findByName("sourcesJar")?.let { artifact(it) }
		}
	}

	repositories {
		maven {
			name = "sshcrackRepository"
			url = uri("https://maven.sshcrack.me/releases")

			credentials {
				username = (findProperty("sshcrackRepoMavenUser") as String?)
					?: System.getenv("sshcrackRepoMavenUser")
				password = (findProperty("sshcrackRepoMavenPassword") as String?)
					?: System.getenv("sshcrackRepoMavenPassword")
			}
		}
	}
}

repositories {
	minecraft.mavenizer(this)
	maven("https://maven.neoforged.net/releases") { name = "NeoForged" }
	maven("https://maven.minecraftforge.net/") { name = "Forge" }
	mavenCentral()
	strictMaven("https://api.modrinth.com/maven", "maven.modrinth") { name = "Modrinth" }
}

dependencies {
	implementation(minecraft.dependency("net.minecraftforge:forge:${prop("deps.minecraft")}-${prop("deps.forge")}"))
	annotationProcessor("org.spongepowered:mixin:${libs.versions.mixin.get()}:processor")
}

sourceSets {
	main {
		resources.srcDir(
			"${rootDir}/versions/datagen/${sc.current.version.split("-")[0]}/src/main/generated"
		)
	}
}
