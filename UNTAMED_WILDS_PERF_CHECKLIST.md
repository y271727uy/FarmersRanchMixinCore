# Untamed Wilds performance checklist

## Must-do first
- [x] Stop spawn-list duplication on reload/login
- [x] Avoid re-registering entity data on every player login
- [x] Cache entity textures instead of resolving them every render frame
- [x] Reduce repeated `getEntityData()` lookups in hot paths
- [x] Throttle large AI entity scans (`HuntMobTarget`, `ProtectChildrenTarget`, `SmartLookAtGoal`)
- [x] Keep `FindItemsGoal` safe against invalid targets while reducing repeated checks
- [x] Clean up and throttle herd maintenance / combine scans

## Already implemented
- `untamed_wilds.HerdEntityMixin`
- `untamed_wilds.ai.FindItemsGoalMixin`
- `untamed_wilds.ai.SmartLookAtGoalMixin`
- `untamed_wilds.ai.target.HuntMobTargetMixin`
- `untamed_wilds.ai.target.ProtectChildrenTargetMixin`
- `untamed_wilds.util.SpawnDataListenerEventMixin`
- `untamed_wilds.util.EntityDataListenerEventMixin`
- `untamed_wilds.entity.ComplexMobTextureCacheMixin`
- `untamed_wilds.entity.mammal.EntityBigCatTextureCacheMixin`

## Optional later
### Already applied safe optional micro-optimizations
- [x] Replace modulo-style tick throttles with countdown-based throttles
- [x] Reduce repeated accessor calls in herd validation and texture-cache paths
- [x] Keep BigCat texture-cache lookups defensive with `@Dynamic`

### Still optional later
- [ ] `getEntitiesOfClass()` call-site tuning in non-critical goals
- [ ] `Math.sqrt()` / `Math.pow()` squared-distance cleanups
- [ ] `Vec3` array reuse in multipart entities
- [ ] Herd set-based membership optimization
- [ ] Render LOD / far-distance simplification
- [ ] Despawn policy redesign

