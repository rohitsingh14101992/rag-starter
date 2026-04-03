val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
}

group = "com.example"
version = "0.0.1"

application {
    mainClass.set("com.example.rag.ApplicationKt")
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("ch.qos.logback:logback-classic:$logback_version")

    // Ktor Client (LLM API calls)
    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-cio:$ktor_version")
    implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")

    // Ktor Server (REST API)
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-server-auth:$ktor_version")
    implementation("io.ktor:ktor-server-auth-jwt:$ktor_version")
    implementation("io.ktor:ktor-server-cors:$ktor_version")
    implementation("io.ktor:ktor-server-status-pages:$ktor_version")

    // Password hashing
    implementation("at.favre.lib:bcrypt:0.10.2")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

    // PDFBox for PDF extraction
    implementation("org.apache.pdfbox:pdfbox:3.0.6")

    // LangChain4j local embedding model
    implementation("dev.langchain4j:langchain4j-embeddings-all-minilm-l6-v2:0.31.0")

    // Vector store dependencies
    implementation("org.postgresql:postgresql:42.7.3")
    implementation("com.pgvector:pgvector:0.1.6")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.0")

    // Dependency Injection
    implementation("io.insert-koin:koin-core:3.5.3")

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
    
    // Testcontainers for VectorStore integration tests
    testImplementation("org.testcontainers:postgresql:1.19.7")
    testImplementation("org.testcontainers:junit-jupiter:1.19.7")
}
