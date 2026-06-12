plugins {
	id("mod-platform")
	id("maven-publish")
	id("net.neoforged.moddev.legacyforge")
}

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
	dependencies {
		required("minecraft") {
			forgeLikeVersionRange = prop("deps.minecraft")
		}
		required("forge") {
			forgeLikeVersionRange.set("[1,)")
		}
		required("voicechat_api") {
			forgeLikeVersionRange = "[${prop("voicechat_api_version")},)"
		}
		required("gemini_live_lib") {
			forgeLikeVersionRange = "[${prop("gemini_live_lib_version")},)"
		}
	}
}

legacyForge {
	version = "${prop("deps.minecraft")}-${prop("deps.forge")}"

	runs {
		register("client") {
			client()
			gameDirectory = file("run/")
			ideName = "Forge Client (${sc.current.version})"
			programArgument("--username=Dev")
		}
		register("server") {
			server()
			gameDirectory = file("run/")
			ideName = "Forge Server (${sc.current.version})"
		}
	}


	mods {
		register(prop("mod.id")) {
			sourceSet(sourceSets["main"])
		}
	}
}

var forgeLoader = sc.current.component1().split("-")[1];
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
	mavenCentral()
	maven("https://maven.maxhenkel.de/repository/public") { name = "MaxHenkel" }
	maven("https://maven.sshcrack.me/releases") { name = "sshcrackRepositoryReleases" }
	strictMaven("https://api.modrinth.com/maven", "maven.modrinth") { name = "Modrinth" }
}

dependencies {
	implementation("de.maxhenkel.voicechat:voicechat-api:${prop("voicechat_api_version")}")
	implementation("me.sshcrack:gemini_live_lib:${prop("gemini_live_lib_version")}-${prop("deps.minecraft")}-forge")
	if (stonecutter.eval(sc.current.version, "<26")) {
		modRuntimeOnly("maven.modrinth:simple-voice-chat:forge-${prop("deps.minecraft")}-${prop("voicechat_mod_version")}")
	}
}

sourceSets {
	main {
		resources.srcDir(
			"${rootDir}/versions/datagen/${sc.current.version.split("-")[0]}/src/main/generated"
		)
	}
}

tasks.named("createMinecraftArtifacts") {
	dependsOn(tasks.named("stonecutterGenerate"))
}
