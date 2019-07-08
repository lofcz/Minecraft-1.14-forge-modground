package com.example.examplemod;

import com.example.examplemod.blocks.FirstBlock;
import com.example.examplemod.blocks.FirstBlockContainer;
import com.example.examplemod.blocks.FirstBlockTile;
import com.example.examplemod.blocks.ModBlocks;
import com.example.examplemod.event.*;
import com.example.examplemod.items.FirstItem;
import com.example.examplemod.lighting.DefaultLightProvider;
import com.example.examplemod.lighting.ILightProvider;
import com.example.examplemod.lighting.Light;
import com.example.examplemod.lighting.LightManager;
import com.example.examplemod.setup.ClientProxy;
import com.example.examplemod.setup.IProxy;
import com.example.examplemod.setup.ModSetup;
import com.example.examplemod.setup.ServerProxy;
import com.example.examplemod.util.ShaderManager;
import com.example.examplemod.util.ShaderUtil;
import com.example.examplemod.util.TriConsumer;
import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RedstoneTorchBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.entity.effect.LightningBoltEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.nbt.INBT;
import net.minecraft.resources.IReloadableResourceManager;
import net.minecraft.tileentity.EndGatewayTileEntity;
import net.minecraft.tileentity.EndPortalTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DeferredWorkQueue;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.*;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

// The value here should match an entry in the META-INF/mods.toml file
@Mod("examplemod")
public class ExampleMod
{
    // Directly reference a log4j logger.
    private static final Logger LOGGER = LogManager.getLogger();
    public static IProxy proxy = DistExecutor.runForDist(() -> () -> new ClientProxy(), () -> () -> new ServerProxy());
    public static final String MODID = "examplemod";
    public static ModSetup setup = new ModSetup();

    // albedo
    // albedo port starts here
    private static final Map<BlockPos, List<Light>> EXISTING = Collections.synchronizedMap(new HashMap<>());
    public static boolean isGui = false;
    private static int ticks = 0;
    private static boolean postedLights = false;
    private static boolean precedesEntities = true;
    private static String section = "";
    private static Thread thread;
    // --------

    private static final Map<Block, TriConsumer<BlockPos, BlockState, GatherLightsEvent>> MAP = new HashMap<>();
    @CapabilityInject(ILightProvider.class)
    public static Capability<ILightProvider> LIGHT_PROVIDER_CAPABILITY;

    public static void registerBlockHandler(Block block, TriConsumer<BlockPos, BlockState, GatherLightsEvent> consumer) {
        MAP.put(block, consumer);
    }

    public static TriConsumer<BlockPos, BlockState, GatherLightsEvent> getLightHandler(Block block) {
        return MAP.get(block);
    }

    public static ImmutableMap<Block, TriConsumer<BlockPos, BlockState, GatherLightsEvent>> getBlockHandlers() {
        return ImmutableMap.copyOf(MAP);
    }

    public void commonSetup(FMLCommonSetupEvent event) {
        CapabilityManager.INSTANCE.register(ILightProvider.class, new Capability.IStorage<ILightProvider>() {

            @Nullable
            @Override
            public INBT writeNBT(Capability<ILightProvider> capability, ILightProvider instance, Direction side) {
                return null;
            }

            @Override
            public void readNBT(Capability<ILightProvider> capability, ILightProvider instance, Direction side, INBT nbt) {

            }
        }, DefaultLightProvider::new);
    }

    public void loadComplete(FMLLoadCompleteEvent event) {
        DeferredWorkQueue.runLater(() -> ((IReloadableResourceManager) Minecraft.getInstance().getResourceManager()).addReloadListener(new ShaderUtil()));
    }

    public ExampleMod() {
        // Register the setup method for modloading
        //FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        // Register the enqueueIMC method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::enqueueIMC);
        // Register the processIMC method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::processIMC);
        // Register the doClientStuff method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::doClientStuff);

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::loadComplete);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ConfigManager.spec);

    }

    public void clientSetup(FMLClientSetupEvent event) {
        //MinecraftForge.EVENT_BUS.register(new ExampleMod()); // [TODO] resolve
        MinecraftForge.EVENT_BUS.register(new ConfigManager());
        registerBlockHandler(Blocks.REDSTONE_TORCH, (pos, state, evt) -> {
            if (state.get(RedstoneTorchBlock.LIT)) {
                evt.add(Light.builder()
                        .pos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)
                        .color(0.0f, 1.0f, 0, 1.0f)
                        .radius(6)
                        .build());
            }
        });

        proxy.getClientWorld();
        setup.init();
        proxy.init();
    }

    /*
    private void setup(final FMLCommonSetupEvent event)
    {
        // some preinit code
        LOGGER.info("HELLO FROM PREINIT");
        LOGGER.info("DIRT BLOCK >> {}", Blocks.DIRT.getRegistryName());

        proxy.getClientWorld();
        setup.init();
        proxy.init();
    }*/

    private void doClientStuff(final FMLClientSetupEvent event) {
        // do something that can only be done on the client
        LOGGER.info("Got game settings {}", event.getMinecraftSupplier().get().gameSettings);
    }

    private void enqueueIMC(final InterModEnqueueEvent event)
    {
        // some example code to dispatch IMC to another mod
        InterModComms.sendTo("data/examplemod", "helloworld", () -> { LOGGER.info("Hello world from the MDK"); return "Hello world";});
    }

    private void processIMC(final InterModProcessEvent event)
    {
        // some example code to receive and process InterModComms from other mods
        LOGGER.info("Got IMC {}", event.getIMCStream().
                map(m->m.getMessageSupplier().get()).
                collect(Collectors.toList()));
    }
    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(FMLServerStartingEvent event) {
        // do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    // You can use EventBusSubscriber to automatically subscribe events on the contained class (this is subscribing to the MOD
    // Event bus for receiving Registry Events)
    @Mod.EventBusSubscriber(bus=Mod.EventBusSubscriber.Bus.MOD)
    public static class RegistryEvents {
        @SubscribeEvent
        public static void onBlocksRegistry(final RegistryEvent.Register<Block> blockRegistryEvent) {
            LOGGER.info("HELLO from Register Block");

            blockRegistryEvent.getRegistry().register(new FirstBlock());
        }

        @SubscribeEvent
        public static void onItemsRegistry(final RegistryEvent.Register<Item> itemRegistryEvent) {
            LOGGER.info("HELLO from Register Item");

            Item.Properties properties = new Item.Properties()
                    .group(setup.itemGroup);

            itemRegistryEvent.getRegistry().register(new BlockItem(ModBlocks.FIRSTBLOCK, properties).setRegistryName("firstblock"));
            itemRegistryEvent.getRegistry().register(new FirstItem());
        }

        @SubscribeEvent
        public static void onTileEntityRegistry(final RegistryEvent.Register<TileEntityType<?>> tileRegistryEvent) {
            tileRegistryEvent.getRegistry().register(TileEntityType.Builder.create(FirstBlockTile::new, ModBlocks.FIRSTBLOCK).build(null).setRegistryName("firstblock"));
        }

        @SubscribeEvent
        public static void onContainerRegistry(final RegistryEvent.Register<ContainerType<?>> containerRegistryEvent) {
            containerRegistryEvent.getRegistry().register(IForgeContainerType.create(((windowId, inv, data) -> {

                BlockPos pos = data.readBlockPos();
                return new FirstBlockContainer(windowId, ExampleMod.proxy.getClientWorld(), pos, inv, ExampleMod.proxy.getClientPlayer());
            })).setRegistryName("firstblock"));
        }

        // albedo starts here
        @SubscribeEvent
        public static void onProfilerChange(ProfilerStartEvent event) {
            section = event.getSection();

            LOGGER.info("onProfilerChange");

            if (ConfigManager.isLightingEnabled()) {

                LOGGER.info("This is OKOK");

                if (event.getSection().compareTo("terrain") == 0) {
                    isGui = false;
                    precedesEntities = true;
                    ShaderUtil.fastLightProgram.useShader();
                    ShaderUtil.fastLightProgram.setUniform("ticks", ticks + Minecraft.getInstance().getRenderPartialTicks());
                    ShaderUtil.fastLightProgram.setUniform("sampler", 0);
                    ShaderUtil.fastLightProgram.setUniform("lightmap", 1);
                    ShaderUtil.fastLightProgram.setUniform("playerPos", (float) Minecraft.getInstance().player.posX, (float) Minecraft.getInstance().player.posY, (float) Minecraft.getInstance().player.posZ);
                    if (!postedLights) {
                        if (thread == null || !thread.isAlive()) {
                            startThread();
                        }
                        EXISTING.forEach((pos, lights) -> LightManager.lights.addAll(lights));
                        LightManager.update(Minecraft.getInstance().world);
                        ShaderManager.stopShader();
                        MinecraftForge.EVENT_BUS.post(new LightUniformEvent());
                        ShaderUtil.fastLightProgram.useShader();
                        LightManager.uploadLights();
                        ShaderUtil.entityLightProgram.useShader();
                        ShaderUtil.entityLightProgram.setUniform("ticks", ticks + Minecraft.getInstance().getRenderPartialTicks());
                        ShaderUtil.entityLightProgram.setUniform("sampler", 0);
                        ShaderUtil.entityLightProgram.setUniform("lightmap", 1);
                        LightManager.uploadLights();
                        ShaderUtil.entityLightProgram.setUniform("playerPos", (float) Minecraft.getInstance().player.posX, (float) Minecraft.getInstance().player.posY, (float) Minecraft.getInstance().player.posZ);
                        ShaderUtil.entityLightProgram.setUniform("lightingEnabled", GL11.glIsEnabled(GL11.GL_LIGHTING));
                        ShaderUtil.fastLightProgram.useShader();
                        postedLights = true;
                        LightManager.clear();
                    }
                }
                if (event.getSection().compareTo("sky") == 0) {
                    ShaderManager.stopShader();
                }
                if (event.getSection().compareTo("litParticles") == 0) {
                    ShaderUtil.fastLightProgram.useShader();
                    ShaderUtil.fastLightProgram.setUniform("sampler", 0);
                    ShaderUtil.fastLightProgram.setUniform("lightmap", 1);
                    ShaderUtil.fastLightProgram.setUniform("playerPos", (float) Minecraft.getInstance().player.posX, (float) Minecraft.getInstance().player.posY, (float) Minecraft.getInstance().player.posZ);
                    ShaderUtil.fastLightProgram.setUniform("chunkX", 0);
                    ShaderUtil.fastLightProgram.setUniform("chunkY", 0);
                    ShaderUtil.fastLightProgram.setUniform("chunkZ", 0);
                }
                if (event.getSection().compareTo("particles") == 0) {
                    ShaderManager.stopShader();
                }
                if (event.getSection().compareTo("weather") == 0) {
                    ShaderManager.stopShader();
                }
                if (event.getSection().compareTo("entities") == 0) {
                    if (Minecraft.getInstance().isOnExecutionThread())
                        ShaderUtil.entityLightProgram.useShader();
                    ShaderUtil.entityLightProgram.setUniform("lightingEnabled", true);
                    ShaderUtil.entityLightProgram.setUniform("fogIntensity", 1.0f);
                }
            }
            if (event.getSection().compareTo("blockEntities") == 0) {
                if (Minecraft.getInstance().isOnExecutionThread())
                    ShaderUtil.entityLightProgram.useShader();
                ShaderUtil.entityLightProgram.setUniform("lightingEnabled", true);
            }

            if (event.getSection().compareTo("outline") == 0) {
                ShaderManager.stopShader();
            }
            if (event.getSection().compareTo("aboveClouds") == 0) {
                ShaderManager.stopShader();
            }
            if (event.getSection().compareTo("destroyProgress") == 0) {
                ShaderManager.stopShader();
            }
            if (event.getSection().compareTo("translucent") == 0) {
                ShaderUtil.fastLightProgram.useShader();
                ShaderUtil.fastLightProgram.setUniform("sampler", 0);
                ShaderUtil.fastLightProgram.setUniform("lightmap", 1);
                ShaderUtil.fastLightProgram.setUniform("playerPos", (float) Minecraft.getInstance().player.posX, (float) Minecraft.getInstance().player.posY, (float) Minecraft.getInstance().player.posZ);
            }
            if (event.getSection().compareTo("hand") == 0) {
                ShaderUtil.entityLightProgram.useShader();
                ShaderUtil.fastLightProgram.setUniform("entityPos", (float) Minecraft.getInstance().player.posX, (float) Minecraft.getInstance().player.posY, (float) Minecraft.getInstance().player.posZ);
                precedesEntities = true;
            }
            if (event.getSection().compareTo("gui") == 0) {
                isGui = true;
                ShaderManager.stopShader();
            }
        }


        @SubscribeEvent
        public static void onRenderEntity(RenderEntityEvent event) {

            LOGGER.info("onRenderEntity");

            if (ConfigManager.isLightingEnabled()) {
                if (event.getEntity() instanceof LightningBoltEntity) {
                    ShaderManager.stopShader();
                } else if (section.equalsIgnoreCase("entities") || section.equalsIgnoreCase("blockEntities")) {
                    ShaderUtil.entityLightProgram.useShader();
                }
                if (ShaderManager.isCurrentShader(ShaderUtil.entityLightProgram)) {
                    ShaderUtil.entityLightProgram.setUniform("entityPos", (float) event.getEntity().posX, (float) event.getEntity().posY + event.getEntity().getHeight() / 2.0f, (float) event.getEntity().posZ);
                    //ShaderUtil.entityLightProgram.setUniform("colorMult", 1f, 1f, 1f, 0f);
                    //if (event.getEntity() instanceof EntityLivingBase) {
                    //    EntityLivingBase e = (EntityLivingBase) event.getEntity();
                    //    if (e.hurtTime > 0 || e.deathTime > 0) {
                    //        ShaderUtil.entityLightProgram.setUniform("colorMult", 1f, 0f, 0f, 0.3f);
                    //    }
                    //}
                }
            }
        }

        @SubscribeEvent
        public static void onRenderTileEntity(RenderTileEntityEvent event) {
            if (ConfigManager.isLightingEnabled()) {
                if (event.getEntity() instanceof EndPortalTileEntity || event.getEntity() instanceof EndGatewayTileEntity) {
                    ShaderManager.stopShader();
                } else if (section.equalsIgnoreCase("entities") || section.equalsIgnoreCase("blockEntities")) {
                    ShaderUtil.entityLightProgram.useShader();
                }
                if (ShaderManager.isCurrentShader(ShaderUtil.entityLightProgram)) {
                    ShaderUtil.entityLightProgram.setUniform("entityPos", (float) event.getEntity().getPos().getX(), (float) event.getEntity().getPos().getY(), (float) event.getEntity().getPos().getZ());
                    //ShaderUtil.entityLightProgram.setUniform("colorMult", 1f, 1f, 1f, 0f);
                }
            }
        }

        @SubscribeEvent
        public static void onRenderChunk(RenderChunkUniformsEvent event) {
            if (ConfigManager.isLightingEnabled()) {
                if (ShaderManager.isCurrentShader(ShaderUtil.fastLightProgram)) {
                    BlockPos pos = event.getChunk().getPosition();
                    ShaderUtil.fastLightProgram.setUniform("chunkX", pos.getX());
                    ShaderUtil.fastLightProgram.setUniform("chunkY", pos.getY());
                    ShaderUtil.fastLightProgram.setUniform("chunkZ", pos.getZ());
                }
            }
        }

        @SubscribeEvent
        public static void clientTick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.START) {
                ticks++;
            }
        }

        @SubscribeEvent
        public static void onRenderWorldLast(RenderWorldLastEvent event) {
            postedLights = false;
            if (Minecraft.getInstance().isOnExecutionThread()) {
                GlStateManager.disableLighting();
                ShaderManager.stopShader();
            }
        }
    }



    public static void startThread() {
        thread = new Thread(() -> {
            while (!thread.isInterrupted()) {
                if (Minecraft.getInstance().player != null) {
                    PlayerEntity player = Minecraft.getInstance().player;
                    if (Minecraft.getInstance().world != null) {
                        IWorldReader reader = Minecraft.getInstance().world;
                        BlockPos playerPos = player.getPosition();
                        int maxDistance = ConfigManager.maxDistance.get();
                        int r = maxDistance / 2;


                        // actually solved(?) by MutableBlockPos.
                        Iterable<BlockPos> posIterable = MutableBlockPos.getAllInBoxMutable(playerPos.add(-r, -r, -r), playerPos.add(r, r, r));

                        // [TODO] iterator error (concurrent shit) resolve
                        for (BlockPos pos : posIterable) {
                            Vec3d cameraPosition = LightManager.cameraPos;
                            ICamera camera = LightManager.camera;
                            BlockState state = reader.getBlockState(pos);
                            ArrayList<Light> lights = new ArrayList<>();
                            GatherLightsEvent lightsEvent = new GatherLightsEvent(lights, maxDistance, cameraPosition, camera);
                            TriConsumer<BlockPos, BlockState, GatherLightsEvent> consumer = getLightHandler(state.getBlock());
                            if (consumer != null)
                                consumer.apply(pos, state, lightsEvent);
                            if (lights.isEmpty()) {
                                EXISTING.remove(pos);
                            } else {
                                EXISTING.put(pos.toImmutable(), lights);
                            }
                        }
                    }
                }

                LOGGER.info("Color thread is running");
            }
        });
        thread.start();
    }

}

