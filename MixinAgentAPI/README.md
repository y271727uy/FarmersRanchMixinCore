# MixinAgentAPI

超级底层的加载通道。字节码怎么改在 `MixinASMAPI`，这一层只负责在类定义前插进去。

1. **Java Agent**（`SubstratumAgent`，`-javaagent:MixinAgentAPI.jar`）
   能看到每一次 `defineClass`，包括已经加载路径上的 `net.minecraftforge.fml.loading`、`cpw.mods.modlauncher`、`cpw.mods.bootstraplauncher`。类已经加载后只能 `retransform` 方法体。
2. **Launch Plugin 侵入**
   把 `frmc_substratum` 塞进 `LaunchPluginHandler.plugins`。之后经过 TransformingClassLoader 的类都能改，包括 `net.minecraftforge.common` 和 `net.minecraftforge.fml`。
3. **模块打开 + 静态字段覆写**
   `ModuleOpener` 把 Forge/FML/ModLauncher 的包 opens 出来，`DeepFields` 用 `Unsafe` 写 `static final`。

`MixinCoremodAPI` 的 `ITransformationService` 会在 `onLoad` 里调用 `Substratum.install()`。游戏层的 Mixin 配置插件也会再调一次。Agent、服务层、游戏层各有一份类，登记的改写通过 bootstrap 里的 `substratum-bridge.jar` 共享。

```java
ForgeFmlHooks.edit("my.fml.hook", "net.minecraftforge.fml.loading.FMLLoader", node ->
        MethodEdits.setBooleanConstant(node, "isProduction", "()Z", true));
```

`frmc.substratum.forceFmlProduction=true` 是默认关闭的探针。`frmc.substratum.trace=true` 只打印经过 agent 的 Forge/FML/ModLauncher 类名。
