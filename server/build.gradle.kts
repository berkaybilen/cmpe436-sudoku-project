plugins {
    id("org.jetbrains.kotlin.jvm")
    id("com.github.johnrengelman.shadow") version "8.1.1"
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

dependencies {
    implementation(project(":shared"))
    
    // WebSocket server
    implementation("org.java-websocket:Java-WebSocket:1.5.6")
    
    // JSON serialization
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Logging
    implementation("org.slf4j:slf4j-simple:2.0.9")
    
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("org.sudoku.ServerMain")
}