// Just the public NilumAPI interface, so other plugins can compile against it without depending
// on the whole nilum-paper plugin jar. At runtime, the actual class comes from nilum-paper's own
// jar (which bundles this module), same pattern Vault/PlaceholderAPI use for their own APIs.

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
}
