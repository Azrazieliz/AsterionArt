plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

kotlin {
    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

val taxonomySource = rootProject.layout.projectDirectory.file("spec/methodologies/concepts_full_v3.md")
val taxonomyCompiler = rootProject.layout.projectDirectory.file("tools/compile_character_taxonomy.py")
val generatedResources = layout.buildDirectory.dir("generated/taxonomy")

val compileCharacterTaxonomy by tasks.registering(Exec::class) {
    inputs.files(taxonomySource, taxonomyCompiler)
    outputs.file(generatedResources.map { it.file("character-taxonomy.json") })
    commandLine(
        providers.environmentVariable("ART_PYTHON").orElse("python3").get(),
        taxonomyCompiler.asFile,
        taxonomySource.asFile,
        generatedResources.get().file("character-taxonomy.json").asFile,
    )
}

sourceSets.main {
    resources.srcDir(generatedResources)
}

tasks.processResources {
    dependsOn(compileCharacterTaxonomy)
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    testImplementation("junit:junit:4.13.2")
}
