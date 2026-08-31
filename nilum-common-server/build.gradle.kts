// Server-only shared logic: TCP asset/HUD/icon/shaderpack/font/block hosting, model/block registries, collision derivation.

dependencies {
    implementation(project(":nilum-common"))
    compileOnly("com.google.code.gson:gson:2.11.0")
}
