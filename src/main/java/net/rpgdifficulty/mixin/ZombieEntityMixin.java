package net.rpgdifficulty.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.World;
import net.rpgdifficulty.access.EntityAccess;

@Mixin(ZombieEntity.class)
public abstract class ZombieEntityMixin extends HostileEntity implements EntityAccess {
    private static final TrackedData<Boolean> BIG_ZOMBIE = DataTracker.registerData(ZombieEntity.class,
            TrackedDataHandlerRegistry.BOOLEAN);

    public ZombieEntityMixin(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "initDataTracker", at = @At("TAIL"))
    protected void initDataTrackerMixin(CallbackInfo info) {
        this.dataTracker.startTracking(BIG_ZOMBIE, false);
    }

    @Inject(method = "readCustomDataFromTag", at = @At("TAIL"))
    private void readCustomDataFromTagMixin(CompoundTag tag, CallbackInfo info) {
        this.dataTracker.set(BIG_ZOMBIE, tag.getBoolean("Big"));
    }

    @Inject(method = "writeCustomDataToTag", at = @At("TAIL"))
    private void writeCustomDataToTagMixin(CompoundTag tag, CallbackInfo info) {
        tag.putBoolean("Big", this.dataTracker.get(BIG_ZOMBIE));
    }

    @Override
    public void setBig() {
        this.dataTracker.set(BIG_ZOMBIE, true);
        this.refreshPosition();
        this.calculateDimensions();
    }

    @Inject(method = "onTrackedDataSet", at = @At("HEAD"))
    public void onTrackedDataSetMixin(TrackedData<?> data, CallbackInfo info) {
        if (BIG_ZOMBIE.equals(data)) {
            this.calculateDimensions();
        }
    }

    @Override
    public EntityDimensions getDimensions(EntityPose pose) {
        return this.dataTracker.get(BIG_ZOMBIE) ? super.getDimensions(pose).scaled(1.3F) : super.getDimensions(pose);
    }
}
