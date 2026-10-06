package client.cn.kafei.simukraft.client.renderer;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;

public class CitizenModel extends HumanoidModel<CitizenRenderState> {
    public final ModelPart leftSleeve;
    public final ModelPart rightSleeve;
    public final ModelPart leftPants;
    public final ModelPart rightPants;
    public final ModelPart jacket;
    private final boolean slim;

    public CitizenModel(ModelPart root, boolean slim) {
        super(root, RenderTypes::entityTranslucent);
        this.slim = slim;
        this.leftSleeve = this.leftArm.getChild("left_sleeve");
        this.rightSleeve = this.rightArm.getChild("right_sleeve");
        this.leftPants = this.leftLeg.getChild("left_pants");
        this.rightPants = this.rightLeg.getChild("right_pants");
        this.jacket = this.body.getChild("jacket");
    }

    @Override
    public void setupAnim(CitizenRenderState state) {
        super.setupAnim(state);
        if (state.workSwing) {
            CitizenAnimationActions.applyBuilderWorkSwing(this, state.ageInTicks);
        }
        copyPart(this.leftArm, this.leftSleeve);
        copyPart(this.rightArm, this.rightSleeve);
        copyPart(this.leftLeg, this.leftPants);
        copyPart(this.rightLeg, this.rightPants);
        copyPart(this.body, this.jacket);
    }

    public static void copyPart(ModelPart from, ModelPart to) {
        to.x = from.x;
        to.y = from.y;
        to.z = from.z;
        to.xRot = from.xRot;
        to.yRot = from.yRot;
        to.zRot = from.zRot;
        to.xScale = from.xScale;
        to.yScale = from.yScale;
        to.zScale = from.zScale;
        to.visible = from.visible;
    }

    public boolean slim() {
        return slim;
    }
}
