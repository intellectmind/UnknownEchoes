package cn.kurt6.unknown_echoes.client.model;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.LivingEntity;

/**
 * 回响套装的客户端厚甲模型。
 * 仍继承 HumanoidModel 以保留原版动作,但使用额外几何做外扩头盔、肩甲和分段腿甲。
 */
public class EchoArmorModel<T extends LivingEntity> extends HumanoidModel<T> {

    private EchoArmorModel(ModelPart root) {
        super(root);
    }

    public static EchoArmorModel<LivingEntity> outer() {
        return new EchoArmorModel<>(createLayer(new CubeDeformation(0.68F), false).bakeRoot());
    }

    public static EchoArmorModel<LivingEntity> inner() {
        return new EchoArmorModel<>(createLayer(new CubeDeformation(0.34F), true).bakeRoot());
    }

    private static LayerDefinition createLayer(CubeDeformation deformation, boolean inner) {
        MeshDefinition mesh = HumanoidModel.createMesh(deformation, 0.0F);
        PartDefinition root = mesh.getRoot();
        addHelmet(root, deformation, inner);
        addChest(root, deformation, inner);
        addArms(root, deformation, inner);
        addLegs(root, deformation, inner);
        return LayerDefinition.create(mesh, 128, 64);
    }

    private static void addHelmet(PartDefinition root, CubeDeformation deformation, boolean inner) {
        if (inner) {
            return;
        }
        PartDefinition head = root.getChild("head");
        CubeDeformation crest = deformation.extend(0.22F);
        head.addOrReplaceChild("echo_face_guard",
                CubeListBuilder.create().texOffs(64, 0)
                        .addBox(-4.6F, -5.2F, -4.9F, 9.2F, 4.4F, 1.0F, crest),
                PartPose.ZERO);
        head.addOrReplaceChild("echo_left_horn",
                CubeListBuilder.create().texOffs(86, 0)
                        .addBox(3.4F, -7.6F, -3.2F, 2.6F, 5.0F, 2.8F, crest),
                PartPose.rotation(0.0F, 0.0F, -0.46F));
        head.addOrReplaceChild("echo_right_horn",
                CubeListBuilder.create().texOffs(86, 0).mirror()
                        .addBox(-6.0F, -7.6F, -3.2F, 2.6F, 5.0F, 2.8F, crest),
                PartPose.rotation(0.0F, 0.0F, 0.46F));
    }

    private static void addChest(PartDefinition root, CubeDeformation deformation, boolean inner) {
        PartDefinition body = root.getChild("body");
        CubeDeformation plate = deformation.extend(inner ? 0.08F : 0.18F);
        body.addOrReplaceChild("echo_chest_core",
                CubeListBuilder.create().texOffs(64, 14)
                        .addBox(-4.8F, 1.0F, -2.85F, 9.6F, 8.8F, 1.2F, plate),
                PartPose.ZERO);
        body.addOrReplaceChild("echo_back_plate",
                CubeListBuilder.create().texOffs(88, 14)
                        .addBox(-4.7F, 1.5F, 1.75F, 9.4F, 8.2F, 1.1F, plate),
                PartPose.ZERO);
        body.addOrReplaceChild("echo_waist_guard",
                CubeListBuilder.create().texOffs(64, 28)
                        .addBox(-5.0F, 9.2F, -2.7F, 10.0F, 3.1F, 5.4F, deformation.extend(0.10F)),
                PartPose.ZERO);
    }

    private static void addArms(PartDefinition root, CubeDeformation deformation, boolean inner) {
        if (inner) {
            return;
        }
        CubeDeformation shoulder = deformation.extend(0.26F);
        root.getChild("right_arm").addOrReplaceChild("echo_right_pauldron",
                CubeListBuilder.create().texOffs(96, 0)
                        .addBox(-4.0F, -3.0F, -2.9F, 5.2F, 4.0F, 5.8F, shoulder),
                PartPose.rotation(0.0F, 0.0F, 0.18F));
        root.getChild("left_arm").addOrReplaceChild("echo_left_pauldron",
                CubeListBuilder.create().texOffs(96, 0).mirror()
                        .addBox(-1.2F, -3.0F, -2.9F, 5.2F, 4.0F, 5.8F, shoulder),
                PartPose.rotation(0.0F, 0.0F, -0.18F));
        root.getChild("right_arm").addOrReplaceChild("echo_right_bracer",
                CubeListBuilder.create().texOffs(96, 12)
                        .addBox(-3.5F, 5.2F, -2.6F, 4.6F, 4.2F, 5.2F, deformation.extend(0.12F)),
                PartPose.ZERO);
        root.getChild("left_arm").addOrReplaceChild("echo_left_bracer",
                CubeListBuilder.create().texOffs(96, 12).mirror()
                        .addBox(-1.1F, 5.2F, -2.6F, 4.6F, 4.2F, 5.2F, deformation.extend(0.12F)),
                PartPose.ZERO);
    }

    private static void addLegs(PartDefinition root, CubeDeformation deformation, boolean inner) {
        CubeDeformation legPlate = deformation.extend(inner ? 0.05F : 0.14F);
        root.getChild("right_leg").addOrReplaceChild("echo_right_thigh",
                CubeListBuilder.create().texOffs(64, 38)
                        .addBox(-2.45F, 0.5F, -2.45F, 4.9F, 5.8F, 4.9F, legPlate),
                PartPose.ZERO);
        root.getChild("left_leg").addOrReplaceChild("echo_left_thigh",
                CubeListBuilder.create().texOffs(64, 38).mirror()
                        .addBox(-2.45F, 0.5F, -2.45F, 4.9F, 5.8F, 4.9F, legPlate),
                PartPose.ZERO);
        root.getChild("right_leg").addOrReplaceChild("echo_right_boot",
                CubeListBuilder.create().texOffs(84, 38)
                        .addBox(-2.55F, 7.1F, -2.55F, 5.1F, 4.9F, 5.1F, deformation.extend(0.16F)),
                PartPose.ZERO);
        root.getChild("left_leg").addOrReplaceChild("echo_left_boot",
                CubeListBuilder.create().texOffs(84, 38).mirror()
                        .addBox(-2.55F, 7.1F, -2.55F, 5.1F, 4.9F, 5.1F, deformation.extend(0.16F)),
                PartPose.ZERO);
    }
}
