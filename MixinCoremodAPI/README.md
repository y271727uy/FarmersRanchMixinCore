# MixinCoremodAPI

Forge 1.20.1 的 coremod，也就是 ModLauncher 的 `ITransformationService`。服务名是 `frmc_coremod_api`。

它只改 `TransformingClassLoader` 之后才加载的类，另外把 `CoremodState.isActive()` 改成 `true`，用来确认服务已经跑起来。

改 `net.minecraftforge.fml.loading` 和 `cpw.mods.modlauncher` 不在这里。那些类在这个服务出现之前就已经加载了，走 `MixinAgentAPI` 的 Java Agent。字节码工具在 `MixinASMAPI`。

`onLoad` 会调用 `Substratum.install()`，把 Launch Plugin 插进 ModLauncher。
