// Client-only shared logic: asset cache/client, in-memory model store, model baking/posing, TCP client.

dependencies {
    implementation(project(":nilum-common"))
    compileOnly("com.google.code.gson:gson:2.11.0")
}
