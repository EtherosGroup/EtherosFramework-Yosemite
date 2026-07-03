plugins {
    id("java")
    id("signing")
    id("com.vanniktech.maven.publish") version "0.36.0"
}

group = "cn.skilfully.etheros"
version = "1.2.0"
description = "Etheros Framework - Yosemite (Java 17 / Minecraft 1.17\u20131.20.4)"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withSourcesJar()
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    implementation("org.ow2.asm:asm:9.3")

    val lombok = "org.projectlombok:lombok:1.18.36"
    compileOnly(lombok)
    annotationProcessor(lombok)
    testCompileOnly(lombok)
    testAnnotationProcessor(lombok)
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc> {
    options.encoding = "UTF-8"
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
}

mavenPublishing {
    publishToMavenCentral(true)
    signAllPublications()

    coordinates("cn.skilfully.etheros", "EtherosFramework-Yosemite", project.version.toString())

    pom {
        name = rootProject.name
        description = project.description
        url = "https://github.com/skilfully/EtherosFramework-Yosemite"

        licenses {
            license {
                name = "Apache License 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0"
            }
        }

        developers {
            developer {
                id = "Etheros Group"
                name = "Etheros Group"
            }
        }

        scm {
            connection = "scm:git:https://github.com/EtherosGroup/EtherosFramework-Yosemite.git"
            developerConnection = "scm:git:https://github.com/EtherosGroup/EtherosFramework-Yosemite.git"
            url = "https://github.com/EtherosGroup/EtherosFramework-Yosemite"
        }
    }
}

signing {
    useGpgCmd()
}

afterEvaluate {
    tasks.matching { it.name == "generateMetadataFileForMavenPublication" }.configureEach {
        dependsOn(tasks.matching { it.name == "plainJavadocJar" })
    }
}
