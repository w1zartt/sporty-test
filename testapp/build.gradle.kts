plugins {
    java
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("com.github.davidmc24.gradle.plugin.avro")
}

val springCloudVersion: String by project
val avroVersion: String by project
val kafkaAvroSerializerVersion: String by project
val testcontainersKafkaVersion: String by project

group = "com.sporty"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://packages.confluent.io/maven/") }
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${springCloudVersion}")
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.apache.avro:avro:${avroVersion}")
    implementation("io.confluent:kafka-avro-serializer:${kafkaAvroSerializerVersion}")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:kafka:${testcontainersKafkaVersion}")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testCompileOnly("org.projectlombok:lombok")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testAnnotationProcessor("org.projectlombok:lombok")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
