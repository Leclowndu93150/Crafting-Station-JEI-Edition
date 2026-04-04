plugins {
    id("dev.prism")
}

group = "com.leclowndu93150"
version = "1.0.0"

prism {
    metadata {
        modId = "craftingstationjei"
        name = "Crafting Station JEI"
        description = "A crafting station block that can access adjacent inventories and integrates with JEI."
        license = "MIT"
        author("Leclowndu93150")
    }

    curseMaven()

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
}
