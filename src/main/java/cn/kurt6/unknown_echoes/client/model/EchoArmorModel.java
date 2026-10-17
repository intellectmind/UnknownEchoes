package cn.kurt6.unknown_echoes.client.model;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

public final class EchoArmorModel<T extends LivingEntity> extends HumanoidModel<T> {
    private EchoArmorModel(ModelPart root) {
        super(root);
    }

    public static EchoArmorModel<LivingEntity> create(EquipmentSlot slot) {
        MeshDefinition mesh = EchoArmorGeometry.anchors();
        PartDefinition root = mesh.getRoot();
        EchoArmorGeometry.Atlas atlas = new EchoArmorGeometry.Atlas(slot);
        switch (slot) {
            case HEAD -> {
                EchoArmorGeometry.helmet(root, atlas);
                PartDefinition head = root.getChild("head");
                EchoArmorGeometry.Plate horn = atlas.box(1.8F, 4.8F, 2.0F);
                horn.add(head, "echo_right_horn", -0.9F, -4.8F, -1.0F, false,
                        PartPose.offsetAndRotation(4.5F, -6.2F, -0.6F, 0, 0.12F, 0.46F));
                horn.add(head, "echo_left_horn", -0.9F, -4.8F, -1.0F, true,
                        PartPose.offsetAndRotation(-4.5F, -6.2F, -0.6F, 0, -0.12F, -0.46F));
                atlas.box(1.4F, 0.8F, 0.5F).add(head, "brow_shard", -0.7F, -7.7F, -5.45F);
            }
            case CHEST -> EchoArmorGeometry.chest(root, atlas, -0.4F, 0.22F, 0.65F, -0.06F);
            case LEGS -> EchoArmorGeometry.leggings(root, atlas, 0.1F);
            case FEET -> EchoArmorGeometry.boots(root, atlas, true);
            default -> {
            }
        }
        return new EchoArmorModel<>(LayerDefinition.create(mesh, 256, 128).bakeRoot());
    }
}
