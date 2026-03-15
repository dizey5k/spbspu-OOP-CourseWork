plugins {
    java
    id("org.springframework.boot") version "3.1.5"
}

group = "com.example"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web:3.1.5")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.3")
    implementation("com.opencsv:opencsv:5.11")
    testImplementation("org.springframework.boot:spring-boot-starter-test:3.1.5")
}

tasks.withType<Test> {
    useJUnitPlatform()
}