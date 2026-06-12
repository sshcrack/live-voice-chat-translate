plugins {
	id("mod-platform")
	id("net.fabricmc.fabric-loom")
}

stonecutter {
	val (version, loader) = current.project.split('-', limit = 2)
	properties.tags(version, loader)

	constants["devtools"] = false

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
	runs.register("clientAutoQuit") {
		client()
		ideConfigGenerated(true)
		runDir = "run/"
		environment = "client"
		programArgs("--username=Dev")
		vmArgs("-Dlive_voice_translate.autoQuit=true")
		configName = "Fabric Client AutoQuit"
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
	implementation("net.fabricmc.fabric-api:fabric-api:${prop("deps.fabric_api")}")
	implementation("me.sshcrack:gemini_live_lib:${prop("gemini_live_lib_version")}-${prop("deps.minecraft")}-fabric")
	runtimeOnly("de.maxhenkel.voicechat:voicechat-api:${prop("voicechat_api_version")}:fabric-stub")
	runtimeOnly("maven.modrinth:simple-voice-chat:fabric-${prop("voicechat_mod_version")}+${prop("deps.minecraft")}")
	runtimeOnly("net.fabricmc.fabric-api:fabric-api:${prop("deps.fabric_api")}")
}
