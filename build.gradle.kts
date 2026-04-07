import dev.prism.gradle.dsl.ReleaseType

plugins {
    id("dev.prism")
}

group = "com.leclowndu93150"
version = "2.0.0"

prism {
    metadata {
        modId = "craftingstationjei"
        name = "Crafting Station JEI"
        description = "A crafting station block that can access adjacent inventories and integrates with JEI."
        license = "MIT"
        author("Leclowndu93150")
    }

    curseMaven()

    publishing {
        type = STABLE
        changelog = "Added Crafting Tweaks and Polymorph Support."

        curseforge {
            accessToken = providers.environmentVariable("CURSEFORGE_TOKEN")
            projectId = "1127715"
        }

        modrinth {
            accessToken = providers.environmentVariable("MODRINTH_TOKEN")
            projectId = "rc8HlDUK"
        }

        dependencies {
            requires("jei")
            optional("polymorph")
            optional("crafting-tweaks")
        }
    }

    version("1.20.1") {
        forge {
            loaderVersion = "47.4.16"
            loaderVersionRange = "[47,)"
            dependencies {
                modImplementation("curse.maven:jei-238222:7391695")
                modRuntimeOnly("curse.maven:iron-chests-228756:4614852")
                modCompileOnly("curse.maven:polymorph-388800:6450982")
                modImplementation("curse.maven:balm-531761:7420617")
                modImplementation("curse.maven:crafting-tweaks-233071:7454498")
            }
        }
    }

    version("1.21.1") {
        neoforge {
            loaderVersion = "21.1.90"
            loaderVersionRange = "[4,)"
            dependencies {
                implementation("curse.maven:jei-238222:7391682")
                runtimeOnly("curse.maven:iron-chests-228756:5491156")
                compileOnly("curse.maven:polymorph-388800:6794589")
                implementation("curse.maven:balm-531761:7420963")
                implementation("curse.maven:crafting-tweaks-233071:7530379")
            }
        }
    }

    version("26.1.1") {
        neoforge {
            loaderVersion = "26.1.1.10-beta"
            loaderVersionRange = "[4,)"
            dependencies {
                implementation("curse.maven:jei-238222:7884734")
                implementation("curse.maven:balm-531761:7842250")
                implementation("curse.maven:crafting-tweaks-233071:7806683")
            }
        }
    }
}
