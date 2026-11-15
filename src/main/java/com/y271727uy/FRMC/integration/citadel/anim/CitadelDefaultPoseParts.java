package com.y271727uy.FRMC.integration.citadel.anim;

import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** 把 Citadel 模型的部件列表固定成数组，供每帧的默认姿态重置直接遍历。 */
public final class CitadelDefaultPoseParts {
    /** 部件列表连续这么多次完全相同之后才信任缓存，避免缓存构造期间还在变化的列表。 */
    public static final int REQUIRED_STABLE_LISTINGS = 3;

    private CitadelDefaultPoseParts() {
    }

    public static AdvancedModelBox[] list(Iterable<AdvancedModelBox> parts) {
        if (parts instanceof Collection<AdvancedModelBox> collection) {
            return collection.toArray(new AdvancedModelBox[0]);
        }
        List<AdvancedModelBox> listed = new ArrayList<>();
        for (AdvancedModelBox part : parts) {
            listed.add(part);
        }
        return listed.toArray(new AdvancedModelBox[0]);
    }

    public static boolean sameParts(AdvancedModelBox[] first, AdvancedModelBox[] second) {
        if (first == null || second == null || first.length != second.length) {
            return false;
        }
        for (int i = 0; i < first.length; i++) {
            if (first[i] != second[i]) {
                return false;
            }
        }
        return true;
    }
}
