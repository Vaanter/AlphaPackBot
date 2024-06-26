import net.ltgt.gradle.errorprone.errorprone

/*
 *    Copyright 2020 Valentín Bolfík
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

plugins {
  java
  id("io.quarkus")
  id("com.github.ben-manes.versions") version "0.51.0"
  id("net.ltgt.errorprone") version "3.1.0"
}

repositories {
  mavenLocal()
  mavenCentral()
  maven {
    name = "m2-dv8tion"
    url = uri("https://m2.dv8tion.net/releases")
  }
  maven {
    url = uri("https://m2.chew.pro/releases")
  }
}

val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project

dependencies {
  implementation(enforcedPlatform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}"))
  implementation("io.quarkus:quarkus-grpc")
  implementation("io.quarkus:quarkus-arc")

  implementation("net.dv8tion:JDA:4.4.1_353") {
    exclude(module = "opus-java")
  }

  implementation("redis.clients:jedis:5.1.0")
  implementation("pw.chew:jda-chewtils:1.24.1")

  implementation("com.google.guava:guava:33.1.0-jre")
  implementation("com.google.mug:mug:7.2")
  implementation("org.apache.commons:commons-lang3:3.14.0")
  errorprone("com.google.errorprone:error_prone_core:2.26.1")

  testImplementation("io.quarkus:quarkus-junit5")
}

group = "com.vb.alphapackbot"
version = "5.1.0"

java {
  sourceCompatibility = JavaVersion.VERSION_21
  targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<Test> {
  systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
  jvmArgs("-Djdk.tracePinnedThreads", "-Xmx4G", "-XX:+ZGenerational")
}
tasks.withType<JavaCompile> {
  options.encoding = "UTF-8"
  options.compilerArgs.add("-parameters")
  options.errorprone {
    disableWarningsInGeneratedCode = true
    ignoreUnknownCheckNames = true
    errorproneArgs = listOf("-XepExcludedPaths:.*/build/classes/java/quarkus-generated-sources/.*")
  }
}

sourceSets {
  main {
    java.setSrcDirs(listOf("build/classes/java/quarkus-generated-sources/grpc", "src/main/java"))
  }
}
