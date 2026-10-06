package nl.rutgerkok.blocklocker.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.TreeType;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import nl.rutgerkok.blocklocker.AttackType;
import nl.rutgerkok.blocklocker.ChestSettings;
import nl.rutgerkok.blocklocker.impl.event.BlockDestroyListener;

public class TreeTypeBlacklistTest {

    @Test
    public void testDefaultTreeTypeBlacklist() {
        Plugin plugin = Mockito.mock(Plugin.class);
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("configVersion", 2);
        Mockito.when(plugin.getConfig()).thenReturn(configuration);
        Mockito.when(plugin.getLogger()).thenReturn(Logger.getLogger("TestLogger"));

        Config config = new Config(plugin);
        Set<TreeType> blacklist = config.getTreeTypeBlacklist();

        // 26 total TreeTypes in 1.21.10 minus 2 mushrooms = 24
        assertEquals(24, blacklist.size());
        assertFalse(config.isTreeTypeBlacklisted(TreeType.BROWN_MUSHROOM));
        assertFalse(config.isTreeTypeBlacklisted(TreeType.RED_MUSHROOM));
        assertTrue(config.isTreeTypeBlacklisted(TreeType.TREE));
        assertTrue(config.isTreeTypeBlacklisted(TreeType.BIG_TREE));
        assertTrue(config.isTreeTypeBlacklisted(TreeType.ACACIA));
        assertTrue(config.isTreeTypeBlacklisted(TreeType.DARK_OAK));
        assertTrue(config.isTreeTypeBlacklisted(TreeType.PALE_OAK));
        assertTrue(config.isTreeTypeBlacklisted(TreeType.PALE_OAK_CREAKING));
        assertFalse(config.isTreeTypeBlacklisted(null));
    }

    @Test
    public void testCustomTreeTypeBlacklist() {
        Plugin plugin = Mockito.mock(Plugin.class);
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("configVersion", 2);
        configuration.set("treeTypeBlacklist", List.of("BROWN_MUSHROOM", "ACACIA"));
        Mockito.when(plugin.getConfig()).thenReturn(configuration);
        Mockito.when(plugin.getLogger()).thenReturn(Logger.getLogger("TestLogger"));

        Config config = new Config(plugin);
        assertTrue(config.isTreeTypeBlacklisted(TreeType.BROWN_MUSHROOM));
        assertTrue(config.isTreeTypeBlacklisted(TreeType.ACACIA));
        assertFalse(config.isTreeTypeBlacklisted(TreeType.RED_MUSHROOM));
        assertFalse(config.isTreeTypeBlacklisted(TreeType.TREE));
    }

    @Test
    public void testEmptyTreeTypeBlacklist() {
        Plugin plugin = Mockito.mock(Plugin.class);
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("configVersion", 2);
        configuration.set("treeTypeBlacklist", List.of());
        Mockito.when(plugin.getConfig()).thenReturn(configuration);
        Mockito.when(plugin.getLogger()).thenReturn(Logger.getLogger("TestLogger"));

        Config config = new Config(plugin);
        assertEquals(0, config.getTreeTypeBlacklist().size());
        assertFalse(config.isTreeTypeBlacklisted(TreeType.TREE));
        assertFalse(config.isTreeTypeBlacklisted(TreeType.BROWN_MUSHROOM));
    }

    @Test
    public void testOnStructureGrowBlacklistedReturnsEarly() {
        BlockLockerPluginImpl plugin = Mockito.mock(BlockLockerPluginImpl.class);
        ChestSettings settings = Mockito.mock(ChestSettings.class);
        Mockito.when(plugin.getChestSettings()).thenReturn(settings);
        Mockito.when(settings.allowDestroyBy(AttackType.SAPLING)).thenReturn(false);
        Mockito.when(settings.isTreeTypeBlacklisted(TreeType.TREE)).thenReturn(true);
        Mockito.when(settings.isTreeTypeBlacklisted(TreeType.BROWN_MUSHROOM)).thenReturn(false);

        BlockDestroyListener listener = new BlockDestroyListener(plugin);

        StructureGrowEvent blacklistedEvent = Mockito.mock(StructureGrowEvent.class);
        Mockito.when(blacklistedEvent.getSpecies()).thenReturn(TreeType.TREE);

        listener.onStructureGrow(blacklistedEvent);
        // Because TREE is blacklisted, it returns early and never inspects event blocks
        Mockito.verify(blacklistedEvent, Mockito.never()).getBlocks();
        Mockito.verify(blacklistedEvent, Mockito.never()).setCancelled(true);

        StructureGrowEvent nonBlacklistedEvent = Mockito.mock(StructureGrowEvent.class);
        Mockito.when(nonBlacklistedEvent.getSpecies()).thenReturn(TreeType.BROWN_MUSHROOM);
        Mockito.when(nonBlacklistedEvent.getBlocks()).thenReturn(List.of());

        listener.onStructureGrow(nonBlacklistedEvent);
        // Because BROWN_MUSHROOM is not blacklisted, it proceeds to inspect event blocks
        Mockito.verify(nonBlacklistedEvent, Mockito.times(1)).getBlocks();
    }
}
