package com.y271727uy.FRMC.integration.citadel.tick;

import com.github.alexthe666.citadel.Citadel;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 用 HashSet 替代对 Citadel.PATREONS 的线性 List.contains。 */
public final class PatreonIndex {
    private static volatile Set<String> index = Set.of();
    private static volatile int indexedSize = -1;

    private PatreonIndex() {}

    public static boolean isPatreon(String username) {
        List<String> source = Citadel.PATREONS;
        int size = source.size();
        if (size == 0) {
            return false;
        }
        Set<String> local = index;
        if (indexedSize != size) {
            try {
                local = new HashSet<>(source);
                index = local;
                indexedSize = size;
            } catch (RuntimeException stillBeingFilled) {
                // 列表正被后台线程填充时快照可能失败，退回线性查找。
                return source.contains(username);
            }
        }
        return local.contains(username);
    }

    public static boolean isEmpty() {
        return Citadel.PATREONS.isEmpty();
    }
}
