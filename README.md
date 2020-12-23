# Modsman

A mod manager for Minecraft. It can download, manage, and update mods and resource packs from CurseForge, as well as [inflate MultiMC modpacks by downloading mods on first run](https://github.com/sargunv-mc-mods/modsman-modpack-example).

CLI releases are available on [Brew for macOS and Linux](https://brew.sh/), or [Scoop for Windows](https://scoop.sh/).

```bash
# Install with Brew
brew tap sargunv/sargunv
brew install modsman
modsman-cli --help
```


```powershell
# Install with Scoop
scoop bucket add sargunv https://github.com/sargunv/scoop-sargunv.git
scoop install modsman
modsman-cli --help
```

If you do not wish to use a package manager, you may download modsman from [GitHub Releases](https://github.com/sargunv/modsman/releases).

## Usage

Some basic knowledge of how to use the command line on your system is expected. The following commands will assume you're using Bash or PowerShell, but everything except the wildcard applies to CMD as well.

### Initialize Modsman

```bash
cd .minecraft/mods
modsman-cli init -R 1.15 -X Forge

# or, without changing the working directory:
modsman-cli -M .minecraft/mods init -R 1.15 -X Forge
```

Adding `-R 1.15` flag tells Modsman you want mods for MC 1.15.x, and `-X Forge` tells Modsman you *don't* want it to download jars labelled as Forge mods on CurseForge. Both of these refer to CurseForge's version tags, and Modsman matches by substring (so 1.15.2 on CF matches 1.15 in Modsman). You can specify as many of `-R` or `-X` as you want. If you want to modify these after the fact, you can edit your *.modlist.json* directly. After the above init command, it looks like this:

```json
{
  "config": {
    "required_game_versions": [
      "1.15"
    ],
    "excluded_game_versions": [
      "Forge"
    ]
  },
  "mods": [
  ]
}
```

All of the following instructions assume your current working directory is your *.minecraft/mods* directory where you've already initialized Modsman.

### Add mods or discover already installed mods

You probably already have some mods from CurseForge in your mods directory. You can have Modsman automatically match them up to their CurseForge listing and track them in its modlist:

```bash
modsman-cli discover fabric-api.jar modmenu.jar

# Or, to discover all your jars
modsman-cli discover *.jar
```

To have modsman install mods for you, use the CurseForge project ID:

```bash
modsman-cli add 306612
```

Now, if you list your mods, you'll see Modsman is aware of your mods:

```bash
modsman-cli list
```

### Update tracked mods

This is easy:

```bash
modsman-cli upgrade-all

# or, for specific mods
modsman-cli upgrade 306612
```

For the specific-mod example, pass the CurseForge project IDs. The mod version resolution is based on the config in the .modlist.json, which we set up with whitelisted/blacklisted MC versions in the first step.

Modsman supports pinning versions to exclude them from updating and update-checking.

```bash
modsman-cli pin 306612
modsman-cli unpin 306612
```

I reccommend just pinning the versions you want to freeze and running upgrade-all.

To check for updates:

```bash
modsman-cli list-outdated
```

### Next steps

The above gives you an overview of Modsman's core functionality, including installing, discovering, and updating mods. For full instructions and features, use the built in help: `modsman-cli --help`.
