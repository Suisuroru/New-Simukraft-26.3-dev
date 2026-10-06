package common.cn.kafei.simukraft.path;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

public record PathCacheKey(Identifier dimensionId, BlockPos startPos, BlockPos targetPos, MovementIntent intent) {
    public static PathCacheKey of(PathRequest request, BlockPos normalizedStart, BlockPos normalizedTarget) {
        return new PathCacheKey(request.dimensionId(), normalizedStart, normalizedTarget, request.intent());
    }
}
