import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
}

kotlin {
    jvm()
    
    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            implementation(libs.material.icons.extended)

        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
        }
    }
}


// Add version info to desktop builds
val desktopVersion = "1.0.3"

compose.desktop {
    application {
        mainClass = "org.lerchenflo.xmllanguagetranslator.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Rpm)
            packageName = "XML Language Translator"
            packageVersion = desktopVersion
            copyright = "© 2026"
            vendor = "lerchenflo"
            modules("jdk.unsupported")

            linux {
                // Linux override. Keep this strictly lowercase with no spaces.
                packageName = "xml-language-translator"

                shortcut = true
                menuGroup = "Development"
            }

            windows {
                perUserInstall = true

                upgradeUuid = "b074e742-a5ba-4b0d-af60-db09ef6efe56"

                menu = true
                shortcut = true

                // ./gradlew packageDistributionForCurrentOS
            }

            macOS {
                // Pro-tip: set a bundleID without spaces here, otherwise macOS might try to
                // generate one from a spaced packageName and fail.
                bundleID = "org.lerchenflo.xmllanguagetranslator"
            }
        }
        buildTypes.release.proguard {
            isEnabled.set(false) // disable ProGuard
        }
    }
}

tasks.withType<Jar> {
    manifest {
        attributes(
            "Implementation-Title" to "XmlLanguageTranslator",
            "Implementation-Version" to desktopVersion,
            "Implementation-Vendor" to "lerchenflo"
        )
    }
}

tasks.register<DefaultTask>("runDesktop") {
    group = "application"
    description = "Runs the Compose Desktop app"

    dependsOn("run") // reuse the Compose Desktop run task
}
