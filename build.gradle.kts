import dev.prism.gradle.dsl.ReleaseType

plugins {
    id("dev.prism")
}

group = "com.leclowndu93150"
version = "2.2.0"

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
        changelog = """
            - Added EMI support on 1.20.1 and 1.21.1
            - Fixed a crash to the main menu on 1.21.1 when moving items into a chest next to the station.
            - Fixed the side inventory using the creative inventory textures. The slots and scrollbar now use the mod's own texture and can be resource packed independently.
            - Fixed scrollbar dragging being offset, grabbing on clicks anywhere in the screen, and leaving the grid misaligned. Scrolling now moves in whole rows.
            - Fixed item loss and apparent duplication with over-stacked slots such as Sophisticated Storage barrels with a Stack Upgrade. The real count is now shown and taking, swapping, and dragging no longer discard the surplus.
            - Fixed a duplication on 26.1 when shift-clicking from an adjacent container into a nearly full inventory.
        """.trimIndent()

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
            optional("functional-storage")
            optional("emi")
        }
    }

    version("1.19.2") {
        forge {
            loaderVersion = "43.5.0"
            loaderVersionRange = "[43,)"
            dependencies {
                modImplementation("curse.maven:jei-238222:5846858")
                modRuntimeOnly("curse.maven:iron-chests-228756:3966365")
                modCompileOnly("curse.maven:polymorph-388800:5222155")
                modImplementation("curse.maven:balm-531761:4751735")
                modImplementation("curse.maven:crafting-tweaks-233071:4939198")
                modImplementation("curse.maven:titanium-287342:5356458")
                modImplementation("curse.maven:functional-storage-556861:5499169")
            }
        }
    }

    version("1.20.1") {
        forge {
            loaderVersion = "47.4.16"
            loaderVersionRange = "[47,)"
            dependencies {
                modCompileOnly("curse.maven:jei-238222:7391695")
                modImplementation("curse.maven:emi-580555:8081375")
                modRuntimeOnly("curse.maven:iron-chests-228756:4614852")
                modCompileOnly("curse.maven:polymorph-388800:6450982")
                modImplementation("curse.maven:balm-531761:7420617")
                modImplementation("curse.maven:crafting-tweaks-233071:7454498")
                modImplementation("curse.maven:titanium-287342:5468426")
                modImplementation("curse.maven:functional-storage-556861:6702553")
            }
        }
    }

    version("1.21.1") {
        neoforge {
            loaderVersion = "21.1.90"
            loaderVersionRange = "[4,)"
            dependencies {
                compileOnly("curse.maven:jei-238222:7391682")
                implementation("curse.maven:emi-580555:8081408")
                runtimeOnly("curse.maven:iron-chests-228756:5491156")
                compileOnly("curse.maven:polymorph-388800:6794589")
                implementation("curse.maven:balm-531761:7420963")
                implementation("curse.maven:crafting-tweaks-233071:7530379")
                implementation("curse.maven:titanium-287342:7662843")
                implementation("curse.maven:functional-storage-556861:7061714")
            }
        }
    }

    version("26.1.1") {
        minecraftVersions("26.1","26.1.1","26.1.2")
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
