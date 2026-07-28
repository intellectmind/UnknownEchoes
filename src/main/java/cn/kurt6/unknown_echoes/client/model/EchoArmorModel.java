package cn.kurt6.unknown_echoes.client.model;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

/**
 * 回响套装的客户端厚甲模型。
 * 仍继承 HumanoidModel 以保留原版动作,但使用额外几何做外扩头盔、肩甲和分段腿甲。
 */
public class EchoArmorModel<T extends LivingEntity> extends HumanoidModel<T> {
    private static final float TEXTURE_SCALE = 0.5F;

    private EchoArmorModel(ModelPart root) {
        super(root);
    }

    public static EchoArmorModel<LivingEntity> create(EquipmentSlot slot) {
        CubeDeformation deformation = slot == EquipmentSlot.LEGS
                ? new CubeDeformation(0.34F)
                : new CubeDeformation(0.68F);
        return new EchoArmorModel<>(createLayer(slot, deformation).bakeRoot());
    }

    private static LayerDefinition createLayer(EquipmentSlot slot, CubeDeformation deformation) {
        MeshDefinition mesh = createHighResolutionMesh(deformation);
        PartDefinition root = mesh.getRoot();
        switch (slot) {
            case HEAD -> addHelmet(root, deformation);
            case CHEST -> {
                addChest(root, deformation);
                addArms(root, deformation);
            }
            case LEGS -> addLeggings(root, deformation);
            case FEET -> addBoots(root, deformation);
            default -> {
            }
        }
        return LayerDefinition.create(mesh, 256, 128);
    }

    private static MeshDefinition createHighResolutionMesh(CubeDeformation deformation) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F,
                                deformation, TEXTURE_SCALE, TEXTURE_SCALE),
                PartPose.ZERO);
        root.addOrReplaceChild("hat",
                CubeListBuilder.create().texOffs(32, 0)
                        .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F,
                                deformation.extend(0.5F), TEXTURE_SCALE, TEXTURE_SCALE),
                PartPose.ZERO);
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(16, 16)
                        .addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F,
                                deformation, TEXTURE_SCALE, TEXTURE_SCALE),
                PartPose.ZERO);
        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(40, 16)
                        .addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F,
                                deformation, TEXTURE_SCALE, TEXTURE_SCALE),
                PartPose.offset(-5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(40, 16).mirror()
                        .addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F,
                                deformation, TEXTURE_SCALE, TEXTURE_SCALE),
                PartPose.offset(5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(0, 16)
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F,
                                deformation, TEXTURE_SCALE, TEXTURE_SCALE),
                PartPose.offset(-1.9F, 12.0F, 0.0F));
        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(0, 16).mirror()
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F,
                                deformation, TEXTURE_SCALE, TEXTURE_SCALE),
                PartPose.offset(1.9F, 12.0F, 0.0F));
        return mesh;
    }

    private static void addHelmet(PartDefinition root, CubeDeformation deformation) {
        PartDefinition head = root.getChild("head");
        CubeDeformation crest = deformation.extend(0.22F);
        head.addOrReplaceChild("echo_face_guard",
                CubeListBuilder.create().texOffs(64, 0)
                        .addBox(-4.6F, -5.2F, -4.9F, 9.2F, 4.4F, 1.0F,
                                crest, TEXTURE_SCALE, TEXTURE_SCALE),
                PartPose.ZERO);
        head.addOrReplaceChild("echo_left_horn",
                CubeListBuilder.create().texOffs(86, 0)
                        .addBox(3.4F, -7.6F, -3.2F, 2.6F, 5.0F, 2.8F,
                                crest, TEXTURE_SCALE, TEXTURE_SCALE),
                PartPose.rotation(0.0F, 0.0F, -0.46F));
        head.addOrReplaceChild("echo_right_horn",
                CubeListBuilder.create().texOffs(86, 0).mirror()
                        .addBox(-6.0F, -7.6F, -3.2F, 2.6F, 5.0F, 2.8F,
                                crest, TEXTURE_SCALE, TEXTURE_SCALE),
                PartPose.rotation(0.0F, 0.0F, 0.46F));
    }

    private static void addChest(PartDefinition root, CubeDeformation deformation) {
        PartDefinition body = root.getChild("body");
        CubeDeformation plate = deformation.extend(0.18F);
        body.addOrReplaceChild("echo_chest_core",
                CubeListBuilder.create().texOffs(64, 14)
                        .addBox(-4.8F, 1.0F, -2.85F, 9.6F, 8.8F, 1.2F,
                                plate, TEXTURE_SCALE, TEXTURE_SCALE),
                PartPose.ZERO);
        body.addOrReplaceChild("echo_back_plate",
                CubeListBuilder.create().texOffs(88, 14)
                        .addBox(-4.7F, 1.5F, 1.75F, 9.4F, 8.2F, 1.1F,
                                plate, TEXTURE_SCALE, TEXTURE_SCALE),
                PartPose.ZERO);
    }

    private static void addArms(PartDefinition root, CubeDeformation deformation) {
        CubeDeformation shoulder = deformation.extend(0.26F);
        root.getChild("right_arm").addOrReplaceChild("echo_right_pauldron",
                CubeListBuilder.create().texOffs(96, 0)
                        .addBox(-4.0F, -3.0F, -2.9F, 5.2F, 4.0F, 5.8F,
                                shoulder, TEXTURE_SCALE, TEXTURE_SCALE),
                PartPose.rotation(0.0F, 0.0F, 0.18F));
        root.getChild("left_arm").addOrReplaceChild("echo_left_pauldron",
                CubeListBuilder.create().texOffs(96, 0).mirror()
                        .addBox(-1.2F, -3.0F, -2.9F, 5.2F, 4.0F, 5.8F,
                                shoulder, TEXTURE_SCALE, TEXTURE_SCALE),
                PartPose.rotation(0.0F, 0.0F, -0.18F));
        root.getChild("right_arm").addOrReplaceChild("echo_right_bracer",
                CubeListBuilder.create().texOffs(96, 12)
                        .addBox(-3.5F, 5.2F, -2.6F, 4.6F, 4.2F, 5.2F,
                                deformation.extend(0.12F), TEXTURE_SCALE, TEXTURE_SCALE),
                PartPose.ZERO);
        root.getChild("left_arm").addOrReplaceChild("echo_left_bracer",
                CubeListBuilder.create().texOffs(96, 12).mirror()
                        .addBox(-1.1F, 5.2F, -2.6F, 4.6F, 4.2F, 5.2F,
                                deformation.extend(0.12F), TEXTURE_SCALE, TEXTURE_SCALE),
                PartPose.ZERO);
    }

    private static void addLeggings(PartDefinition root, CubeDeformation deformation) {
        root.getChild("body").addOrReplaceChild("echo_waist_guard",
                CubeListBuilder.create().texOffs(64, 28)
                        .addBox(-5.0F, 9.2F, -2.7F, 10.0F, 3.1F, 5.4F,
                                deformation.extend(0.10F), TEXTURE_SCALE, TEXTURE_SCALE),
                PartPose.ZERO);
        CubeDeformation legPlate = deformation.extend(0.05F);
        root.getChild("right_leg").addOrReplaceChild("echo_right_thigh",
                CubeListBuilder.create().texOffs(64, 38)
                        .addBox(-2.45F, 0.5F, -2.45F, 4.9F, 5.8F, 4.9F,
                                legPlate, TEXTURE_SCALE, TEXTURE_SCALE),
                PartPose.ZERO);
        root.getChild("left_leg").addOrReplaceChild("echo_left_thigh",
                CubeListBuilder.create().texOffs(64, 38).mirror()
                        .addBox(-2.45F, 0.5F, -2.45F, 4.9F, 5.8F, 4.9F,
                                legPlate, TEXTURE_SCALE, TEXTURE_SCALE),
                PartPose.ZERO);
    }

    private static void addBoots(PartDefinition root, CubeDeformation deformation) {
        root.getChild("right_leg").addOrReplaceChild("echo_right_boot",
                CubeListBuilder.create().texOffs(84, 38)
                        .addBox(-2.55F, 7.1F, -2.55F, 5.1F, 4.9F, 5.1F,
                                deformation.extend(0.16F), TEXTURE_SCALE, TEXTURE_SCALE),
                PartPose.ZERO);
        root.getChild("left_leg").addOrReplaceChild("echo_left_boot",
                CubeListBuilder.create().texOffs(84, 38).mirror()
                        .addBox(-2.55F, 7.1F, -2.55F, 5.1F, 4.9F, 5.1F,
                                deformation.extend(0.16F), TEXTURE_SCALE, TEXTURE_SCALE),
                PartPose.ZERO);
    }
}
