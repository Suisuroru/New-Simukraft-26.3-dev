package client.cn.kafei.simukraft.client.renderer;

import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

import java.util.List;

public class CitizenRenderState extends HumanoidRenderState {
    public Identifier texture = CitizenRenderer.DEFAULT_TEXTURE;
    public boolean workSwing;
    public boolean childNpc;
    public int npcAge = 18;
    public String pregnancyStage = "";
    public boolean hideOverhead;
    public List<CitizenOverheadStatusRegistry.StatusLine> overheadLines = List.of();
}
