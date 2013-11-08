/** Implementing and Using Forge's EventHandler */

/*
If you want to change any type of default Minecraft behavior, chances are there is a Forge Event that handles it.

There are Player Events, Living Events, Item Events, World Events, TerrainGenEvents, Minecart Events... there's just
so much you can do with these it's incredible.

I personally prefer using EventHandler over TickHandler for this very reason - most things you could ever want to do
already have a framework built to handle it, whereas Tick Handler you have to build it all yourself.

This tutorial will cover:
1. Building and using an Event Handler
2. Advanced information for handling events
3. A sampling of Event types and their possible uses
*/

/**
 * Step 1: Create TutEventHandler class
 */
/*
IMPORTANT!!! Do NOT name your EventHandler 'EventHandler' - that's already the name of a Forge class.
Also, do NOT edit any of the Forge classes. You need to create a new class to handle events.
*/

public class TutEventHandler
{
}

/**
 * Step 2: Register your event handler class to EVENT_BUS either in
 * 'load' or 'postInit' methods in your main mod
 */

@EventHandler
public void load(FMLInitializationEvent event)
{
	MinecraftForge.EVENT_BUS.register(new TutEventHandler());
}

// You're finished! That was easy  But it doesn't do anything right now,
// so on to step 3.

/**
 * Step 3: Look through MinecraftForge event types for ones you want to use
 * and add them to your TutEventHandler
 */

// In your TutEventHandler class - the name of the method doesn't matter
// Only the Event type parameter is what's important (see below for explanations of some types)
@ForgeSubscribe
public void onLivingUpdateEvent(LivingUpdateEvent event)
{
	// This event has an Entity variable, access it like this:
	event.entity;

	// do something to player every update tick:
	if (event.entity instanceof EntityPlayer)
	{
		EntityPlayer player = (EntityPlayer) event.entity;
		ItemStack heldItem = player.getHeldItem();
		if (heldItem != null && heldItem.itemID == Item.arrow.itemID) {
			player.capabilities.allowFlying = true;
		}
		else {
			player.capabilities.allowFlying = player.capabilities.isCreativeMode ? true : false;
		}
	}
}

// If you're ever curious what variables the Event stores, type 'event.'
// in Eclipse and it will bring up a menu of all the methods and variables.
// Or go to the implementation by ctrl-clicking on the class name.

/**
 * Step 4: Using Events in your custom classes
 * Forge Events are all hooked into automatically from vanilla code, but say
 * you made a custom Bow and want it to use ArrowNock and ArrowLoose events?
 * You need to post them to the event bus in your item code.
 */

/** ArrowNockEvent should be placed in 'onItemRightClick' */
@Override
public ItemStack onItemRightClick(ItemStack itemstack, World world, EntityPlayer player)
{
	// Create the event and post it
	ArrowNockEvent event = new ArrowNockEvent(player, itemstack);
	MinecraftForge.EVENT_BUS.post(event);

	if (event.isCanceled())
	{
		// you could do other stuff here as well
		return event.result;
	}

	player.setItemInUse(itemstack, this.getMaxItemUseDuration(itemstack));

	return itemstack;
}

/** ArrowLooseEvent should be placed in 'onPlayerStoppedUsing' */
@Override
public void onPlayerStoppedUsing(ItemStack itemstack, World world, EntityPlayer player, int par4)
{
	// Ticks in use is max duration minus par4, which is equal to max duration - 1 for every tick in use
	int ticksInUse = this.getMaxItemUseDuration(itemstack) - par4;

	ArrowLooseEvent event = new ArrowLooseEvent(player, itemstack, ticksInUse);
	MinecraftForge.EVENT_BUS.post(event);

	if (event.isCanceled()) { return; }

	// ticksInUse might be modified by the Event in your EventHandler, so reassign it here:
	ticksInUse = event.charge;

	// Do whatever else you want with the itemstack like fire an arrow or cast a spell
}

/**
 * Step 5: Adding events to your TutEventHandler
 */
/* A template for whatever event method you want to make. Name it whatever you want,
   but use the correct Event Type from above. Event variables are accessed by using 
   'event.variableName' I give the variable names for many Events below.
 */

@ForgeSubscribe
public void methodName(EventType event)
{
	// do whatever you want here
}

/*
 * Advanced Information: Setting Priority
 */
/*
Priority is the order in which all listeners listening to a posted event are called. A listener is a method with the
@ForgeSubscribe annotation, and is said to be actively listening if the class containing the method was registered to
the MinecraftForge EVENT_BUS. Whenever an event is posted matching the parameters of the listener, the listener's
method is called.

When there are multiple listeners listening to a single event, the order in which they are called is important if one
listener's functionality relies on having first or last access to the event in process, or if it relies on information
set by a prior listener. This can be especially useful with Cancelable events.

A single Event can be handled multiple times by different handlers or even within the same handler provided the methods
have different names:
*/
@ForgeSubscribe
public void onLivingHurt(LivingHurtEvent event) {}

@ForgeSubscribe
public void onPlayerHurt(LivingHurtEvent event) {}
/*
Both methods will be called each time a LivingHurtEvent is posted to the EVENT_BUS in the order they are added to
the event listener (see IEventListener), which in the case above is simply their order in the code. The order can
be controlled by appending (priority=VALUE) to the @ForgeSubscribe annotation, where VALUE is defined in the
EventPriority enum class. HIGHEST priority is always called first, while LOWEST priority is called last.
*/
// this method will now be called after 'onPlayerHurt()'
@ForgeSubscribe(priority=LOWEST)
public void onLivingHurt(LivingHurtEvent event) {}

@ForgeSubscribe(priority=HIGHEST)
public void onPlayerHurt(LivingHurtEvent event) {}
/*
If two listeners have the same priority level, then the order is again controlled by the order in which they are added.
In order to control the flow for such a case within a mod, the methods can be placed in separate 'event handler'
classes to be registered in the order desired:
*/
// In the case of identical priority levels, PlayerHurtHandler will process first
MinecraftForge.EVENT_BUS.register(new PlayerHurtHandler());
MinecraftForge.EVENT_BUS.register(new LivingHurtHandler());
// For multiple Mods that affect the same events, the order of mod registration would have the same effect.
/*
 * Advanced Information: Cancelable Events
 */
/*
Events with the @Cancelable annotation have the special quality of being cancelable. Once an event is canceled,
subsequent listeners will not process the event unless provided with special annotation:
*/
@ForgeSubscribe // default priority, so it will be called first
public void onLivingHurt(LivingHurtEvent event) {
event.setCanceled(true);
}

@ForgeSubscribe(priority=LOWEST, receiveCanceled=true)
public void onPlayerHurt(LivingHurtEvent event) {
// un-cancel the event
event.setCanceled(false);
}
/*
By controlling the order in which each listener method is called, it is usually possible to avoid un-canceling a
previously canceled event, although exceptional circumstances do arise; in those cases, extra care must be taken
to avoid making logical errors.

More will be added as I learn. Thanks to GotoLink for his excellent explanations regarding priority.
*/

/*
 Now for some examples of Event types, their variables, when they are called and
 what you might do with them:

1. ArrowNockEvent
Variables: EntityPlayer player, ItemStack result
Usually called from 'onItemRightClick'.
Uses: It is cancelable, so if some conditions are not met (e.g. no arrows in inventory) you could cancel it and stop
the player from setting the item in use. One thing I use it for is to set a boolean 'isAiming', which I can then
interrupt if the player takes damage or some such (using another boolean 'wasInterrupted')

2. ArrowLooseEvent
Variables: EntityPlayer player, ItemStack bow, int charge
Usually called from 'onPlayerStoppedUsing'. It is also cancelable.
Uses: I use it in tandem with the above to check if the player was interrupted, and if so, cancel the event.

3. EntityConstructing
Variables: Entity entity
Called for every Entity when its constructor is called.
Uses: Useful if you need to add ExtendedEntityProperties.

4. EntityJoinWorldEvent
Variables: Entity entity, World world
Called when an entity joins the world for the first time.
Uses: Useful for synchronizing ExtendedEntityProperties, giving your player an item when spawned or any other number
of things.

5. LivingUpdateEvent
Variables: EntityLivingBase entity
Called every tick at the beginning of the entity's onUpdate method.
Uses: This is probably the most useful Event. You can allow player's to fly if holding an item or wearing your armor
set, you can modify a player's fall speed here, add potion effects or anything else you can imagine. It's really
really handy.

6. LivingDropsEvent
Variables: EntityLivingBase entity, DamageSource source, ArrayList<EntityItem> drops, int lootingLevel, boolean
recentlyHit, int specialDropValue
Called when an entity is killed and drops items.
Uses: Handy if you want to modify a vanilla mobs drops or only drop your custom item if it was killed from your custom
DamageSource. You can also remove items from drops, adjust it based on the looting enchantment level of the item used
to kill it, etc. Pretty useful.

7. LivingFallEvent
Variables: EntityLivingBase entity, float distance
Called when the entity hits the ground after a fall, but before damage is calculated.
SPECIAL NOTE: This event is NOT called while in Creative Mode; PlayerFlyableFallEvent is called instead
Uses: It is cancelable, so 'event.setCanceled(true)' will preclude further processing of the fall.
You can also modify the distance fallen here, but keep in mind this is ONLY on impact. If you want
to modify fall distance only while certain conditions are met, better to do it in LivingUpdateEvent.
Also, be sure you are modifying 'event.distance' and NOT 'entity.fallDistance' or you won't change the outcome of the
fall.

8. LivingJumpEvent
Variables: EntityLivingBase entity
Called whenever entity jumps.
Uses: Useful for entity.motionY += 10.0D. Just give it a try 

9. LivingAttackEvent
Variables: EntityLivingBase entity, DamageSource source, float ammount
Called when an entity is attacked, but before any damage is applied
Uses: Cancelable. Here you can do pre-processing of an attack before LivingHurtEvent is called. The source entity of
the attack is stored in DamageSource, and you can adjust the damage to be dealt however you see fit. Basically the
same uses as LivingHurtEvent, but done sooner.

10. LivingHurtEvent
Variables: EntityLivingBase entity, DamageSource source, float ammount
Called when an entity is damaged, but before any damage is applied
Uses: Another super useful one if you have custom armor that reduces fire damage, increases damage taken from magic,
do something if ammount is greater than current health or whatever.

11. LivingDeathEvent
Variables: EntityLivingBase entity, DamageSource source
Called when an entity dies; cancelable!
Uses: Recall that DamageSource has lots of variables, too, such as getEntity() that returns the entity that caused the
damage and thus that killed the current entity. All sorts of things you could do with that. This is also the place to
cancel death and resurrect yourself, or set a timer for resurrection. If you have data you want to persist through
player death, such as IExtendedEntityProperties, you can save that here as well.

12. EntityInteractEvent
Variables: EntityPlayer player, Entity target
Called when the player right-clicks on an entity, such as a cow
Uses: Gee, you could do anything with this. One use could be getting milk into your custom bucket...

13. EntityItemPickupEvent
Variables: EntityPlayer player, EntityItem item
Called when the player picks up an item
Uses: This one is useful for special items that need handling on pickup; an example would be if you made something
similar to experience orbs, mana orbs for instance, that replenish mana rather than adding an item to inventory

14. HarvestCheck
Variables: EntityPlayer player, Block block, boolean success
Called when the player breaks a block before the block releases its drops
Uses: Coupled with the BreakSpeed event, this is perhaps the best way to change the behavior of mining.
*/
