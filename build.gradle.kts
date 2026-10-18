import dev.prism.gradle.dsl.ReleaseType

plugins {
    id("dev.prism")
}

group = "com.leclowndu93150"
version = "2.2.1"

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
            - Added support for 26.2 and 26.3. The 26.1 version now targets 26.1.2.
            - Fixed items being voided on 26.1 when JEI or a click put items into an adjacent container that doesn't accept all of them.
            - Fixed a server freeze when using JEI max transfer with an adjacent container that shows items but doesn't allow taking them out.
            - Fixed modded chests, such as Every Compat's Woodworks chests, not being detected as adjacent inventories on 1.21.1 and 26.1.
            - Fixed disconnects on 26.1 when the client and server disagreed on adjacent inventories, for example next to multiblocks like the Atomic Forge. Adjacent inventory sync is also stricter on all versions.
            - Fixed item loss and duplication when using number keys to swap items with an adjacent container slot.
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

    version("26.1.2") {
        neoforge {
            loaderVersion = "26.1.2.122"
            loaderVersionRange = "[4,)"
            dependencies {
                implementation("curse.maven:jei-238222:9205181")
                implementation("curse.maven:balm-531761:8969765")
                implementation("curse.maven:crafting-tweaks-233071:8697035")
            }
        }
    }

    version("26.2") {
        neoforge {
            loaderVersion = "26.2.0.93"
            loaderVersionRange = "[4,)"
            dependencies {
                implementation("curse.maven:jei-238222:9205166")
                implementation("curse.maven:balm-531761:8969714")
                implementation("curse.maven:crafting-tweaks-233071:8697034")
            }
        }
    }

    version("26.3") {
        neoforge {
            loaderVersion = "26.3.0.90"
            loaderVersionRange = "[4,)"
            dependencies {
                implementation("curse.maven:jei-238222:9235396")
                implementation("curse.maven:balm-531761:9035246")
                implementation("curse.maven:crafting-tweaks-233071:8893528")
            }
        }
    }
}
