plugins {
    id("java")
}


group = "dk.sdu.mmmi"
version = "2.0-SNAPSHOT"

repositories {
    mavenCentral()
}

tasks.compileJava {
    options.encoding = "UTF-8"
    options.javaModuleMainClass = "dk.sdu.mmmi.Main"
}

tasks.jar {
    manifest.attributes["Main-Class"] = "dk.sdu.mmmi.Main"
    configurations["compileClasspath"].forEach { file: File ->
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        exclude("META-INF/*.RSA", "META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.LIST")
        from(zipTree(file.absoluteFile))
    }
}


dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    // https://mvnrepository.com/artifact/org.json/json
    implementation("org.json:json:20230618")
    // https://mvnrepository.com/artifact/com.badlogicgames.gdx/gdx
    implementation("com.badlogicgames.gdx:gdx:1.12.0")
    // https://mvnrepository.com/artifact/com.badlogicgames.gdx/gdx-freetype
    implementation("com.badlogicgames.gdx:gdx-freetype:1.12.0")
    // https://mvnrepository.com/artifact/com.badlogicgames.gdx/gdx-backend-lwjgl
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl:1.12.0")
    implementation("com.badlogicgames.gdx:gdx-platform:1.12.0:natives-desktop")
    implementation("com.badlogicgames.gdx:gdx-freetype-platform:1.12.0:natives-desktop")
}

tasks.test {
    useJUnitPlatform()
}