package com.y271727uy.FRMC.mixin.acceleratedrendering.mesh;

import com.github.argon4w.acceleratedrendering.core.meshes.ServerMesh;
import com.y271727uy.FRMC.integration.citadel.render.ar.CitadelGpuMaterialMeshes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(value = ServerMesh.Builder.class, remap = false)
public abstract class ServerMeshBuilderCitadelMixin {
    @Inject(method = {"delete", "reload"}, at = @At("HEAD"))
    private void frmc$releaseMaterialMeshes(CallbackInfo callback) {
        CitadelGpuMaterialMeshes.release();
    }
}
