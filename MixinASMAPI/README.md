# MixinASMAPI

只负责改类字节码，不负责什么时候加载。

`ClassEditor` 改一个 `ClassNode`。`MethodEdits` 提供改方法体的小工具。`Bytecodes` 负责读、写，以及打上 `Applied` 标记。同一个类被 Agent 和 Launch Plugin 各看一次时，第二次看到标记就跳过。

Java Agent、Launch Plugin、coremod 都调用这一层，不要把加载逻辑放进来。
