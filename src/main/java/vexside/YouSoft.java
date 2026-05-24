buildscript {
    repositories {
        mavenCentral()
        maven { url = 'https://maven.minecraftforge.net/' }
    }
    dependencies {
        classpath group: 'net.minecraftforge.gradle', name: 'ForgeGradle', version: '5.1.+', changing: true
    }
}
apply plugin: 'net.minecraftforge.gradle'

version = '1.0'
group = 'vexside'
archivesBaseName = 'YouSoft'

java.toolchain.languageVersion = JavaLanguageVersion.of(8)

minecraft {
    mappings channel: 'official', version: '1.16.5'
    runs {
        client {
            workingDirectory project.file('run')
            property 'forge.logging.mojanglevels', 'info'
            mods {
                yousoft {
                    source sourceSets.main
                }
            }
        }
    }
}

dependencies {
    implementation 'com.google.code.gson:gson:2.8.9'
    minecraft 'net.minecraftforge:forge:1.16.5-36.2.34'
}

jar {
    manifest {
        attributes([
            "Specification-Title": "yousoft",
            "Specification-Vendor": "yousoft",
            "Specification-Version": "1",
            "Implementation-Title": project.name,
            "Implementation-Version": project.jar.archiveVersion,
            "Implementation-Vendor" :"yousoft"
        ])
    }
}
