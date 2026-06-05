plugins {
    java
    id("io.github.goooler.shadow") version "8.1.8"
}

group = "com.astrasmp"
version = "1.0-SNAPSHOT"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withSourcesJar()
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    implementation("net.dv8tion:JDA:6.4.1")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("com.mysql:mysql-connector-j:9.2.0")
    implementation("com.google.code.gson:gson:2.11.0")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

tasks.shadowJar {
    archiveClassifier.set("")
    relocate("net.dv8tion.jda", "com.astrasmp.libs.jda")
    relocate("com.zaxxer.hikari", "com.astrasmp.libs.hikari")
    relocate("com.mysql", "com.astrasmp.libs.mysql")
    relocate("com.google.gson", "com.astrasmp.libs.gson")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}