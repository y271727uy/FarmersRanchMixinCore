package com.y271727uy.FRMC.mixin.acceleratedrendering.mesh;

import com.github.argon4w.acceleratedrendering.core.meshes.ServerMesh;
import com.github.argon4w.acceleratedrendering.core.programs.dispatchers.meshes.DenseUploads;
import com.github.argon4w.acceleratedrendering.core.programs.dispatchers.meshes.MeshUploadingProgramDispatcher;
import com.github.argon4w.acceleratedrendering.core.programs.overrides.IUploadingOverride;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.y271727uy.FRMC.integration.citadel.render.ar.CitadelGpuMaterialMeshes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(value = MeshUploadingProgramDispatcher.class, remap = false)
public abstract class MeshUploadingDispatcherCitadelMixin {
    @WrapOperation(method = "dispatch", at = @At(value = "INVOKE", remap = false,
            target = "Lcom/github/argon4w/acceleratedrendering/core/programs/dispatchers/meshes/DenseUploads$Group;getMesh()Lcom/github/argon4w/acceleratedrendering/core/meshes/ServerMesh;"))
    private ServerMesh frmc$captureDenseMesh(DenseUploads.Group group, Operation<ServerMesh> original,
                                             @Share("frmc$citadelMesh") LocalRef<ServerMesh> current) {
        ServerMesh mesh = original.call(group);
        current.set(mesh);
        return mesh;
    }

    @WrapOperation(method = "dispatch", at = @At(value = "INVOKE", remap = false,
            target = "Lcom/github/argon4w/acceleratedrendering/core/programs/overrides/IUploadingOverride;dispatchUploading(III)I"))
    private int frmc$uploadCitadelMesh(IUploadingOverride uploader, int count, int size, int offset,
                                       Operation<Integer> original,
                                       @Share("frmc$citadelMesh") LocalRef<ServerMesh> current) {
        ServerMesh mesh = current.get();
        if (CitadelGpuMaterialMeshes.handles(mesh)) {
            return CitadelGpuMaterialMeshes.dispatch(mesh, count, size, offset);
        }
        return original.call(uploader, count, size, offset);
    }
}
