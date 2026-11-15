# Xaero 地图性能补丁（首批）

目标依赖：Minecraft 1.20.1 / Forge，Xaero World Map 1.45.0，AR 接口核对版本为 Accelerated Rendering 1.0.14。

客户端首次启动后生成 `config/frmc/frmc-xaero.toml`。所有选项独立；下列是默认值。

| 配置项 | 默认值 | 作用 |
| --- | --- | --- |
| `lazy_height_arrays` | `true` | 空地图瓦片共享初始高度数组，首次非零写入或外露底层数组时才分离；不修改高度数据格式。 |
| `skip_upload_benchmark` | `true` | 跳过地图启动期上传测速中的 GPU 同步。保留已有真实样本；未采样时沿用 Xaero 的未测量预算（普通类型 1 ms，类型 3 为 3 ms，类型 4 为 4 ms）。调试显示中的未测量耗时是预算估计，不是实测性能。 |
| `deduplicate_layer_marks` | `true` | 同一次 MapWriter.onRender 中，避免连续对同一 manager、同一层和同一区域重复标记其他洞穴层。 |
| `vram_poll_interval_ms` | `500` | 限制显存余量查询频率；`0` 恢复逐帧查询。不会限制地图瓦片更新频率。 |
| `orphan_upload_pbo` | `true` | 完整替换地图瓦片前丢弃旧 PBO 存储，沿用 Xaero 的映射、GL/ARB 分派与失败回退。驱动异常时先关闭此项。 |
| `ar_batch_pressure_overlay` | `true` | AR 存在且其 GUI 批处理允许时，批量提交 FRMC 区块压力覆盖层矩形。尊重 AR 自身设置；不替换 Xaero 的原生多纹理地图渲染器。 |

五个 Xaero 本体 Mixin 只在 World Map 1.45.0 时加载；检测到 `collections_of_optimizations` 时让出这些补丁。FRMC 原有地图 UI 集成仍保留。没有安装 Xaero 时不加载这些 Mixin；没有安装 AR 时不加载 AR 桥接实现。关闭高度数组优化不会使已创建的懒数组失去写时隔离。

## 验证场景

1. 固定视距、资源包、地图设置和帧率上限，在同一路线探索新区块，观察帧时间尖峰与分配/GC；不要仅看平均 FPS。
2. 打开世界地图，测试缩放、拖拽、地表/洞穴层切换和传送后的瓦片更新；退出重进确认高度着色、地图缓存读写一致。
3. 分别启用和关闭 `orphan_upload_pbo`，观察纹理是否完整、是否存在驱动报错或上传卡顿。
4. 开启 FRMC 地图压力覆盖层，积累较多区块数据后测试 AR 批处理；检查矩形颜色、剪裁、鼠标提示层级，并在 AR 自身 GUI batching 关闭或无 AR 的环境确认回退。
5. 对照时一次只改变一个开关；最好重启客户端，尤其是高度数组分配和启动测速相关选项。

源码编译与字节码/行为测试不能代替显卡驱动和游戏内 A/B 测试。本次没有降低小地图/大地图的默认绘制或瓦片处理频率，没有修改 waypoint 数据，也没有重写 Xaero 的多纹理 shader。
