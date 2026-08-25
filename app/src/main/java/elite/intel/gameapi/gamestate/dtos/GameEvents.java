package elite.intel.gameapi.gamestate.dtos;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.gameapi.StationName;
import elite.intel.util.json.GsonFactory;
import elite.intel.util.json.ToJsonConvertible;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

import java.util.List;

public class GameEvents {

    public static class CargoEvent implements ToJsonConvertible, ToYamlConvertable {
        /**
         * Journal name of limpet drones; {@code Name_Localised} is "Limpet".
         */
        private static final String DRONES = "drones";

        @SerializedName("timestamp")
        private String timestamp;
        @SerializedName("event")
        private String event;
        @SerializedName("Vessel")
        private String vessel;
        @SerializedName("Count")
        private int count;
        @SerializedName("Inventory")
        private List<Inventory> inventory;


        // Getters
        public String getTimestamp() {
            return timestamp;
        }

        public String getEvent() {
            return event;
        }

        public String getVessel() {
            return vessel;
        }

        public int getCount() {
            return count;
        }

        public List<Inventory> getInventory() {
            return inventory;
        }

        /**
         * Cargo count excluding limpet drones. Limpets occupy the hold and are counted by
         * {@link #getCount()}, but they are equipment rather than trade goods, so a hold holding
         * nothing but limpets is empty as far as trading is concerned.
         * Falls back to the reported total when the inventory breakdown is absent.
         */
        public int getTradeableCount() {
            if (inventory == null || inventory.isEmpty()) {
                return count;
            }
            int tradeable = 0;
            for (Inventory item : inventory) {
                if (DRONES.equalsIgnoreCase(item.getName())) {
                    continue;
                }
                tradeable += (int) item.getCount();
            }
            return tradeable;
        }

        /**
         * Limpets currently in the hold.
         */
        public int getDroneCount() {
            if (inventory == null) return 0;
            for (Inventory item : inventory) {
                if (DRONES.equalsIgnoreCase(item.getName())) return (int) item.getCount();
            }
            return 0;
        }

        /**
         * Takes one limpet out of the hold, reporting whether there was one to take.
         * <p>
         * The game does not rewrite {@code Cargo.json} when a limpet is launched - it only
         * writes a {@code LaunchDrone} journal line - so between cargo snapshots this is the
         * only thing keeping the count honest. It is a correction applied to a snapshot,
         * never a tally of its own: the next {@code Cargo} event replaces this whole object
         * and the game's number wins.
         * <p>
         * The row is kept at zero rather than removed, so a hold that has run dry still says
         * so instead of going quiet.
         */
        public boolean launchDrone() {
            if (inventory == null) return false;
            for (Inventory item : inventory) {
                if (!DRONES.equalsIgnoreCase(item.getName())) continue;
                if (item.count < 1) return false;
                item.count -= 1;
                if (count > 0) count -= 1;
                return true;
            }
            return false;
        }

        public String toJson() {
            return GsonFactory.getGson().toJson(this);
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }

        public void setEvent(String event) {
            this.event = event;
        }

        public void setVessel(String vessel) {
            this.vessel = vessel;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public void setInventory(List<Inventory> inventory) {
            this.inventory = inventory;
        }

        @Override
        public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }


    public static class Inventory {
        //      "Name": "tea",
        //      "Count": 240.0,
        //      "Stolen": 0.0
        @SerializedName("Name")
        private String name;
        @SerializedName("Count")
        private double count;
        @SerializedName("Stolen")
        private double stolen;
        @SerializedName("Name_Localised")
        private String nameLocalised;

        public String getName() {
            return name;
        }

        public double getCount() {
            return count;
        }

        public double getStolen() {
            return stolen;
        }

        public String getNameLocalised() {
            return nameLocalised;
        }
    }

    public static class ModulesInfoEvent {
        public static class Module {
            @SerializedName("Slot")
            private String slot;
            @SerializedName("Item")
            private String item;
            @SerializedName("Power")
            private double power;
            @SerializedName("Priority")
            private int priority;


            // Getters
            public String getSlot() {
                return slot;
            }

            public String getItem() {
                return item;
            }

            public double getPower() {
                return power;
            }

            public int getPriority() {
                return priority;
            }

            public String toJson() {
                return GsonFactory.getGson().toJson(this);
            }

            public void setSlot(String slot) {
                this.slot = slot;
            }

            public void setItem(String item) {
                this.item = item;
            }

            public void setPower(double power) {
                this.power = power;
            }

            public void setPriority(int priority) {
                this.priority = priority;
            }
        }

        @SerializedName("timestamp")
        private String timestamp;
        @SerializedName("event")
        private String event;
        @SerializedName("Modules")
        private List<Module> modules;

        // Getters
        public String getTimestamp() {
            return timestamp;
        }

        public String getEvent() {
            return event;
        }

        public List<Module> getModules() {
            return modules;
        }
    }

    public static class StatusEvent {
        public static class Fuel {
            @SerializedName("FuelMain")
            private double fuelMain;
            @SerializedName("FuelReservoir")
            private double fuelReservoir;

            // Getters
            public double getFuelMain() {
                return fuelMain;
            }

            public double getFuelReservoir() {
                return fuelReservoir;
            }

            public String toJson() {
                return GsonFactory.getGson().toJson(this);
            }

            public void setFuelMain(double fuelMain) {
                this.fuelMain = fuelMain;
            }

            public void setFuelReservoir(double fuelReservoir) {
                this.fuelReservoir = fuelReservoir;
            }
        }

        public static class Destination {
            @SerializedName("System")
            private long system;
            @SerializedName("Body")
            private long body;
            @SerializedName("Name")
            private String name;

            // Getters
            public long getSystem() {
                return system;
            }

            public long getBody() {
                return body;
            }

            public String getName() {
                return name;
            }

            public String toJson() {
                return GsonFactory.getGson().toJson(this);
            }

            public void setSystem(long system) {
                this.system = system;
            }

            public void setBody(long body) {
                this.body = body;
            }

            public void setName(String name) {
                this.name = name;
            }
        }

        @SerializedName("timestamp")
        private String timestamp;
        @SerializedName("event")
        private String event;
        @SerializedName("Flags")
        private long flags;
        @SerializedName("Flags2")
        private long flags2;
        @SerializedName("Pips")
        private int[] pips;
        @SerializedName("FireGroup")
        private int fireGroup;
        @SerializedName("GuiFocus")
        private int guiFocus;
        @SerializedName("Fuel")
        private Fuel fuel;
        @SerializedName("Cargo")
        private double cargo;
        @SerializedName("LegalState")
        private String legalState;
        @SerializedName("Latitude")
        private double latitude;
        @SerializedName("Longitude")
        private double longitude;
        @SerializedName("Heading")
        private int heading;
        @SerializedName("Altitude")
        private double altitude;
        @SerializedName("Balance")
        private long balance;
        @SerializedName("Destination")
        private Destination destination;
        @SerializedName("PlanetRadius")
        private double planetRadius;
        @SerializedName("BodyName")
        private String bodyName;
        // On-foot fields (Odyssey only – absent when in ship/SRV)
        @SerializedName("Oxygen")
        private Double oxygen;
        @SerializedName("Health")
        private Double health;
        @SerializedName("Temperature")
        private Double temperature;
        @SerializedName("SelectedWeapon")
        private String selectedWeapon;
        @SerializedName("Gravity")
        private Double gravity;


        // Getters
        public String getTimestamp() {
            return timestamp;
        }

        public String getEvent() {
            return event;
        }

        public long getFlags() {
            return flags;
        }

        public long getFlags2() {
            return flags2;
        }

        public int[] getPips() {
            return pips;
        }

        public int getFireGroup() {
            return fireGroup;
        }

        public int getGuiFocus() {
            return guiFocus;
        }

        public Fuel getFuel() {
            return fuel;
        }

        public double getCargo() {
            return cargo;
        }

        public String getLegalState() {
            return legalState;
        }

        public long getBalance() {
            return balance;
        }

        public Destination getDestination() {
            return destination;
        }

        public String toJson() {
            return GsonFactory.getGson().toJson(this);
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        public int getHeading() {
            return heading;
        }

        public double getAltitude() {
            return altitude;
        }

        public double getPlanetRadius() {
            return planetRadius;
        }

        public String getBodyName() {
            return bodyName;
        }

        public Double getOxygen() {
            return oxygen;
        }

        public Double getHealth() {
            return health;
        }

        public Double getTemperature() {
            return temperature;
        }

        public String getSelectedWeapon() {
            return selectedWeapon;
        }

        public Double getGravity() {
            return gravity;
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }

        public void setEvent(String event) {
            this.event = event;
        }

        public void setFlags(long flags) {
            this.flags = flags;
        }

        public void setFlags2(long flags2) {
            this.flags2 = flags2;
        }

        public void setPips(int[] pips) {
            this.pips = pips;
        }

        public void setFireGroup(int fireGroup) {
            this.fireGroup = fireGroup;
        }

        public void setGuiFocus(int guiFocus) {
            this.guiFocus = guiFocus;
        }

        public void setFuel(Fuel fuel) {
            this.fuel = fuel;
        }

        public void setCargo(double cargo) {
            this.cargo = cargo;
        }

        public void setLegalState(String legalState) {
            this.legalState = legalState;
        }

        public void setLatitude(double latitude) {
            this.latitude = latitude;
        }

        public void setLongitude(double longitude) {
            this.longitude = longitude;
        }

        public void setHeading(int heading) {
            this.heading = heading;
        }

        public void setAltitude(double altitude) {
            this.altitude = altitude;
        }

        public void setBalance(long balance) {
            this.balance = balance;
        }

        public void setDestination(Destination destination) {
            this.destination = destination;
        }

        public void setPlanetRadius(double planetRadius) {
            this.planetRadius = planetRadius;
        }

        public void setBodyName(String bodyName) {
            this.bodyName = bodyName;
        }

        public void setOxygen(Double oxygen) {
            this.oxygen = oxygen;
        }

        public void setHealth(Double health) {
            this.health = health;
        }

        public void setTemperature(Double temperature) {
            this.temperature = temperature;
        }

        public void setSelectedWeapon(String selectedWeapon) {
            this.selectedWeapon = selectedWeapon;
        }

        public void setGravity(Double gravity) {
            this.gravity = gravity;
        }
    }

    public static class BackpackEvent {
        @SerializedName("timestamp")
        private String timestamp;
        @SerializedName("event")
        private String event;
        @SerializedName("Items")
        private List<Object> items;
        @SerializedName("Components")
        private List<Object> components;
        @SerializedName("Consumables")
        private List<Object> consumables;
        @SerializedName("Data")
        private List<Object> data;


        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }

        public void setEvent(String event) {
            this.event = event;
        }

        public void setItems(List<Object> items) {
            this.items = items;
        }

        public void setComponents(List<Object> components) {
            this.components = components;
        }

        public void setConsumables(List<Object> consumables) {
            this.consumables = consumables;
        }

        public void setData(List<Object> data) {
            this.data = data;
        }

        // Getters
        public String getTimestamp() {
            return timestamp;
        }

        public String getEvent() {
            return event;
        }

        public List<Object> getItems() {
            return items;
        }

        public List<Object> getComponents() {
            return components;
        }

        public List<Object> getConsumables() {
            return consumables;
        }

        public List<Object> getData() {
            return data;
        }

        public String toJson() {
            return GsonFactory.getGson().toJson(this);
        }


    }

    public static class NavRouteEvent {
        public static class RouteEntry {
            @SerializedName("StarSystem")
            private String starSystem;
            @SerializedName("SystemAddress")
            private long systemAddress;
            @SerializedName("StarPos")
            private float[] starPos;
            @SerializedName("StarClass")
            private String starClass;


            public void setStarSystem(String starSystem) {
                this.starSystem = starSystem;
            }

            public void setSystemAddress(long systemAddress) {
                this.systemAddress = systemAddress;
            }

            public void setStarPos(float[] starPos) {
                this.starPos = starPos;
            }

            public void setStarClass(String starClass) {
                this.starClass = starClass;
            }

            // Getters
            public String getStarSystem() {
                return starSystem;
            }

            public long getSystemAddress() {
                return systemAddress;
            }

            public float[] getStarPos() {
                return starPos;
            }

            public String getStarClass() {
                return starClass;
            }

            public boolean isFuelStart() {
                return "KGBFOAM".contains(starClass.toUpperCase());
            }

            public String toJson() {
                return GsonFactory.getGson().toJson(this);
            }
        }

        @SerializedName("timestamp")
        private String timestamp;
        @SerializedName("event")
        private String event;
        @SerializedName("Route")
        private List<RouteEntry> route;

        // Getters
        public String getTimestamp() {
            return timestamp;
        }

        public String getEvent() {
            return event;
        }

        public List<RouteEntry> getRoute() {
            return route;
        }

        public String toJson() {
            return GsonFactory.getGson().toJson(this);
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }

        public void setEvent(String event) {
            this.event = event;
        }

        public void setRoute(List<RouteEntry> route) {
            this.route = route;
        }
    }

    public static class FCMaterialsEvent {
        @SerializedName("timestamp")
        private String timestamp;
        @SerializedName("event")
        private String event;
        @SerializedName("MarketID")
        private long marketID;
        @SerializedName("CarrierName")
        private String carrierName;
        @SerializedName("CarrierID")
        private String carrierID;
        @SerializedName("Items")
        private List<Object> items;


        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }

        public void setEvent(String event) {
            this.event = event;
        }

        public void setMarketID(long marketID) {
            this.marketID = marketID;
        }

        public void setCarrierName(String carrierName) {
            this.carrierName = carrierName;
        }

        public void setCarrierID(String carrierID) {
            this.carrierID = carrierID;
        }

        public void setItems(List<Object> items) {
            this.items = items;
        }

        // Getters
        public String getTimestamp() {
            return timestamp;
        }

        public String getEvent() {
            return event;
        }

        public long getMarketID() {
            return marketID;
        }

        public String getCarrierName() {
            return carrierName;
        }

        public String getCarrierID() {
            return carrierID;
        }

        public List<Object> getItems() {
            return items;
        }

        public String toJson() {
            return GsonFactory.getGson().toJson(this);
        }
    }

    public static class OutfittingEvent {
        @SerializedName("timestamp")
        private String timestamp;
        @SerializedName("event")
        private String event;
        @SerializedName("MarketID")
        private long marketID;
        @SerializedName("StationName")
        private String stationName;
        @SerializedName("StarSystem")
        private String starSystem;
        @SerializedName("Horizons")
        private boolean horizons;
        @SerializedName("Items")
        private List<OutfittingItem> items;

        public static class OutfittingItem {
            @SerializedName("id")
            private Long id;
            @SerializedName("Name")
            private String name;
            @SerializedName("Name_Localised")
            private String nameLocalised;
            @SerializedName("BuyPrice")
            private Long buyPrice;
            @SerializedName("Stock")
            private Integer stock;
            @SerializedName("Category")
            private String category;
            @SerializedName("Class")
            private Integer moduleClass;
            @SerializedName("Rating")
            private String rating;
            @SerializedName("Ship")
            private String ship;
            @SerializedName("Hot")
            private Boolean hot;
            @SerializedName("EngineerModifications")
            private String engineerModifications;
            @SerializedName("Level")
            private Integer level;
            @SerializedName("Quality")
            private Double quality;

            public Long getId() {
                return id;
            }

            public String getName() {
                return name;
            }

            public String getNameLocalised() {
                return nameLocalised;
            }

            public Long getBuyPrice() {
                return buyPrice;
            }

            public Integer getStock() {
                return stock;
            }

            public String getCategory() {
                return category;
            }

            public Integer getModuleClass() {
                return moduleClass;
            }

            public String getRating() {
                return rating;
            }

            public String getShip() {
                return ship;
            }

            public Boolean getHot() {
                return hot;
            }

            public String getEngineerModifications() {
                return engineerModifications;
            }

            public Integer getLevel() {
                return level;
            }

            public Double getQuality() {
                return quality;
            }
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }

        public void setEvent(String event) {
            this.event = event;
        }

        public void setMarketID(long marketID) {
            this.marketID = marketID;
        }

        public void setStationName(String stationName) {
            this.stationName = stationName;
        }

        public void setStarSystem(String starSystem) {
            this.starSystem = starSystem;
        }

        public void setHorizons(boolean horizons) {
            this.horizons = horizons;
        }

        public void setItems(List<OutfittingItem> items) {
            this.items = items;
        }

        // Getters
        public String getTimestamp() {
            return timestamp;
        }

        public String getEvent() {
            return event;
        }

        public long getMarketID() {
            return marketID;
        }

        /**
         * The station as a human names it - see {@link StationName} for the colonisation-ship shape.
         */
        public String getStationName() {
            return StationName.display(stationName);
        }

        public String getStarSystem() {
            return starSystem;
        }

        public boolean isHorizons() {
            return horizons;
        }

        public List<OutfittingItem> getItems() {
            return items;
        }

        public String toJson() {
            return GsonFactory.getGson().toJson(this);
        }

        public JsonObject toJsonObject() {
            return GsonFactory.toJsonObject(this);
        }
    }

    public static class ShipyardEvent {
        public JsonObject toJsonObject() {
            return GsonFactory.toJsonObject(this);
        }

        public static class ShipPrice {
            @SerializedName("id")
            private int id;
            @SerializedName("ShipType")
            private String shipType;
            @SerializedName("ShipType_Localised")
            private String shipTypeLocalised;
            @SerializedName("ShipPrice")
            private long shipPrice;

            public void setId(int id) {
                this.id = id;
            }

            public void setShipType(String shipType) {
                this.shipType = shipType;
            }

            public void setShipTypeLocalised(String shipTypeLocalised) {
                this.shipTypeLocalised = shipTypeLocalised;
            }

            public void setShipPrice(long shipPrice) {
                this.shipPrice = shipPrice;
            }

            // Getters
            public int getId() {
                return id;
            }

            public String getShipType() {
                return shipType;
            }

            public String getShipTypeLocalised() {
                return shipTypeLocalised;
            }

            public long getShipPrice() {
                return shipPrice;
            }

            public String toJson() {
                return GsonFactory.getGson().toJson(this);
            }
        }

        @SerializedName("timestamp")
        private String timestamp;
        @SerializedName("event")
        private String event;
        @SerializedName("MarketID")
        private long marketID;
        @SerializedName("StationName")
        private String stationName;
        @SerializedName("StarSystem")
        private String starSystem;
        @SerializedName("Horizons")
        private boolean horizons;
        @SerializedName("AllowCobraMkIV")
        private boolean allowCobraMkIV;
        @SerializedName("PriceList")
        private List<ShipPrice> priceList;

        // Getters
        public String getTimestamp() {
            return timestamp;
        }

        public String getEvent() {
            return event;
        }

        public long getMarketID() {
            return marketID;
        }

        /** The station as a human names it - see {@link StationName} for the colonisation-ship shape. */
        public String getStationName() {
            return StationName.display(stationName);
        }

        public String getStarSystem() {
            return starSystem;
        }

        public boolean isHorizons() {
            return horizons;
        }

        public boolean isAllowCobraMkIV() {
            return allowCobraMkIV;
        }

        public List<ShipPrice> getPriceList() {
            return priceList;
        }

        public String toJson() {
            return GsonFactory.getGson().toJson(this);
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }

        public void setEvent(String event) {
            this.event = event;
        }

        public void setMarketID(long marketID) {
            this.marketID = marketID;
        }

        public void setStationName(String stationName) {
            this.stationName = stationName;
        }

        public void setStarSystem(String starSystem) {
            this.starSystem = starSystem;
        }

        public void setHorizons(boolean horizons) {
            this.horizons = horizons;
        }

        public void setAllowCobraMkIV(boolean allowCobraMkIV) {
            this.allowCobraMkIV = allowCobraMkIV;
        }

        public void setPriceList(List<ShipPrice> priceList) {
            this.priceList = priceList;
        }
    }

    public static class ShipLockerEvent {
        public static class Component {
            @SerializedName("Name")
            private String name;
            @SerializedName("Name_Localised")
            private String nameLocalised;
            @SerializedName("OwnerID")
            private int ownerID;
            @SerializedName("Count")
            private int count;

            public void setName(String name) {
                this.name = name;
            }

            public void setNameLocalised(String nameLocalised) {
                this.nameLocalised = nameLocalised;
            }

            public void setOwnerID(int ownerID) {
                this.ownerID = ownerID;
            }

            public void setCount(int count) {
                this.count = count;
            }

            // Getters
            public String getName() {
                return name;
            }

            public String getNameLocalised() {
                return nameLocalised;
            }

            public int getOwnerID() {
                return ownerID;
            }

            public int getCount() {
                return count;
            }

            public String toJson() {
                return GsonFactory.getGson().toJson(this);
            }
        }

        public static class Consumable {
            @SerializedName("Name")
            private String name;
            @SerializedName("Name_Localised")
            private String nameLocalised;
            @SerializedName("OwnerID")
            private int ownerID;
            @SerializedName("Count")
            private int count;

            public void setName(String name) {
                this.name = name;
            }

            public void setNameLocalised(String nameLocalised) {
                this.nameLocalised = nameLocalised;
            }

            public void setOwnerID(int ownerID) {
                this.ownerID = ownerID;
            }

            public void setCount(int count) {
                this.count = count;
            }

            // Getters
            public String getName() {
                return name;
            }

            public String getNameLocalised() {
                return nameLocalised;
            }

            public int getOwnerID() {
                return ownerID;
            }

            public int getCount() {
                return count;
            }

            public String toJson() {
                return GsonFactory.getGson().toJson(this);
            }
        }

        @SerializedName("timestamp")
        private String timestamp;
        @SerializedName("event")
        private String event;
        @SerializedName("Items")
        private List<Object> items;
        @SerializedName("Components")
        private List<Component> components;
        @SerializedName("Consumables")
        private List<Consumable> consumables;
        @SerializedName("Data")
        private List<Object> data;


        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }

        public void setEvent(String event) {
            this.event = event;
        }

        public void setItems(List<Object> items) {
            this.items = items;
        }

        public void setComponents(List<Component> components) {
            this.components = components;
        }

        public void setConsumables(List<Consumable> consumables) {
            this.consumables = consumables;
        }

        public void setData(List<Object> data) {
            this.data = data;
        }

        // Getters
        public String getTimestamp() {
            return timestamp;
        }

        public String getEvent() {
            return event;
        }

        public List<Object> getItems() {
            return items;
        }

        public List<Component> getComponents() {
            return components;
        }

        public List<Consumable> getConsumables() {
            return consumables;
        }

        public List<Object> getData() {
            return data;
        }

        public String toJson() {
            return GsonFactory.getGson().toJson(this);
        }
    }

    public static class MarketEvent implements ToJsonConvertible{
        public JsonObject toJsonObject() {
            return GsonFactory.toJsonObject(this);
        }

        public static class MarketItem {
            @SerializedName("id")
            private long id;
            @SerializedName("Name")
            private String name;
            @SerializedName("Name_Localised")
            private String nameLocalised;
            @SerializedName("Category")
            private String category;
            @SerializedName("Category_Localised")
            private String categoryLocalised;
            @SerializedName("BuyPrice")
            private int buyPrice;
            @SerializedName("SellPrice")
            int sellPrice;
            @SerializedName("MeanPrice")
            private int meanPrice;
            @SerializedName("StockBracket")
            private int stockBracket;
            @SerializedName("DemandBracket")
            private int demandBracket;
            @SerializedName("Stock")
            private int stock;
            @SerializedName("Demand")
            private int demand;
            @SerializedName("Consumer")
            private boolean consumer;
            @SerializedName("Producer")
            private boolean producer;
            @SerializedName("Rare")
            private boolean rare;


            public void setId(long id) {
                this.id = id;
            }

            public void setName(String name) {
                this.name = name;
            }

            public void setNameLocalised(String nameLocalised) {
                this.nameLocalised = nameLocalised;
            }

            public void setCategory(String category) {
                this.category = category;
            }

            public void setCategoryLocalised(String categoryLocalised) {
                this.categoryLocalised = categoryLocalised;
            }

            public void setBuyPrice(int buyPrice) {
                this.buyPrice = buyPrice;
            }

            public void setSellPrice(int sellPrice) {
                this.sellPrice = sellPrice;
            }

            public void setMeanPrice(int meanPrice) {
                this.meanPrice = meanPrice;
            }

            public void setStockBracket(int stockBracket) {
                this.stockBracket = stockBracket;
            }

            public void setDemandBracket(int demandBracket) {
                this.demandBracket = demandBracket;
            }

            public void setStock(int stock) {
                this.stock = stock;
            }

            public void setDemand(int demand) {
                this.demand = demand;
            }

            public void setConsumer(boolean consumer) {
                this.consumer = consumer;
            }

            public void setProducer(boolean producer) {
                this.producer = producer;
            }

            public void setRare(boolean rare) {
                this.rare = rare;
            }

            // Getters
            public long getId() {
                return id;
            }

            public String getName() {
                return name;
            }

            public String getNameLocalised() {
                return nameLocalised;
            }

            public String getCategory() {
                return category;
            }

            public String getCategoryLocalised() {
                return categoryLocalised;
            }

            public int getBuyPrice() {
                return buyPrice;
            }

            public int getSellPrice() {
                return sellPrice;
            }

            public int getMeanPrice() {
                return meanPrice;
            }

            public int getStockBracket() {
                return stockBracket;
            }

            public int getDemandBracket() {
                return demandBracket;
            }

            public int getStock() {
                return stock;
            }

            public int getDemand() {
                return demand;
            }

            public boolean isConsumer() {
                return consumer;
            }

            public boolean isProducer() {
                return producer;
            }

            public boolean isRare() {
                return rare;
            }

            public String toJson() {
                return GsonFactory.getGson().toJson(this);
            }


        }

        @SerializedName("timestamp")
        private String timestamp;
        @SerializedName("event")
        private String event;
        @SerializedName("MarketID")
        private long marketID;
        @SerializedName("StationName")
        private String stationName;
        @SerializedName("StationType")
        private String stationType;
        @SerializedName("StarSystem")
        private String starSystem;
        @SerializedName("Items")
        private List<MarketItem> items;


        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }

        public void setEvent(String event) {
            this.event = event;
        }

        public void setMarketID(long marketID) {
            this.marketID = marketID;
        }

        public void setStationName(String stationName) {
            this.stationName = stationName;
        }

        public void setStationType(String stationType) {
            this.stationType = stationType;
        }

        public void setStarSystem(String starSystem) {
            this.starSystem = starSystem;
        }

        public void setItems(List<MarketItem> items) {
            this.items = items;
        }

        // Getters
        public String getTimestamp() {
            return timestamp;
        }

        public String getEvent() {
            return event;
        }

        public long getMarketID() {
            return marketID;
        }

        /** The station as a human names it - see {@link StationName} for the colonisation-ship shape. */
        public String getStationName() {
            return StationName.display(stationName);
        }

        public String getStationType() {
            return stationType;
        }

        public String getStarSystem() {
            return starSystem;
        }

        public List<MarketItem> getItems() {
            return items;
        }

        public String toJson() {
            return GsonFactory.getGson().toJson(this);
        }
    }
}