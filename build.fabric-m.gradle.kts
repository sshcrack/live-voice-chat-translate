plugins {
	id("mod-platform")
	id("net.fabricmc.fabric-loom")
}

stonecutter {
	val (version, loader) = current.project.split('-', limit = 2)
	properties.tags(version, loader)

	replacements.string(current.parsed >= "1.21.11") {
		replace("ResourceLocation", "Identifier")
		replace("location()", "identifier()")
	}
	replacements.string(current.parsed >= "26.1.2") {
		replace("FabricDataOutput", "FabricPackOutput")
	}
}

platform {
	loader = "fabric-m"
	dependencies {
		required("minecraft") {
			fabricLikeVersionRange = prop("deps.minecraft")
		}
		required("fabricloader") {
			fabricLikeVersionRange = ">=${prop("deps.fabric-loader")}"
		}
		required("voicechat_api") {
			fabricLikeVersionRange = ">=${prop("voicechat_api_version")}"
		}
		required("gemini_live_lib") {
			fabricLikeVersionRange = ">=${prop("gemini_live_lib_version")}"
		}
	}
}

loom {
	runs.named("client") {
		client()
		ideConfigGenerated(true)
		runDir = "run/"
		environment = "client"
		programArgs("--username=Dev")
		configName = "Fabric Client"
	}
	runs.named("server") {
		server()
		ideConfigGenerated(true)
		runDir = "run/"
		environment = "server"
		configName = "Fabric Server"
	}
}

repositories {
	mavenCentral()
	maven("https://maven.maxhenkel.de/repository/public") { name = "MaxHenkel" }
	maven("https://maven.sshcrack.me/releases") { name = "sshcrackRepositoryReleases" }
	strictMaven("https://api.modrinth.com/maven", "maven.modrinth") { name = "Modrinth" }
}

dependencies {
	minecraft("com.mojang:minecraft:${prop("deps.minecraft")}")
	implementation("net.fabricmc:fabric-loader:${prop("deps.fabric-loader")}")
	implementation("de.maxhenkel.voicechat:voicechat-api:${prop("voicechat_api_version")}")
	implementation("me.sshcrack:gemini_live_lib:${prop("gemini_live_lib_version")}-${prop("deps.minecraft")}-fabric")
	if (stonecutter.eval(sc.current.version, "<26")) {
		runtimeOnly("de.maxhenkel.voicechat:voicechat-api:${prop("voicechat_api_version")}:fabric-stub")
		implementation("maven.modrinth:simple-voice-chat:fabric-${prop("deps.minecraft")}-${prop("voicechat_mod_version")}")
	}
}
