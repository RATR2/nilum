# libs/

We don't ship full builds of other people's projects inside this repo; third-party mod jars
needed at compile time go here locally, but stay gitignored.

Before compiling `nilum-fabric`, download the tested version of Iris and place it in this
folder:

```
iris-fabric-1.10.7+mc1.21.11.jar
```

Get it from the official Iris mod page (Modrinth/CurseForge). Newer or older builds may not
match what `nilum-fabric`'s Iris integration was written and tested against.
