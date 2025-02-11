package net.mokai.quicksandrehydrated.block.quicksands;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;
import net.mokai.quicksandrehydrated.block.quicksands.core.QuicksandBase;
import net.mokai.quicksandrehydrated.block.quicksands.core.QuicksandBehavior;
import net.mokai.quicksandrehydrated.entity.EntityBubble;
import net.mokai.quicksandrehydrated.entity.entityQuicksandVar;
import net.mokai.quicksandrehydrated.util.BodyDepthThreshold;
import net.mokai.quicksandrehydrated.util.DepthCurve;
import net.mokai.quicksandrehydrated.util.EasingHandler;
import org.jetbrains.annotations.NotNull;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.LevelAccessor;
import org.joml.Vector3f;

import java.util.Properties;
import java.util.Random;

import static org.joml.Math.*;

public class CheeseBlock extends QuicksandBase {

    
    public String coverageTexture() {
        return "qsrehydrated:textures/entity/coverage/mud_coverage.png";
    }


    public double maxDepth;
    public CheeseBlock(Properties pProperties, QuicksandBehavior QSB, double mudDepth) {
        super(pProperties, QSB);
        this.maxDepth = mudDepth;
    }
    // helps determine what behaviour the mud should have. Is the entity deep enough to get stuck in place?
    /**
     * The Body Depth Threshold where the body is considered as stuck.
     */
    public static final BodyDepthThreshold STUCK_DEPTH = BodyDepthThreshold.UNDER;

    /**
     * Find out, if the depth level is considered as stuck.
     * @param depthRaw The depth level.
     * @return <code>true</code>, if it's considered as stuck.
     */
    public boolean depthIsStuck(double depthRaw) {
        return depthRaw >= STUCK_DEPTH.depth;
    }

    public boolean canStepOut(double depth) {
        return depth < 0.25d;
    }

    private double interpolateAfterKnee(double depth, double a, double b) {
        double normalDepth = (depth - STUCK_DEPTH.depth) / (2- STUCK_DEPTH.depth); // start the array at knee depth
        return EasingHandler.doubleListInterpolate(normalDepth, new double[]{a, b}); // player not allowed to move

    }

    private static final VoxelShape NORMAL_SHAPE = Block.box(0, 0, 0, 16, 15, 16);

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return NORMAL_SHAPE;
    }
    
    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return NORMAL_SHAPE;
    }
    
    @Override
    public VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        return NORMAL_SHAPE;
    }
    
    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return NORMAL_SHAPE;
    }

    @Override
    public void applyQuicksandEffects(@NotNull BlockState pState, @NotNull Level pLevel, @NotNull BlockPos pPos,
                                      @NotNull Entity pEntity) {
    super.applyQuicksandEffects(pState, pLevel, pPos, pEntity);

        if (pEntity instanceof EntityBubble) {
            return;
        }

        double depth = getDepth(pLevel, pPos, pEntity);
        if (depth > 0) {

            double walk = getWalkSpeed(depth);
            entityQuicksandVar eqs = (entityQuicksandVar) pEntity;

            Random rng = new Random();

            // get entity momentum, to determine if moving.
            Vec3 momentumVec3 = pEntity.getDeltaMovement();
            momentumVec3 = new Vec3(momentumVec3.x, 0, momentumVec3.z);
            double momentum = momentumVec3.length();

            double momentumValue = momentumVec3.length()*100;

            // if they're moving, turn some of their horizontal momentum into sinking
            if (momentumValue > 0.0001) {

                Vec3 sinkVec = pEntity.getDeltaMovement();
                sinkVec = new Vec3(sinkVec.x, 0.0, sinkVec.z);

                double sinkAmount = sinkVec.length() * 0.1;
                eqs.addQuicksandAdditive(new Vec3(0.0, -sinkAmount, 0.0));

            }

            // random slurping

            double rngVal = rng.nextDouble();
            boolean canSink = depth < maxDepth;

            if (canSink) {

                // in the end, an RNG value has to be BELOW slurpChance to succeed.
                // so a large value = not likely at all
                // small value = very likely

                double slurpLerp = depth/maxDepth; // as you approach max depth, this approaches 1.0 ...
//                    slurpLerp = Math.pow(slurpLerp, 2); // curved down.
                // slurpLerp starts small, but gets large very quickly near 1.0.

                double movementChance = 0.0;
                if (walk != 0.0) {
                    movementChance = (momentum / walk) * 0.05; // doesn't have much effect
                }
                // attempts to correct for the thickness, but isn't quite right ... gets comparatively larger as walk increases.

                double slurpChance = 1.0;
                slurpChance -= movementChance;

                if (momentumValue*100 > 0.01) {
                    slurpChance -= 0.02;
                }

                slurpChance = lerp(slurpChance, 1.0, slurpLerp);

                if (rngVal > slurpChance) {
                    slurpEntity(pState, pEntity);
                }

            }

        }

    }



    public void slurpEntity(@NotNull BlockState pState, @NotNull Entity pEntity) {

        Random rng = new Random();

        double rngVal = rng.nextDouble();

        float gain = (float) ((rngVal * 0.75F) - 0.3F);
        gain = Math.max(gain, 0.0F);

        double pulldown = rngVal * -0.05;
        pulldown -= 0.01;

        pEntity.addDeltaMovement(new Vec3(0.0, pulldown, 0.0));
        pEntity.level().playSound(pEntity, pEntity.blockPosition(), SoundEvents.HONEY_BLOCK_SLIDE, SoundSource.BLOCKS, gain, (pEntity.level().getRandom().nextFloat() * 0.1F) + 0.45F);
        pEntity.level().playSound(pEntity, pEntity.blockPosition(), SoundEvents.MAGMA_CUBE_JUMP, SoundSource.BLOCKS, gain, (pEntity.level().getRandom().nextFloat() * 0.1F) + 0.45F);


        for (int i = 0; i < 5; i++) {
            spawnParticles(pEntity.level(), pEntity.blockPosition());
        }

    }

    @Override
    public void struggleAttempt(@NotNull BlockState pState, @NotNull Entity pEntity, double struggleAmount) {

        double min = 0.0;
        struggleAmount *= 0.2;
        double struggleForce = min+struggleAmount;

        Random rng = new Random();
        double rngVal = rng.nextDouble(0.7, 1.2);
        struggleForce *= rngVal;

        pEntity.addDeltaMovement(new Vec3(0.0, struggleForce, 0.0));
        pEntity.level().playSound(pEntity, pEntity.blockPosition(), SoundEvents.HONEY_BLOCK_PLACE, SoundSource.BLOCKS, 0.25F, (pEntity.level().getRandom().nextFloat() * 0.1F) + 0.45F);

        for (int i = 0; i < 5; i++) {
            spawnParticles(pEntity.level(), pEntity.blockPosition());
        }

    }



    public static final Vector3f MUD_COLOR = Vec3.fromRGB24(16763768).toVector3f();

    // normal block things v

    @Override
    public void firstTouch(BlockPos pPos, Entity pEntity, Level pLevel) {

        super.firstTouch(pPos, pEntity, pLevel);

        entityQuicksandVar es = (entityQuicksandVar) pEntity;

        if (pEntity.getDeltaMovement().y <= -0.5) {
            pEntity.level().playSound(pEntity, pEntity.blockPosition(), SoundEvents.HONEY_BLOCK_FALL, SoundSource.BLOCKS, 0.4F, (pEntity.level().getRandom().nextFloat() * 0.1F) + 0.45F);
        }

    }



    private static void spawnParticles(Level pLevel, BlockPos pPos) {

        RandomSource randomsource = pLevel.random;

        Direction direction = Direction.UP;

        // taken from redstone ore block code
        BlockPos blockpos = pPos.relative(direction);

        Direction.Axis direction$axis = direction.getAxis();
        double d1 = direction$axis == Direction.Axis.X ? 0.5D + 0.5625D * (double) direction.getStepX() : (double) randomsource.nextFloat();
        double d2 = direction$axis == Direction.Axis.Y ? 0.5D + 0.5625D * (double) direction.getStepY() : (double) randomsource.nextFloat();
        double d3 = direction$axis == Direction.Axis.Z ? 0.5D + 0.5625D * (double) direction.getStepZ() : (double) randomsource.nextFloat();
//        pLevel.addParticle(???, d1, d2, d3, 0.0D, 0.0, 0.0D);

        Vector3f MUD_COLOOR = Vec3.fromRGB24(16763768).toVector3f();
        pLevel.addParticle(new DustParticleOptions(MUD_COLOOR, 1.0F), (double) pPos.getX() + d1, (double) pPos.getY() + d2, (double) pPos.getZ() + d3, 0.0D, 0.0D, 0.0D);


    }

    public void animateTick(BlockState pState, Level pLevel, BlockPos pPos, RandomSource pRandom) {
        //spawnParticles(pLevel, pPos);
    }


}