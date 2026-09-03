from pathlib import Path

ROOT = Path("src/main/java/slimeknights/tconstruct")


def read(path: str) -> tuple[Path, str]:
    target = ROOT / path
    return target, target.read_text()


def replace_once(path: str, old: str, new: str, label: str) -> None:
    target, source = read(path)
    count = source.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected one match, got {count}")
    target.write_text(source.replace(old, new))


# Channel interaction split. Held items and empty hands are separate hooks in 1.21.
target, source = read("smeltery/block/ChannelBlock.java")
anchor = "import net.minecraft.world.InteractionResult;\n"
if source.count(anchor) != 1 or "import net.minecraft.world.ItemInteractionResult;\n" in source:
    raise SystemExit("ChannelBlock: unexpected interaction imports")
source = source.replace(anchor, anchor + "import net.minecraft.world.ItemInteractionResult;\n")
start_marker = "  @SuppressWarnings(\"deprecation\")\n  @Override\n  public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {"
end_marker = "\n  @SuppressWarnings(\"deprecation\")\n  @Override\n  @Deprecated\n  public void neighborChanged"
start = source.find(start_marker)
end = source.find(end_marker, start)
if start < 0 or end < 0:
    raise SystemExit("ChannelBlock: legacy use method boundaries not found")
new_method = '''  @SuppressWarnings("deprecation")
  @Override
  protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
    Direction hitFace = hit.getDirection();
    if (world.getBlockState(pos.relative(hitFace)).canBeReplaced()) {
      if (stack.getItem() == this.asItem()) {
        return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
      }
      if (hitFace != Direction.DOWN && stack.getItem() instanceof BlockItem blockItem && RegistryHelper.contains(MantleTags.Blocks.ATTACHED_GAUGES, blockItem.getBlock())) {
        if (hitFace != Direction.UP) {
          EnumProperty<ChannelConnection> prop = DIRECTION_MAP.get(hitFace);
          ChannelConnection connection = state.getValue(prop);
          if (connection == ChannelConnection.NONE) {
            BlockState newState = state.setValue(prop, ChannelConnection.IN);
            world.setBlockAndUpdate(pos, newState);
            world.invalidateCapabilities(pos);
          }
        }
        return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
      }
    }

    return interactWithChannel(state, world, pos, player, hit) == InteractionResult.SUCCESS
      ? ItemInteractionResult.SUCCESS
      : ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
  }

  @SuppressWarnings("deprecation")
  @Override
  protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
    return interactWithChannel(state, world, pos, player, hit);
  }

  private InteractionResult interactWithChannel(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
    Direction hitFace = hit.getDirection();
    Direction side = hitFace == Direction.UP ? Direction.DOWN : hitFace;
    if (player.isShiftKeyDown() && side != Direction.DOWN) {
      side = side.getOpposite();
    }

    Vec3 hitVec = hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
    if (hitVec.z() < 0.25f) {
      side = Direction.NORTH;
    } else if (hitVec.z() > 0.75f) {
      side = Direction.SOUTH;
    } else if (hitVec.x() < 0.25f) {
      side = Direction.WEST;
    } else if (hitVec.x() > 0.75f) {
      side = Direction.EAST;
    }

    BlockState newState = interactWithSide(state, world, pos, player, side);
    if (newState != null) {
      world.setBlockAndUpdate(pos, newState);
      if (!world.isClientSide && world.getBlockEntity(pos) instanceof ChannelBlockEntity te) {
        te.refreshNeighbor(newState, side);
      }
      return InteractionResult.SUCCESS;
    }
    return InteractionResult.PASS;
  }
'''
target.write_text(source[:start] + new_method + source[end:])


# Proxy tank: preserve clicked-region routing for both held-item and empty-hand interactions.
target, source = read("smeltery/block/ProxyTankBlock.java")
if source.count("import net.minecraft.world.InteractionResult;\n") != 1 or "ItemInteractionResult" in source:
    raise SystemExit("ProxyTankBlock: unexpected interaction imports")
source = source.replace(
    "import net.minecraft.world.InteractionResult;\n",
    "import net.minecraft.world.InteractionResult;\nimport net.minecraft.world.ItemInteractionResult;\n",
)
source = source.replace(
    "import net.minecraft.world.entity.player.Player;\n",
    "import net.minecraft.world.entity.player.Player;\nimport net.minecraft.world.item.ItemStack;\n",
    1,
)
old = '''  @Deprecated
  @Override
  public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
    if (world.getBlockEntity(pos) instanceof ProxyTankBlockEntity tank) {
      boolean clickedTank;
      Direction direction = hit.getDirection();
      if (direction == Direction.DOWN) {
        // down is a solid flat spot, treat it all as items
        clickedTank = false;
      } else {
        Vec3 location = hit.getLocation();
        double x = location.x - pos.getX();
        double z = location.z - pos.getZ();
        // up is a window, corners are tanks and center item
        clickedTank = (x < LOWER || x > UPPER) && (z < LOWER || z > UPPER);
        // if you clicked a side tank, cancel that if we clicked too low
        if (clickedTank && direction != Direction.UP) {
          double y = location.y - pos.getY();
          clickedTank = y > 0.25f;
        }
      }
      tank.interact(player, hand, clickedTank);
    }
    return InteractionResult.SUCCESS;
  }
'''
new = '''  @Deprecated
  @Override
  protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
    interact(world, pos, player, hand, hit);
    return ItemInteractionResult.SUCCESS;
  }

  @Deprecated
  @Override
  protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
    interact(world, pos, player, InteractionHand.MAIN_HAND, hit);
    return InteractionResult.SUCCESS;
  }

  private void interact(Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
    if (world.getBlockEntity(pos) instanceof ProxyTankBlockEntity tank) {
      boolean clickedTank;
      Direction direction = hit.getDirection();
      if (direction == Direction.DOWN) {
        clickedTank = false;
      } else {
        Vec3 location = hit.getLocation();
        double x = location.x - pos.getX();
        double z = location.z - pos.getZ();
        clickedTank = (x < LOWER || x > UPPER) && (z < LOWER || z > UPPER);
        if (clickedTank && direction != Direction.UP) {
          double y = location.y - pos.getY();
          clickedTank = y > 0.25f;
        }
      }
      tank.interact(player, hand, clickedTank);
    }
  }
'''
if source.count(old) != 1:
    raise SystemExit("ProxyTankBlock: legacy use block mismatch")
target.write_text(source.replace(old, new))


# Casting tank: preserve lower-tank/item-region interaction for held and empty hands.
target, source = read("smeltery/block/CastingTankBlock.java")
anchor = "import net.minecraft.world.InteractionResult;\n"
if source.count(anchor) != 1 or "ItemInteractionResult" in source:
    raise SystemExit("CastingTankBlock: unexpected interaction imports")
source = source.replace(anchor, anchor + "import net.minecraft.world.ItemInteractionResult;\n")
old = '''  @Deprecated
  @Override
  public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
    if (world.getBlockEntity(pos) instanceof CastingTankBlockEntity tank) {
      tank.interact(player, hand, hit.getLocation().y - pos.getY() < 0.6875);
      return InteractionResult.SUCCESS;
    }
    return InteractionResult.FAIL;
  }
'''
new = '''  @Deprecated
  @Override
  protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
    if (world.getBlockEntity(pos) instanceof CastingTankBlockEntity tank) {
      tank.interact(player, hand, hit.getLocation().y - pos.getY() < 0.6875);
      return ItemInteractionResult.SUCCESS;
    }
    return ItemInteractionResult.FAIL;
  }

  @Deprecated
  @Override
  protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
    if (world.getBlockEntity(pos) instanceof CastingTankBlockEntity tank) {
      tank.interact(player, InteractionHand.MAIN_HAND, hit.getLocation().y - pos.getY() < 0.6875);
      return InteractionResult.SUCCESS;
    }
    return InteractionResult.FAIL;
  }
'''
if source.count(old) != 1:
    raise SystemExit("CastingTankBlock: legacy use block mismatch")
target.write_text(source.replace(old, new))


# Drain fluid-container interaction.
target, source = read("smeltery/block/component/SearedDrainBlock.java")
if source.count("import net.minecraft.world.InteractionResult;\n") != 1:
    raise SystemExit("SearedDrainBlock: interaction import mismatch")
source = source.replace("import net.minecraft.world.InteractionResult;\n", "import net.minecraft.world.ItemInteractionResult;\n")
source = source.replace(
    "import net.minecraft.world.entity.player.Player;\n",
    "import net.minecraft.world.entity.player.Player;\nimport net.minecraft.world.item.ItemStack;\n",
    1,
)
old = '''  public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
    if (FluidTransferHelper.interactWithTank(world, pos, player, hand, hit.getDirection(), state.getValue(FACING).getOpposite())) {
      return InteractionResult.SUCCESS;
    }
    return InteractionResult.PASS;
  }
'''
new = '''  protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
    if (FluidTransferHelper.interactWithTank(world, pos, player, hand, hit.getDirection(), state.getValue(FACING).getOpposite())) {
      return ItemInteractionResult.SUCCESS;
    }
    return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
  }
'''
if source.count(old) != 1:
    raise SystemExit("SearedDrainBlock: legacy use method mismatch")
target.write_text(source.replace(old, new))


# 1.21 moved custom render bounds from BlockEntity to the renderer extension.
target, source = read("smeltery/block/entity/ChannelBlockEntity.java")
if source.count("import net.minecraft.world.phys.AABB;\n") != 1:
    raise SystemExit("ChannelBlockEntity: AABB import mismatch")
source = source.replace("import net.minecraft.world.phys.AABB;\n", "")
old = '''  @Override
  public AABB getRenderBoundingBox() {
    return new AABB(worldPosition.getX(), worldPosition.getY() - 1, worldPosition.getZ(), worldPosition.getX() + 1, worldPosition.getY() + 1, worldPosition.getZ() + 1);
  }

'''
if source.count(old) != 1:
    raise SystemExit("ChannelBlockEntity: legacy render bounds mismatch")
target.write_text(source.replace(old, ""))

target, source = read("smeltery/client/render/ChannelBlockEntityRenderer.java")
anchor = "import net.minecraft.world.level.block.state.BlockState;\n"
if source.count(anchor) != 1 or "import net.minecraft.world.phys.AABB;\n" in source:
    raise SystemExit("Channel renderer: import state mismatch")
source = source.replace(anchor, anchor + "import net.minecraft.world.phys.AABB;\n")
constructor = "  public ChannelBlockEntityRenderer(Context context) {}\n\n"
method = '''  @Override
  public AABB getRenderBoundingBox(ChannelBlockEntity te) {
    BlockPos pos = te.getBlockPos();
    return new AABB(pos.getX(), pos.getY() - 1, pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
  }

'''
if source.count(constructor) != 1:
    raise SystemExit("Channel renderer: constructor anchor mismatch")
target.write_text(source.replace(constructor, constructor + method))


# Geometry loader registration now takes namespaced resource IDs.
target, source = read("smeltery/SmelteryClientEvents.java")
if source.count('event.register("tank", TankModel.LOADER);') != 1 or source.count('event.register("fluid_texture", FluidTextureModel.LOADER);') != 1:
    raise SystemExit("SmelteryClientEvents: geometry loader registrations mismatch")
source = source.replace('event.register("tank", TankModel.LOADER);', 'event.register(TConstruct.getResource("tank"), TankModel.LOADER);')
source = source.replace('event.register("fluid_texture", FluidTextureModel.LOADER);', 'event.register(TConstruct.getResource("fluid_texture"), FluidTextureModel.LOADER);')
target.write_text(source)


# StairBlock constructor now receives a concrete base state.
replace_once(
    "smeltery/block/component/SearedStairsBlock.java",
    "    super(state, properties);",
    "    super(state.get(), properties);",
    "SearedStairsBlock",
)


# Item tooltip API uses Item.TooltipContext in 1.21.
target, source = read("smeltery/item/DummyMaterialItem.java")
if source.count("import net.minecraft.world.level.Level;\n") != 1 or source.count("import javax.annotation.Nullable;\n") != 1:
    raise SystemExit("DummyMaterialItem: legacy imports mismatch")
source = source.replace("import net.minecraft.world.level.Level;\n", "").replace("import javax.annotation.Nullable;\n", "")
old = "  public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> tooltip, TooltipFlag pIsAdvanced) {"
new = "  public void appendHoverText(ItemStack pStack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag pIsAdvanced) {"
if source.count(old) != 1:
    raise SystemExit("DummyMaterialItem: legacy tooltip signature mismatch")
target.write_text(source.replace(old, new))


# FluidStack serialization moved to the registry-aware stream codec.
target, source = read("smeltery/network/FaucetActivationPacket.java")
anchor = "import net.minecraft.network.FriendlyByteBuf;\n"
if source.count(anchor) != 1 or "RegistryFriendlyByteBuf" in source:
    raise SystemExit("FaucetActivationPacket: buffer import mismatch")
source = source.replace(anchor, anchor + "import net.minecraft.network.RegistryFriendlyByteBuf;\n")
if source.count("this.fluid = buffer.readFluidStack();") != 1 or source.count("buffer.writeFluidStack(fluid);") != 1:
    raise SystemExit("FaucetActivationPacket: legacy fluid codec calls mismatch")
source = source.replace(
    "this.fluid = buffer.readFluidStack();",
    "this.fluid = FluidStack.OPTIONAL_STREAM_CODEC.decode((RegistryFriendlyByteBuf)buffer);",
)
source = source.replace(
    "buffer.writeFluidStack(fluid);",
    "FluidStack.OPTIONAL_STREAM_CODEC.encode((RegistryFriendlyByteBuf)buffer, fluid);",
)
target.write_text(source)


# Vanilla entity textures used the old one-argument ResourceLocation constructor.
target, source = read("world/WorldClientEvents.java")
old = 'new ResourceLocation("textures/'
count = source.count(old)
if count != 12:
    raise SystemExit(f"WorldClientEvents: expected 12 vanilla texture IDs, got {count}")
target.write_text(source.replace(old, 'ResourceLocation.withDefaultNamespace("textures/'))
