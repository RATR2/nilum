// Shared, loader-agnostic logic: protocol, asset cache, .bbmodel parser, HUD atlas, expression evaluator, shader pipeline abstraction, trust model.

// compileOnly, not bundled: Gson is already on every platform's runtime classpath.
dependencies {
    compileOnly("com.google.code.gson:gson:2.11.0")
}
