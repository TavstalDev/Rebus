plugins {
    id("java")
    id("com.gradleup.shadow") version "9.3.1"
}

// Define project properties for versions and package name
val jetbrainsAnnotationsVersion = project.property("jetbrainsAnnotationsVersion") as String
val lombokVersion = project.property("lombokVersion") as String
val hikariCpVersion = project.property("hikariCpVersion") as String
val postgreSqlVersion = project.property("postgreSqlVersion") as String
val vaultApiVersion = project.property("vaultApiVersion") as String
val citizensApiVersion = project.property("citizensApiVersion") as String
val protocolLibVersion = project.property("protocolLibVersion") as String
val playerPointsVersion = project.property("playerPointsVersion") as String
val yggraLibVersion = project.property("yggraLibVersion") as String
val nbtApiVersion = project.property("nbtApiVersion") as String
val xseriesVersion = project.property("xseriesVersion") as String
val triumphGuiVersion = project.property("triumphGuiVersion") as String
val adventureApiVersion = project.property("adventureApiVersion") as String
val projectPackageName = "${project.group}.rebus"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(8)
    }
}

configurations.runtimeClasspath {
    attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 25)
}

// Define repositories for dependency resolution
repositories {
    //mavenLocal()
    mavenCentral() // Central Maven repository
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        url = uri("https://jitpack.io") // JitPack repository
    }
    maven {
        name = "sonatype"
        url = uri("https://oss.sonatype.org/content/groups/public/") // Sonatype repository
    }
    maven {
        name = "CodeMC"
        url = uri("https://repo.codemc.io/repository/maven-public/")
    }
    maven {
        name = "citizens"
        url = uri("https://maven.citizensnpcs.co/repo")
    }
    maven {
        name = "playerpoints"
        url = uri("https://repo.rosewooddev.io/repository/public/")
    }
}

// Define project dependencies
dependencies {
    compileOnly("org.jetbrains:annotations:${jetbrainsAnnotationsVersion}")
    compileOnly("org.projectlombok:lombok:${lombokVersion}")
    annotationProcessor("org.projectlombok:lombok:${lombokVersion}")

    compileOnly("org.spigotmc:spigot-api:1.8.8-R0.1-SNAPSHOT")
    compileOnly("com.github.MilkBowl:VaultAPI:${vaultApiVersion}") {
        exclude(group = "org.bukkit", module = "bukkit")
    }
    compileOnly("net.citizensnpcs:citizens-main:${citizensApiVersion}") {
        exclude(group = "*", module = "*")
    }
    compileOnly("net.dmulloy2:ProtocolLib:${protocolLibVersion}")
    compileOnly("com.zaxxer:HikariCP:${hikariCpVersion}")
    compileOnly("org.postgresql:postgresql:${postgreSqlVersion}")
    compileOnly("org.black_ixx:playerpoints:${playerPointsVersion}")
    compileOnly("de.tr7zw:item-nbt-api-plugin:${nbtApiVersion}")
    compileOnly("com.github.cryptomorin:XSeries:${xseriesVersion}")
    //compileOnly("dev.triumphteam:triumph-gui:${triumphGuiVersion}")
    compileOnly("com.github.lidanthedev.triumph-gui:triumph-gui:${triumphGuiVersion}")

    compileOnly("net.kyori:adventure-api:${adventureApiVersion}")
    compileOnly("net.kyori:adventure-text-serializer-legacy:${adventureApiVersion}")
    compileOnly("net.kyori:adventure-text-minimessage:${adventureApiVersion}")
    compileOnly("net.kyori:adventure-text-serializer-plain:${adventureApiVersion}")
    compileOnly("net.kyori:adventure-text-serializer-gson:${adventureApiVersion}")

    implementation("com.github.TavstalDev:yggraLib:v$yggraLibVersion")
}

// Disable the default JAR task
tasks.jar {
    enabled = false
}

// Configure the Shadow JAR task
tasks.shadowJar {
    archiveClassifier.set("") // Set the classifier for the JAR
    manifest {
        attributes["paperweight-mappings-namespace"] = "spigot" // Add custom manifest attributes
    }

    exclude("com/google/**")
    exclude("org/jspecify/**")
    exclude("org/slf4j/**")

    relocate("com.github.TavstalDev.yggraLib", "${project.group}.rebus.shadow.yggraLib")
}

// Ensure the Shadow JAR task runs during the build process
tasks.build {
    dependsOn(tasks.shadowJar)
}

// Configure additional tasks
tasks {

    // Process resources and expand placeholders in `plugin.yml`
    processResources {
        val props = mapOf("version" to project.version.toString()) // Define properties for resource filtering
        inputs.properties(props)
        filteringCharset = "UTF-8" // Set the charset for filtering
        filesMatching("plugin.yml") {
            expand(props) // Replace placeholders in `plugin.yml`
        }
    }
}