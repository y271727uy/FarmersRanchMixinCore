参考内容：
Low storage and efficient 3D reverse time migration method based on eikonal equation
Jing Wang , Qingqing Li , Hongwei Liu , Li-Yun Fu
Journal of Geophysics and Engineering, gxag124, https://doi.org/10.1093/jge/gxag124
Published: 25 September 2026

借鉴了论文“缓存可复用结果、避免重复计算”的思路，把 Citadel 每个骨骼的三次独立轴旋转合并成一次 ZYX 四元数旋转，并在角度不变时复用缓存

测试环境：1.20.1 forge 47.4.22，java 17  
前置依赖：acceleratedrendering-1.0.14-1.20.1-alpha、embeddium-0.3.31+mc1.20.1    
测试对象：超平坦环境，和平模型，单区块256只uw模组的big cat实体

| 模型        | 骨骼节点 | 有几何的节点 | 空节点 | 最大层级 |
|-----------| --- | --- | --- | --- |
| BigCat 成年 | 35 | 35 | 0 | 6 |
| BigCat 幼年 | 14 | 14 | 0 | 3 |

由于当前统计日志尚未可靠地区分成年 BigCat 与幼年 BigCat 的实际数量，以下工作量估算统一按幼年模型的骨骼规模进行保守估计。幼年 BigCat 模型包含 14 个骨骼节点，因此 256 只实体对应约 3,584 个骨骼节点/完整渲染轮。


