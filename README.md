
Installation information
=======

This template repository can be directly cloned to get you started with a new
mod. Simply create a new repository cloned from this one, by following the
instructions provided by [GitHub](https://docs.github.com/en/repositories/creating-and-managing-repositories/creating-a-repository-from-a-template).

Once you have your clone, simply open the repository in the IDE of your choice. The usual recommendation for an IDE is either IntelliJ IDEA or Eclipse.

If at any point you are missing libraries in your IDE, or you've run into problems you can
run `gradlew --refresh-dependencies` to refresh the local cache. `gradlew clean` to reset everything 
{this does not affect your code} and then start the process again.

Mapping Names:
============
The MDK is configured to use the official mapping names from Mojang for methods and fields 
in the Minecraft codebase. These names are covered by a specific license. All modders should be aware of this
license. For the latest license text, refer to the mapping file itself, or the reference copy here:
https://github.com/NeoForged/NeoForm/blob/main/Mojang.md

MDG Legacy:
==========
This template uses [ModDevGradle Legacy](https://github.com/neoforged/ModDevGradle). Documentation can be found [here](https://github.com/neoforged/ModDevGradle/blob/main/LEGACY.md).

Additional Resources: 
==========
Community Documentation: https://docs.neoforged.net/  
NeoForged Discord: https://discord.neoforged.net/

Mixin blacklist config
==========

FRMC now creates a dedicated config file at `config/frmc-mixin-blacklist.toml` during startup.

Use `blacklisted_mixins` to disable specific external mixins by fully-qualified class name. Prefix wildcards ending in `*` are also supported.

Example:

```toml
blacklisted_mixins = [
	"example.mod.mixin.SomeSpecificMixin",
	"example.mod.mixin.problematic.*"
]
```

Recipe search config
==========

FRMC includes a source-level port of the `RecipeSearch 1.3` search tree and the
Forge integration from `Fast-Recipe-Search-forge-1.20`. No nested RecipeSearch
JAR is bundled. Recipe integration settings are created at
`config/frmc-recipe-search.properties` during startup.

The compatibility-safe upstream defaults are:

```properties
enable=true
optimize_only_vanilla=true
ingredient_sync=false
ingredient_deduplicator=false
```

`enable` controls all recipe-search mixins. `optimize_only_vanilla` limits the
optimized database to vanilla recipe types. Ingredient network encoding and
ingredient deduplication are separate optional optimizations and remain off by
default because every connected side or affected mod must be compatible.

The ported implementation is distributed under the GNU Lesser General Public
License, version 3 or later. Its license and attribution are included in
`third_party/` and in the built mod JAR.

