package eu.darkbot.impl.galaxy;

import eu.darkbot.api.game.galaxy.GalaxyGate;
import eu.darkbot.api.game.galaxy.SpinResult;
import eu.darkbot.api.game.items.SelectableItem;
import eu.darkbot.util.XmlUtils;
import org.w3c.dom.Element;

import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

public class SpinResultImpl implements SpinResult {
    private final GalaxyInfoImpl galaxyInfo;

    private final Map<ItemType, SpinInfoImpl> itemsResult = new EnumMap<>(ItemType.class);
    private final Map<SelectableItem.Laser, SpinInfo> ammoResult;

    private int parts, multipliers;

    private Instant date;
    private GalaxyGate gate;

    SpinResultImpl(GalaxyInfoImpl galaxyInfo) {
        this.galaxyInfo = galaxyInfo;

        Map<SelectableItem.Laser, SpinInfo> ammoResult = new HashMap<>();

        for (ItemType item : ItemType.values()) {
            SelectableItem.Laser laser = item.laser;
            if (laser != null) ammoResult.put(laser, getSpinInfo(item));
        }

        this.ammoResult = Collections.unmodifiableMap(ammoResult);
    }

    void update(Stream<Element> itemsStream, GalaxyGate gate) {
        this.gate = gate;
        this.itemsResult.values().forEach(SpinInfoImpl::reset);

        this.date = null;

        itemsStream.forEach(itemElement -> {
            Optional.ofNullable(XmlUtils.attrToInt(itemElement, "gate_id"))
                    .map(GalaxyGate::of)
                    .map(galaxyGate -> (GateInfoImpl) galaxyInfo.getGateInfo(galaxyGate))
                    .ifPresent(gateInfo -> gateInfo.update(itemElement));

            if (date == null)
                date = Instant.ofEpochSecond(Long.parseLong(itemElement.getAttribute("date")));

            String itemType = itemElement.getAttribute("type");
            String itemId = itemElement.getAttribute("item_id");
            Integer amount = XmlUtils.attrToInt(itemElement, "amount");
            Integer spinsUsed = XmlUtils.attrToInt(itemElement, "spins");

            if (itemType.equals("part") && itemElement.getAttribute("duplicate") == null)
                parts++;
            else if (itemType.equals("multiplier") && amount != null)
                multipliers += amount;

            if (itemId != null) itemType += "-" + itemId;
            if (amount != null && spinsUsed != null)
                getSpinInfo(ItemType.of(itemType)).set(amount, spinsUsed);
        });
    }

    private SpinInfoImpl getSpinInfo(ItemType itemType) {
        return itemsResult.computeIfAbsent(itemType, i -> new SpinInfoImpl());
    }

    @Override
    public Instant getDate() {
        return date;
    }

    @Override
    public GalaxyGate getGalaxyGate() {
        return gate;
    }

    @Override
    public SpinInfo getMines() {
        return getSpinInfo(ItemType.MINES);
    }

    @Override
    public int getParts() {
        return parts;
    }

    @Override
    public SpinInfo getRockets() {
        return getSpinInfo(ItemType.ROCKETS);
    }

    @Override
    public SpinInfo getXenomit() {
        return getSpinInfo(ItemType.ORES);
    }

    @Override
    public SpinInfo getNanoHull() {
        return getSpinInfo(ItemType.NANO_HULL);
    }

    @Override
    public SpinInfo getLogFiles() {
        return getSpinInfo(ItemType.LOG_FILES);
    }

    @Override
    public SpinInfo getVouchers() {
        return getSpinInfo(ItemType.VOUCHERS);
    }

    @Override
    public int getMultipliers() {
        return multipliers;
    }

    @Override
    public Map<SelectableItem.Laser, SpinInfo> getAmmo() {
        return ammoResult;
    }

    public enum ItemType {
        MINES("rocket-11"),
        ROCKETS("rocket-3"),
        ORES("ore-4"),
        NANO_HULL("nanohull"),
        LOG_FILES("logfile"),
        VOUCHERS("voucher"),
        MCB_25("battery-2", SelectableItem.Laser.MCB_25),
        MCB_50("battery-3", SelectableItem.Laser.MCB_50),
        UCB_100("battery-4", SelectableItem.Laser.UCB_100),
        SAB_50("battery-5", SelectableItem.Laser.SAB_50);

        private final String typeName;
        private final SelectableItem.Laser laser;

        ItemType(String typeName) {
            this(typeName, null);
        }

        ItemType(String typeName, SelectableItem.Laser laser) {
            this.typeName = typeName;
            this.laser = laser;
        }

        private static ItemType of(String itemType) {
            for (ItemType item : values()) {
                if (item.typeName.equals(itemType))
                    return item;
            }
            return null;
        }
    }

    public static class SpinInfoImpl implements SpinInfo {
        private int obtained, spinsUsed;

        void set(int obtained, int spinsUsed) {
            this.obtained += obtained;
            this.spinsUsed += spinsUsed;
        }

        void reset() {
            this.obtained = 0;
            this.spinsUsed = 0;
        }

        @Override
        public int getObtained() {
            return obtained;
        }

        @Override
        public int getSpinsUsed() {
            return spinsUsed;
        }
    }
}
