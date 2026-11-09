# DonutPay (Fabric, Minecraft 26.2)

Client-side mod for DonutSMP:
- `NAME paid you $X` / `You paid NAME $X` -> name in orange, amount in lime green
- Flashy popup with the payer's skin face when someone pays YOU 5B or more (small/quick if several are queued)
- "Profit of the day" box under the right scoreboard
  - **J** = start/pause counting, **K** = reset (change both in Options > Controls > Key Binds > DonutPay)

## Build it (beginner steps)
1. Install **JDK 25** (e.g. Eclipse Temurin 25).
2. Go to https://fabricmc.net/develop/template/ , pick Minecraft **26.2**, mod name `DonutPay`, mod ID `donutpay`, package `com.donutpay`, download and unzip it.
   (This gives you the Gradle wrapper and the right version numbers.)
3. In the unzipped template, DELETE its `src` folder and copy this project's `src` folder in its place.
4. Open a terminal in that folder and run:
   - Windows: `gradlew.bat build`
   - Mac/Linux: `./gradlew build`
5. Your mod is `build/libs/donutpay-1.0.0.jar` (NOT the `-sources` jar).
6. Put that jar plus **Fabric API** into `.minecraft/mods` (Fabric Loader 0.19+, Java 25).

If the build reports an error, copy the message and send it back; Minecraft 26.x is very new and a method name may need a tiny fix.

## Build on GitHub (no installs needed)
1. Create a new GitHub repository and upload everything from this folder (including the hidden `.github` folder).
2. Open the **Actions** tab. The "Build DonutPay" workflow runs automatically on every push (or press **Run workflow**).
3. When it finishes (green check), open the run and download **donutpay-mod** under *Artifacts*. Unzip it and put the jar in your mods folder.
4. Optional: create a tag like `v1.0.0` to also get a downloadable Release.
