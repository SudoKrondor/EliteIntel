package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;

public class StatisticsEvent extends BaseEvent {
    @SerializedName("Bank_Account")
    public BankAccount bankAccount;

    @SerializedName("Combat")
    public Combat combat;

    @SerializedName("Crime")
    public Crime crime;

    @SerializedName("Smuggling")
    public Smuggling smuggling;

    @SerializedName("Trading")
    public Trading trading;

    @SerializedName("Mining")
    public Mining mining;

    @SerializedName("Exploration")
    public Exploration exploration;

    @SerializedName("Passengers")
    public Passengers passengers;

    @SerializedName("Search_And_Rescue")
    public SearchAndRescue searchAndRescue;

    @SerializedName("Squadron")
    public Squadron squadron;

    @SerializedName("Crafting")
    public Crafting crafting;

    @SerializedName("Crew")
    public Crew crew;

    @SerializedName("Multicrew")
    public Multicrew multicrew;

    @SerializedName("Material_Trader_Stats")
    public MaterialTraderStats materialTraderStats;

    @SerializedName("FLEETCARRIER")
    public FleetCarrier fleetCarrier;

    @SerializedName("Exobiology")
    public Exobiology exobiology;

    public StatisticsEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofHours(1), "Statistics");
        StatisticsEvent event = GsonFactory.getGson().fromJson(json, StatisticsEvent.class);
        this.bankAccount = event.bankAccount;
        this.combat = event.combat;
        this.crime = event.crime;
        this.smuggling = event.smuggling;
        this.trading = event.trading;
        this.mining = event.mining;
        this.exploration = event.exploration;
        this.passengers = event.passengers;
        this.searchAndRescue = event.searchAndRescue;
        this.squadron = event.squadron;
        this.crafting = event.crafting;
        this.crew = event.crew;
        this.multicrew = event.multicrew;
        this.materialTraderStats = event.materialTraderStats;
        this.fleetCarrier = event.fleetCarrier;
        this.exobiology = event.exobiology;
    }

    @Override
    public String getEventType() {
        return "Statistics";
    }

    /** Lifetime stats snapshot; memory context. */
    @Override
    public Importance importance() {
        return Importance.NORMAL;
    }

    @Override
    public String llmDescription() {
        return "A snapshot of lifetime player statistics; written at game load. Background detail.";
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public BankAccount getBankAccount() {
        return bankAccount;
    }

    public void setBankAccount(BankAccount bankAccount) {
        this.bankAccount = bankAccount;
    }

    public Combat getCombat() {
        return combat;
    }

    public void setCombat(Combat combat) {
        this.combat = combat;
    }

    public Crime getCrime() {
        return crime;
    }

    public void setCrime(Crime crime) {
        this.crime = crime;
    }

    public Smuggling getSmuggling() {
        return smuggling;
    }

    public void setSmuggling(Smuggling smuggling) {
        this.smuggling = smuggling;
    }

    public Trading getTrading() {
        return trading;
    }

    public void setTrading(Trading trading) {
        this.trading = trading;
    }

    public Mining getMining() {
        return mining;
    }

    public void setMining(Mining mining) {
        this.mining = mining;
    }

    public Exploration getExploration() {
        return exploration;
    }

    public void setExploration(Exploration exploration) {
        this.exploration = exploration;
    }

    public Passengers getPassengers() {
        return passengers;
    }

    public void setPassengers(Passengers passengers) {
        this.passengers = passengers;
    }

    public SearchAndRescue getSearchAndRescue() {
        return searchAndRescue;
    }

    public void setSearchAndRescue(SearchAndRescue searchAndRescue) {
        this.searchAndRescue = searchAndRescue;
    }

    public Squadron getSquadron() {
        return squadron;
    }

    public void setSquadron(Squadron squadron) {
        this.squadron = squadron;
    }

    public Crafting getCrafting() {
        return crafting;
    }

    public void setCrafting(Crafting crafting) {
        this.crafting = crafting;
    }

    public Crew getCrew() {
        return crew;
    }

    public void setCrew(Crew crew) {
        this.crew = crew;
    }

    public Multicrew getMulticrew() {
        return multicrew;
    }

    public void setMulticrew(Multicrew multicrew) {
        this.multicrew = multicrew;
    }

    public MaterialTraderStats getMaterialTraderStats() {
        return materialTraderStats;
    }

    public void setMaterialTraderStats(MaterialTraderStats materialTraderStats) {
        this.materialTraderStats = materialTraderStats;
    }

    public FleetCarrier getFleetCarrier() {
        return fleetCarrier;
    }

    public void setFleetCarrier(FleetCarrier fleetCarrier) {
        this.fleetCarrier = fleetCarrier;
    }

    public Exobiology getExobiology() {
        return exobiology;
    }

    public void setExobiology(Exobiology exobiology) {
        this.exobiology = exobiology;
    }

    @Override
    public String toString() {
        return String.format("%s: Statistics event with wealth: %d", timestamp, bankAccount != null ? bankAccount.currentWealth : 0);
    }

    public static class BankAccount {
        @SerializedName("Current_Wealth")
        public long currentWealth;
        @SerializedName("Spent_On_Ships")
        public long spentOnShips;
        @SerializedName("Spent_On_Outfitting")
        public long spentOnOutfitting;
        @SerializedName("Spent_On_Repairs")
        public long spentOnRepairs;
        @SerializedName("Spent_On_Fuel")
        public long spentOnFuel;
        @SerializedName("Spent_On_Ammo_Consumables")
        public long spentOnAmmoConsumables;
        @SerializedName("Insurance_Claims")
        public int insuranceClaims;
        @SerializedName("Spent_On_Insurance")
        public long spentOnInsurance;
        @SerializedName("Owned_Ship_Count")
        public int ownedShipCount;
        @SerializedName("Spent_On_Suits")
        public long spentOnSuits;
        @SerializedName("Spent_On_Weapons")
        public long spentOnWeapons;
        @SerializedName("Spent_On_Suit_Consumables")
        public long spentOnSuitConsumables;
        @SerializedName("Suits_Owned")
        public int suitsOwned;
        @SerializedName("Weapons_Owned")
        public int weaponsOwned;
        @SerializedName("Spent_On_Premium_Stock")
        public long spentOnPremiumStock;
        @SerializedName("Premium_Stock_Bought")
        public int premiumStockBought;

        public long getCurrentWealth() {
            return currentWealth;
        }

        public void setCurrentWealth(long currentWealth) {
            this.currentWealth = currentWealth;
        }

        public long getSpentOnShips() {
            return spentOnShips;
        }

        public void setSpentOnShips(long spentOnShips) {
            this.spentOnShips = spentOnShips;
        }

        public long getSpentOnOutfitting() {
            return spentOnOutfitting;
        }

        public void setSpentOnOutfitting(long spentOnOutfitting) {
            this.spentOnOutfitting = spentOnOutfitting;
        }

        public long getSpentOnRepairs() {
            return spentOnRepairs;
        }

        public void setSpentOnRepairs(long spentOnRepairs) {
            this.spentOnRepairs = spentOnRepairs;
        }

        public long getSpentOnFuel() {
            return spentOnFuel;
        }

        public void setSpentOnFuel(long spentOnFuel) {
            this.spentOnFuel = spentOnFuel;
        }

        public long getSpentOnAmmoConsumables() {
            return spentOnAmmoConsumables;
        }

        public void setSpentOnAmmoConsumables(long spentOnAmmoConsumables) {
            this.spentOnAmmoConsumables = spentOnAmmoConsumables;
        }

        public int getInsuranceClaims() {
            return insuranceClaims;
        }

        public void setInsuranceClaims(int insuranceClaims) {
            this.insuranceClaims = insuranceClaims;
        }

        public long getSpentOnInsurance() {
            return spentOnInsurance;
        }

        public void setSpentOnInsurance(long spentOnInsurance) {
            this.spentOnInsurance = spentOnInsurance;
        }

        public int getOwnedShipCount() {
            return ownedShipCount;
        }

        public void setOwnedShipCount(int ownedShipCount) {
            this.ownedShipCount = ownedShipCount;
        }

        public long getSpentOnSuits() {
            return spentOnSuits;
        }

        public void setSpentOnSuits(long spentOnSuits) {
            this.spentOnSuits = spentOnSuits;
        }

        public long getSpentOnWeapons() {
            return spentOnWeapons;
        }

        public void setSpentOnWeapons(long spentOnWeapons) {
            this.spentOnWeapons = spentOnWeapons;
        }

        public long getSpentOnSuitConsumables() {
            return spentOnSuitConsumables;
        }

        public void setSpentOnSuitConsumables(long spentOnSuitConsumables) {
            this.spentOnSuitConsumables = spentOnSuitConsumables;
        }

        public int getSuitsOwned() {
            return suitsOwned;
        }

        public void setSuitsOwned(int suitsOwned) {
            this.suitsOwned = suitsOwned;
        }

        public int getWeaponsOwned() {
            return weaponsOwned;
        }

        public void setWeaponsOwned(int weaponsOwned) {
            this.weaponsOwned = weaponsOwned;
        }

        public long getSpentOnPremiumStock() {
            return spentOnPremiumStock;
        }

        public void setSpentOnPremiumStock(long spentOnPremiumStock) {
            this.spentOnPremiumStock = spentOnPremiumStock;
        }

        public int getPremiumStockBought() {
            return premiumStockBought;
        }

        public void setPremiumStockBought(int premiumStockBought) {
            this.premiumStockBought = premiumStockBought;
        }
    }

    public static class Combat {
        @SerializedName("Bounties_Claimed")
        public int bountiesClaimed;
        @SerializedName("Bounty_Hunting_Profit")
        public long bountyHuntingProfit;
        @SerializedName("Combat_Bonds")
        public int combatBonds;
        @SerializedName("Combat_Bond_Profits")
        public long combatBondProfits;
        @SerializedName("Assassinations")
        public int assassinations;
        @SerializedName("Assassination_Profits")
        public long assassinationProfits;
        @SerializedName("Highest_Single_Reward")
        public long highestSingleReward;
        @SerializedName("Skimmers_Killed")
        public int skimmersKilled;
        @SerializedName("OnFoot_Combat_Bonds")
        public int onFootCombatBonds;
        @SerializedName("OnFoot_Combat_Bonds_Profits")
        public long onFootCombatBondsProfits;
        @SerializedName("OnFoot_Vehicles_Destroyed")
        public int onFootVehiclesDestroyed;
        @SerializedName("OnFoot_Ships_Destroyed")
        public int onFootShipsDestroyed;
        @SerializedName("Dropships_Taken")
        public int dropshipsTaken;
        @SerializedName("Dropships_Booked")
        public int dropshipsBooked;
        @SerializedName("Dropships_Cancelled")
        public int dropshipsCancelled;
        @SerializedName("ConflictZone_High")
        public int conflictZoneHigh;
        @SerializedName("ConflictZone_Medium")
        public int conflictZoneMedium;
        @SerializedName("ConflictZone_Low")
        public int conflictZoneLow;
        @SerializedName("ConflictZone_Total")
        public int conflictZoneTotal;
        @SerializedName("ConflictZone_High_Wins")
        public int conflictZoneHighWins;
        @SerializedName("ConflictZone_Medium_Wins")
        public int conflictZoneMediumWins;
        @SerializedName("ConflictZone_Low_Wins")
        public int conflictZoneLowWins;
        @SerializedName("ConflictZone_Total_Wins")
        public int conflictZoneTotalWins;
        @SerializedName("Settlement_Defended")
        public int settlementDefended;
        @SerializedName("Settlement_Conquered")
        public int settlementConquered;
        @SerializedName("OnFoot_Skimmers_Killed")
        public int onFootSkimmersKilled;
        @SerializedName("OnFoot_Scavs_Killed")
        public int onFootScavsKilled;

        public int getBountiesClaimed() {
            return bountiesClaimed;
        }

        public long getBountyHuntingProfit() {
            return bountyHuntingProfit;
        }

        public int getCombatBonds() {
            return combatBonds;
        }

        public long getCombatBondProfits() {
            return combatBondProfits;
        }

        public int getAssassinations() {
            return assassinations;
        }

        public long getAssassinationProfits() {
            return assassinationProfits;
        }

        public long getHighestSingleReward() {
            return highestSingleReward;
        }

        public int getSkimmersKilled() {
            return skimmersKilled;
        }

        public int getOnFootCombatBonds() {
            return onFootCombatBonds;
        }

        public long getOnFootCombatBondsProfits() {
            return onFootCombatBondsProfits;
        }

        public int getOnFootVehiclesDestroyed() {
            return onFootVehiclesDestroyed;
        }

        public int getOnFootShipsDestroyed() {
            return onFootShipsDestroyed;
        }

        public int getDropshipsTaken() {
            return dropshipsTaken;
        }

        public int getDropshipsBooked() {
            return dropshipsBooked;
        }

        public int getDropshipsCancelled() {
            return dropshipsCancelled;
        }

        public int getConflictZoneHigh() {
            return conflictZoneHigh;
        }

        public int getConflictZoneMedium() {
            return conflictZoneMedium;
        }

        public int getConflictZoneLow() {
            return conflictZoneLow;
        }

        public int getConflictZoneTotal() {
            return conflictZoneTotal;
        }

        public int getConflictZoneHighWins() {
            return conflictZoneHighWins;
        }

        public int getConflictZoneMediumWins() {
            return conflictZoneMediumWins;
        }

        public int getConflictZoneLowWins() {
            return conflictZoneLowWins;
        }

        public int getConflictZoneTotalWins() {
            return conflictZoneTotalWins;
        }

        public int getSettlementDefended() {
            return settlementDefended;
        }

        public int getSettlementConquered() {
            return settlementConquered;
        }

        public int getOnFootSkimmersKilled() {
            return onFootSkimmersKilled;
        }

        public int getOnFootScavsKilled() {
            return onFootScavsKilled;
        }
    }

    public static class Crime {
        @SerializedName("Notoriety")
        public int notoriety;
        @SerializedName("Fines")
        public int fines;
        @SerializedName("Total_Fines")
        public long totalFines;
        @SerializedName("Bounties_Received")
        public int bountiesReceived;
        @SerializedName("Total_Bounties")
        public long totalBounties;
        @SerializedName("Highest_Bounty")
        public long highestBounty;
        @SerializedName("Malware_Uploaded")
        public int malwareUploaded;
        @SerializedName("Settlements_State_Shutdown")
        public int settlementsStateShutdown;
        @SerializedName("Production_Sabotage")
        public int productionSabotage;
        @SerializedName("Production_Theft")
        public int productionTheft;
        @SerializedName("Total_Murders")
        public int totalMurders;
        @SerializedName("Citizens_Murdered")
        public int citizensMurdered;
        @SerializedName("Omnipol_Murdered")
        public int omnipolMurdered;
        @SerializedName("Guards_Murdered")
        public int guardsMurdered;
        @SerializedName("Data_Stolen")
        public int dataStolen;
        @SerializedName("Goods_Stolen")
        public int goodsStolen;
        @SerializedName("Sample_Stolen")
        public int sampleStolen;
        @SerializedName("Total_Stolen")
        public int totalStolen;
        @SerializedName("Turrets_Destroyed")
        public int turretsDestroyed;
        @SerializedName("Turrets_Overloaded")
        public int turretsOverloaded;
        @SerializedName("Turrets_Total")
        public int turretsTotal;
        @SerializedName("Value_Stolen_StateChange")
        public long valueStolenStateChange;
        @SerializedName("Profiles_Cloned")
        public int profilesCloned;

        public int getNotoriety() {
            return notoriety;
        }

        public int getFines() {
            return fines;
        }

        public long getTotalFines() {
            return totalFines;
        }

        public int getBountiesReceived() {
            return bountiesReceived;
        }

        public long getTotalBounties() {
            return totalBounties;
        }

        public long getHighestBounty() {
            return highestBounty;
        }

        public int getMalwareUploaded() {
            return malwareUploaded;
        }

        public int getSettlementsStateShutdown() {
            return settlementsStateShutdown;
        }

        public int getProductionSabotage() {
            return productionSabotage;
        }

        public int getProductionTheft() {
            return productionTheft;
        }

        public int getTotalMurders() {
            return totalMurders;
        }

        public int getCitizensMurdered() {
            return citizensMurdered;
        }

        public int getOmnipolMurdered() {
            return omnipolMurdered;
        }

        public int getGuardsMurdered() {
            return guardsMurdered;
        }

        public int getDataStolen() {
            return dataStolen;
        }

        public int getGoodsStolen() {
            return goodsStolen;
        }

        public int getSampleStolen() {
            return sampleStolen;
        }

        public int getTotalStolen() {
            return totalStolen;
        }

        public int getTurretsDestroyed() {
            return turretsDestroyed;
        }

        public int getTurretsOverloaded() {
            return turretsOverloaded;
        }

        public int getTurretsTotal() {
            return turretsTotal;
        }

        public long getValueStolenStateChange() {
            return valueStolenStateChange;
        }

        public int getProfilesCloned() {
            return profilesCloned;
        }
    }

    public static class Smuggling {
        @SerializedName("Black_Markets_Traded_With")
        public int blackMarketsTradedWith;
        @SerializedName("Black_Markets_Profits")
        public long blackMarketsProfits;
        @SerializedName("Resources_Smuggled")
        public int resourcesSmuggled;
        @SerializedName("Average_Profit")
        public double averageProfit;
        @SerializedName("Highest_Single_Transaction")
        public long highestSingleTransaction;

        public int getBlackMarketsTradedWith() {
            return blackMarketsTradedWith;
        }

        public long getBlackMarketsProfits() {
            return blackMarketsProfits;
        }

        public int getResourcesSmuggled() {
            return resourcesSmuggled;
        }

        public double getAverageProfit() {
            return averageProfit;
        }

        public long getHighestSingleTransaction() {
            return highestSingleTransaction;
        }
    }

    public static class Trading {
        @SerializedName("Markets_Traded_With")
        public int marketsTradedWith;
        @SerializedName("Market_Profits")
        public long marketProfits;
        @SerializedName("Resources_Traded")
        public int resourcesTraded;
        @SerializedName("Average_Profit")
        public double averageProfit;
        @SerializedName("Highest_Single_Transaction")
        public long highestSingleTransaction;
        @SerializedName("Data_Sold")
        public int dataSold;
        @SerializedName("Goods_Sold")
        public int goodsSold;
        @SerializedName("Assets_Sold")
        public int assetsSold;

        public int getMarketsTradedWith() {
            return marketsTradedWith;
        }

        public long getMarketProfits() {
            return marketProfits;
        }

        public int getResourcesTraded() {
            return resourcesTraded;
        }

        public double getAverageProfit() {
            return averageProfit;
        }

        public long getHighestSingleTransaction() {
            return highestSingleTransaction;
        }

        public int getDataSold() {
            return dataSold;
        }

        public int getGoodsSold() {
            return goodsSold;
        }

        public int getAssetsSold() {
            return assetsSold;
        }
    }

    public static class Mining {
        @SerializedName("Mining_Profits")
        public long miningProfits;
        @SerializedName("Quantity_Mined")
        public int quantityMined;
        @SerializedName("Materials_Collected")
        public int materialsCollected;

        public long getMiningProfits() {
            return miningProfits;
        }

        public int getQuantityMined() {
            return quantityMined;
        }

        public int getMaterialsCollected() {
            return materialsCollected;
        }
    }

    public static class Exploration {
        @SerializedName("Systems_Visited")
        public int systemsVisited;
        @SerializedName("Exploration_Profits")
        public long explorationProfits;
        @SerializedName("Planets_Scanned_To_Level_2")
        public int planetsScannedToLevel2;
        @SerializedName("Planets_Scanned_To_Level_3")
        public int planetsScannedToLevel3;
        @SerializedName("Efficient_Scans")
        public int efficientScans;
        @SerializedName("Highest_Payout")
        public long highestPayout;
        @SerializedName("Total_Hyperspace_Distance")
        public long totalHyperspaceDistance;
        @SerializedName("Total_Hyperspace_Jumps")
        public int totalHyperspaceJumps;
        @SerializedName("Greatest_Distance_From_Start")
        public double greatestDistanceFromStart;
        @SerializedName("Time_Played")
        public long timePlayed;
        @SerializedName("OnFoot_Distance_Travelled")
        public long onFootDistanceTravelled;
        @SerializedName("Shuttle_Journeys")
        public int shuttleJourneys;
        @SerializedName("Shuttle_Distance_Travelled")
        public long shuttleDistanceTravelled;
        @SerializedName("Spent_On_Shuttles")
        public long spentOnShuttles;
        @SerializedName("First_Footfalls")
        public int firstFootfalls;
        @SerializedName("Planet_Footfalls")
        public int planetFootfalls;
        @SerializedName("Settlements_Visited")
        public int settlementsVisited;

        public int getSystemsVisited() {
            return systemsVisited;
        }

        public long getExplorationProfits() {
            return explorationProfits;
        }

        public int getPlanetsScannedToLevel2() {
            return planetsScannedToLevel2;
        }

        public int getPlanetsScannedToLevel3() {
            return planetsScannedToLevel3;
        }

        public int getEfficientScans() {
            return efficientScans;
        }

        public long getHighestPayout() {
            return highestPayout;
        }

        public long getTotalHyperspaceDistance() {
            return totalHyperspaceDistance;
        }

        public int getTotalHyperspaceJumps() {
            return totalHyperspaceJumps;
        }

        public double getGreatestDistanceFromStart() {
            return greatestDistanceFromStart;
        }

        public long getTimePlayed() {
            return timePlayed;
        }

        public long getOnFootDistanceTravelled() {
            return onFootDistanceTravelled;
        }

        public int getShuttleJourneys() {
            return shuttleJourneys;
        }

        public long getShuttleDistanceTravelled() {
            return shuttleDistanceTravelled;
        }

        public long getSpentOnShuttles() {
            return spentOnShuttles;
        }

        public int getFirstFootfalls() {
            return firstFootfalls;
        }

        public int getPlanetFootfalls() {
            return planetFootfalls;
        }

        public int getSettlementsVisited() {
            return settlementsVisited;
        }
    }

    public static class Passengers {
        @SerializedName("Passengers_Missions_Accepted")
        public int passengersMissionsAccepted;
        @SerializedName("Passengers_Missions_Bulk")
        public int passengersMissionsBulk;
        @SerializedName("Passengers_Missions_VIP")
        public int passengersMissionsVip;
        @SerializedName("Passengers_Missions_Delivered")
        public int passengersMissionsDelivered;
        @SerializedName("Passengers_Missions_Ejected")
        public int passengersMissionsEjected;

        public int getPassengersMissionsAccepted() {
            return passengersMissionsAccepted;
        }

        public int getPassengersMissionsBulk() {
            return passengersMissionsBulk;
        }

        public int getPassengersMissionsVip() {
            return passengersMissionsVip;
        }

        public int getPassengersMissionsDelivered() {
            return passengersMissionsDelivered;
        }

        public int getPassengersMissionsEjected() {
            return passengersMissionsEjected;
        }
    }

    public static class SearchAndRescue {
        @SerializedName("SearchRescue_Traded")
        public int searchRescueTraded;
        @SerializedName("SearchRescue_Profit")
        public long searchRescueProfit;
        @SerializedName("SearchRescue_Count")
        public int searchRescueCount;
        @SerializedName("Salvage_Legal_POI")
        public int salvageLegalPoi;
        @SerializedName("Salvage_Legal_Settlements")
        public int salvageLegalSettlements;
        @SerializedName("Salvage_Illegal_POI")
        public int salvageIllegalPoi;
        @SerializedName("Salvage_Illegal_Settlements")
        public int salvageIllegalSettlements;
        @SerializedName("Maglocks_Opened")
        public int maglocksOpened;
        @SerializedName("Panels_Opened")
        public int panelsOpened;
        @SerializedName("Settlements_State_FireOut")
        public int settlementsStateFireOut;
        @SerializedName("Settlements_State_Reboot")
        public int settlementsStateReboot;

        public int getSearchRescueTraded() {
            return searchRescueTraded;
        }

        public long getSearchRescueProfit() {
            return searchRescueProfit;
        }

        public int getSearchRescueCount() {
            return searchRescueCount;
        }

        public int getSalvageLegalPoi() {
            return salvageLegalPoi;
        }

        public int getSalvageLegalSettlements() {
            return salvageLegalSettlements;
        }

        public int getSalvageIllegalPoi() {
            return salvageIllegalPoi;
        }

        public int getSalvageIllegalSettlements() {
            return salvageIllegalSettlements;
        }

        public int getMaglocksOpened() {
            return maglocksOpened;
        }

        public int getPanelsOpened() {
            return panelsOpened;
        }

        public int getSettlementsStateFireOut() {
            return settlementsStateFireOut;
        }

        public int getSettlementsStateReboot() {
            return settlementsStateReboot;
        }
    }

    public static class Squadron {
        @SerializedName("Squadron_Bank_Credits_Deposited")
        public long squadronBankCreditsDeposited;
        @SerializedName("Squadron_Bank_Credits_Withdrawn")
        public long squadronBankCreditsWithdrawn;
        @SerializedName("Squadron_Bank_Commodities_Deposited_Num")
        public int squadronBankCommoditiesDepositedNum;
        @SerializedName("Squadron_Bank_Commodities_Deposited_Value")
        public long squadronBankCommoditiesDepositedValue;
        @SerializedName("Squadron_Bank_Commodities_Withdrawn_Num")
        public int squadronBankCommoditiesWithdrawnNum;
        @SerializedName("Squadron_Bank_Commodities_Withdrawn_Value")
        public long squadronBankCommoditiesWithdrawnValue;
        @SerializedName("Squadron_Bank_PersonalAssets_Deposited_Num")
        public int squadronBankPersonalAssetsDepositedNum;
        @SerializedName("Squadron_Bank_PersonalAssets_Deposited_Value")
        public long squadronBankPersonalAssetsDepositedValue;
        @SerializedName("Squadron_Bank_PersonalAssets_Withdrawn_Num")
        public int squadronBankPersonalAssetsWithdrawnNum;
        @SerializedName("Squadron_Bank_PersonalAssets_Withdrawn_Value")
        public long squadronBankPersonalAssetsWithdrawnValue;
        @SerializedName("Squadron_Bank_Ships_Deposited_Num")
        public int squadronBankShipsDepositedNum;
        @SerializedName("Squadron_Bank_Ships_Deposited_Value")
        public long squadronBankShipsDepositedValue;
        @SerializedName("Squadron_Leaderboard_aegis_highestcontribution")
        public int squadronLeaderboardAegisHighestContribution;
        @SerializedName("Squadron_Leaderboard_bgs_highestcontribution")
        public int squadronLeaderboardBgsHighestContribution;
        @SerializedName("Squadron_Leaderboard_bounty_highestcontribution")
        public int squadronLeaderboardBountyHighestContribution;
        @SerializedName("Squadron_Leaderboard_colonisation_contribution_highestcontribution")
        public int squadronLeaderboardColonisationContributionHighestContribution;
        @SerializedName("Squadron_Leaderboard_combat_highestcontribution")
        public int squadronLeaderboardCombatHighestContribution;
        @SerializedName("Squadron_Leaderboard_cqc_highestcontribution")
        public int squadronLeaderboardCqcHighestContribution;
        @SerializedName("Squadron_Leaderboard_exploration_highestcontribution")
        public int squadronLeaderboardExplorationHighestContribution;
        @SerializedName("Squadron_Leaderboard_mining_highestcontribution")
        public int squadronLeaderboardMiningHighestContribution;
        @SerializedName("Squadron_Leaderboard_powerplay_highestcontribution")
        public int squadronLeaderboardPowerplayHighestContribution;
        @SerializedName("Squadron_Leaderboard_trade_highestcontribution")
        public int squadronLeaderboardTradeHighestContribution;
        @SerializedName("Squadron_Leaderboard_trade_illicit_highestcontribution")
        public int squadronLeaderboardTradeIllicitHighestContribution;
        @SerializedName("Squadron_Leaderboard_podiums")
        public int squadronLeaderboardPodiums;

        public long getSquadronBankCreditsDeposited() {
            return squadronBankCreditsDeposited;
        }

        public long getSquadronBankCreditsWithdrawn() {
            return squadronBankCreditsWithdrawn;
        }

        public int getSquadronBankCommoditiesDepositedNum() {
            return squadronBankCommoditiesDepositedNum;
        }

        public long getSquadronBankCommoditiesDepositedValue() {
            return squadronBankCommoditiesDepositedValue;
        }

        public int getSquadronBankCommoditiesWithdrawnNum() {
            return squadronBankCommoditiesWithdrawnNum;
        }

        public long getSquadronBankCommoditiesWithdrawnValue() {
            return squadronBankCommoditiesWithdrawnValue;
        }

        public int getSquadronBankPersonalAssetsDepositedNum() {
            return squadronBankPersonalAssetsDepositedNum;
        }

        public long getSquadronBankPersonalAssetsDepositedValue() {
            return squadronBankPersonalAssetsDepositedValue;
        }

        public int getSquadronBankPersonalAssetsWithdrawnNum() {
            return squadronBankPersonalAssetsWithdrawnNum;
        }

        public long getSquadronBankPersonalAssetsWithdrawnValue() {
            return squadronBankPersonalAssetsWithdrawnValue;
        }

        public int getSquadronBankShipsDepositedNum() {
            return squadronBankShipsDepositedNum;
        }

        public long getSquadronBankShipsDepositedValue() {
            return squadronBankShipsDepositedValue;
        }

        public int getSquadronLeaderboardAegisHighestContribution() {
            return squadronLeaderboardAegisHighestContribution;
        }

        public int getSquadronLeaderboardBgsHighestContribution() {
            return squadronLeaderboardBgsHighestContribution;
        }

        public int getSquadronLeaderboardBountyHighestContribution() {
            return squadronLeaderboardBountyHighestContribution;
        }

        public int getSquadronLeaderboardColonisationContributionHighestContribution() {
            return squadronLeaderboardColonisationContributionHighestContribution;
        }

        public int getSquadronLeaderboardCombatHighestContribution() {
            return squadronLeaderboardCombatHighestContribution;
        }

        public int getSquadronLeaderboardCqcHighestContribution() {
            return squadronLeaderboardCqcHighestContribution;
        }

        public int getSquadronLeaderboardExplorationHighestContribution() {
            return squadronLeaderboardExplorationHighestContribution;
        }

        public int getSquadronLeaderboardMiningHighestContribution() {
            return squadronLeaderboardMiningHighestContribution;
        }

        public int getSquadronLeaderboardPowerplayHighestContribution() {
            return squadronLeaderboardPowerplayHighestContribution;
        }

        public int getSquadronLeaderboardTradeHighestContribution() {
            return squadronLeaderboardTradeHighestContribution;
        }

        public int getSquadronLeaderboardTradeIllicitHighestContribution() {
            return squadronLeaderboardTradeIllicitHighestContribution;
        }

        public int getSquadronLeaderboardPodiums() {
            return squadronLeaderboardPodiums;
        }
    }

    public static class Crafting {
        @SerializedName("Count_Of_Used_Engineers")
        public int countOfUsedEngineers;
        @SerializedName("Recipes_Generated")
        public int recipesGenerated;
        @SerializedName("Recipes_Generated_Rank_1")
        public int recipesGeneratedRank1;
        @SerializedName("Recipes_Generated_Rank_2")
        public int recipesGeneratedRank2;
        @SerializedName("Recipes_Generated_Rank_3")
        public int recipesGeneratedRank3;
        @SerializedName("Recipes_Generated_Rank_4")
        public int recipesGeneratedRank4;
        @SerializedName("Recipes_Generated_Rank_5")
        public int recipesGeneratedRank5;
        @SerializedName("Suit_Mods_Applied")
        public int suitModsApplied;
        @SerializedName("Weapon_Mods_Applied")
        public int weaponModsApplied;
        @SerializedName("Suits_Upgraded")
        public int suitsUpgraded;
        @SerializedName("Weapons_Upgraded")
        public int weaponsUpgraded;
        @SerializedName("Suits_Upgraded_Full")
        public int suitsUpgradedFull;
        @SerializedName("Weapons_Upgraded_Full")
        public int weaponsUpgradedFull;
        @SerializedName("Suit_Mods_Applied_Full")
        public int suitModsAppliedFull;
        @SerializedName("Weapon_Mods_Applied_Full")
        public int weaponModsAppliedFull;

        public int getCountOfUsedEngineers() {
            return countOfUsedEngineers;
        }

        public int getRecipesGenerated() {
            return recipesGenerated;
        }

        public int getRecipesGeneratedRank1() {
            return recipesGeneratedRank1;
        }

        public int getRecipesGeneratedRank2() {
            return recipesGeneratedRank2;
        }

        public int getRecipesGeneratedRank3() {
            return recipesGeneratedRank3;
        }

        public int getRecipesGeneratedRank4() {
            return recipesGeneratedRank4;
        }

        public int getRecipesGeneratedRank5() {
            return recipesGeneratedRank5;
        }

        public int getSuitModsApplied() {
            return suitModsApplied;
        }

        public int getWeaponModsApplied() {
            return weaponModsApplied;
        }

        public int getSuitsUpgraded() {
            return suitsUpgraded;
        }

        public int getWeaponsUpgraded() {
            return weaponsUpgraded;
        }

        public int getSuitsUpgradedFull() {
            return suitsUpgradedFull;
        }

        public int getWeaponsUpgradedFull() {
            return weaponsUpgradedFull;
        }

        public int getSuitModsAppliedFull() {
            return suitModsAppliedFull;
        }

        public int getWeaponModsAppliedFull() {
            return weaponModsAppliedFull;
        }
    }

    public static class Crew {
        @SerializedName("NpcCrew_TotalWages")
        public long npcCrewTotalWages;
        @SerializedName("NpcCrew_Hired")
        public int npcCrewHired;
        @SerializedName("NpcCrew_Fired")
        public int npcCrewFired;
        @SerializedName("NpcCrew_Died")
        public int npcCrewDied;

        public long getNpcCrewTotalWages() {
            return npcCrewTotalWages;
        }

        public int getNpcCrewHired() {
            return npcCrewHired;
        }

        public int getNpcCrewFired() {
            return npcCrewFired;
        }

        public int getNpcCrewDied() {
            return npcCrewDied;
        }
    }

    public static class Multicrew {
        @SerializedName("Multicrew_Time_Total")
        public long multicrewTimeTotal;
        @SerializedName("Multicrew_Gunner_Time_Total")
        public long multicrewGunnerTimeTotal;
        @SerializedName("Multicrew_Fighter_Time_Total")
        public long multicrewFighterTimeTotal;
        @SerializedName("Multicrew_Credits_Total")
        public long multicrewCreditsTotal;
        @SerializedName("Multicrew_Fines_Total")
        public long multicrewFinesTotal;

        public long getMulticrewTimeTotal() {
            return multicrewTimeTotal;
        }

        public long getMulticrewGunnerTimeTotal() {
            return multicrewGunnerTimeTotal;
        }

        public long getMulticrewFighterTimeTotal() {
            return multicrewFighterTimeTotal;
        }

        public long getMulticrewCreditsTotal() {
            return multicrewCreditsTotal;
        }

        public long getMulticrewFinesTotal() {
            return multicrewFinesTotal;
        }
    }

    public static class MaterialTraderStats {
        @SerializedName("Trades_Completed")
        public int tradesCompleted;
        @SerializedName("Materials_Traded")
        public int materialsTraded;
        @SerializedName("Encoded_Materials_Traded")
        public int encodedMaterialsTraded;
        @SerializedName("Raw_Materials_Traded")
        public int rawMaterialsTraded;
        @SerializedName("Grade_1_Materials_Traded")
        public int grade1MaterialsTraded;
        @SerializedName("Grade_2_Materials_Traded")
        public int grade2MaterialsTraded;
        @SerializedName("Grade_3_Materials_Traded")
        public int grade3MaterialsTraded;
        @SerializedName("Grade_4_Materials_Traded")
        public int grade4MaterialsTraded;
        @SerializedName("Grade_5_Materials_Traded")
        public int grade5MaterialsTraded;
        @SerializedName("Assets_Traded_In")
        public int assetsTradedIn;
        @SerializedName("Assets_Traded_Out")
        public int assetsTradedOut;

        public int getTradesCompleted() {
            return tradesCompleted;
        }

        public int getMaterialsTraded() {
            return materialsTraded;
        }

        public int getEncodedMaterialsTraded() {
            return encodedMaterialsTraded;
        }

        public int getRawMaterialsTraded() {
            return rawMaterialsTraded;
        }

        public int getGrade1MaterialsTraded() {
            return grade1MaterialsTraded;
        }

        public int getGrade2MaterialsTraded() {
            return grade2MaterialsTraded;
        }

        public int getGrade3MaterialsTraded() {
            return grade3MaterialsTraded;
        }

        public int getGrade4MaterialsTraded() {
            return grade4MaterialsTraded;
        }

        public int getGrade5MaterialsTraded() {
            return grade5MaterialsTraded;
        }

        public int getAssetsTradedIn() {
            return assetsTradedIn;
        }

        public int getAssetsTradedOut() {
            return assetsTradedOut;
        }
    }

    public static class FleetCarrier {
        @SerializedName("FLEETCARRIER_EXPORT_TOTAL")
        public int fleetCarrierExportTotal;
        @SerializedName("FLEETCARRIER_IMPORT_TOTAL")
        public int fleetCarrierImportTotal;
        @SerializedName("FLEETCARRIER_TRADEPROFIT_TOTAL")
        public long fleetCarrierTradeProfitTotal;
        @SerializedName("FLEETCARRIER_TRADESPEND_TOTAL")
        public long fleetCarrierTradeSpendTotal;
        @SerializedName("FLEETCARRIER_STOLENPROFIT_TOTAL")
        public long fleetCarrierStolenProfitTotal;
        @SerializedName("FLEETCARRIER_STOLENSPEND_TOTAL")
        public long fleetCarrierStolenSpendTotal;
        @SerializedName("FLEETCARRIER_DISTANCE_TRAVELLED")
        public double fleetCarrierDistanceTravelled;
        @SerializedName("FLEETCARRIER_TOTAL_JUMPS")
        public int fleetCarrierTotalJumps;
        @SerializedName("FLEETCARRIER_SHIPYARD_SOLD")
        public int fleetCarrierShipyardSold;
        @SerializedName("FLEETCARRIER_SHIPYARD_PROFIT")
        public long fleetCarrierShipyardProfit;
        @SerializedName("FLEETCARRIER_OUTFITTING_SOLD")
        public int fleetCarrierOutfittingSold;
        @SerializedName("FLEETCARRIER_OUTFITTING_PROFIT")
        public long fleetCarrierOutfittingProfit;
        @SerializedName("FLEETCARRIER_REARM_TOTAL")
        public int fleetCarrierRearmTotal;
        @SerializedName("FLEETCARRIER_REFUEL_TOTAL")
        public int fleetCarrierRefuelTotal;
        @SerializedName("FLEETCARRIER_REFUEL_PROFIT")
        public long fleetCarrierRefuelProfit;
        @SerializedName("FLEETCARRIER_REPAIRS_TOTAL")
        public int fleetCarrierRepairsTotal;
        @SerializedName("FLEETCARRIER_VOUCHERS_REDEEMED")
        public int fleetCarrierVouchersRedeemed;
        @SerializedName("FLEETCARRIER_VOUCHERS_PROFIT")
        public long fleetCarrierVouchersProfit;

        public int getFleetCarrierExportTotal() {
            return fleetCarrierExportTotal;
        }

        public int getFleetCarrierImportTotal() {
            return fleetCarrierImportTotal;
        }

        public long getFleetCarrierTradeProfitTotal() {
            return fleetCarrierTradeProfitTotal;
        }

        public long getFleetCarrierTradeSpendTotal() {
            return fleetCarrierTradeSpendTotal;
        }

        public long getFleetCarrierStolenProfitTotal() {
            return fleetCarrierStolenProfitTotal;
        }

        public long getFleetCarrierStolenSpendTotal() {
            return fleetCarrierStolenSpendTotal;
        }

        public double getFleetCarrierDistanceTravelled() {
            return fleetCarrierDistanceTravelled;
        }

        public int getFleetCarrierTotalJumps() {
            return fleetCarrierTotalJumps;
        }

        public int getFleetCarrierShipyardSold() {
            return fleetCarrierShipyardSold;
        }

        public long getFleetCarrierShipyardProfit() {
            return fleetCarrierShipyardProfit;
        }

        public int getFleetCarrierOutfittingSold() {
            return fleetCarrierOutfittingSold;
        }

        public long getFleetCarrierOutfittingProfit() {
            return fleetCarrierOutfittingProfit;
        }

        public int getFleetCarrierRearmTotal() {
            return fleetCarrierRearmTotal;
        }

        public int getFleetCarrierRefuelTotal() {
            return fleetCarrierRefuelTotal;
        }

        public long getFleetCarrierRefuelProfit() {
            return fleetCarrierRefuelProfit;
        }

        public int getFleetCarrierRepairsTotal() {
            return fleetCarrierRepairsTotal;
        }

        public int getFleetCarrierVouchersRedeemed() {
            return fleetCarrierVouchersRedeemed;
        }

        public long getFleetCarrierVouchersProfit() {
            return fleetCarrierVouchersProfit;
        }
    }

    public static class Exobiology {
        @SerializedName("Organic_Genus_Encountered")
        public int organicGenusEncountered;
        @SerializedName("Organic_Species_Encountered")
        public int organicSpeciesEncountered;
        @SerializedName("Organic_Variant_Encountered")
        public int organicVariantEncountered;
        @SerializedName("Organic_Data_Profits")
        public long organicDataProfits;
        @SerializedName("Organic_Data")
        public int organicData;
        @SerializedName("First_Logged_Profits")
        public long firstLoggedProfits;
        @SerializedName("First_Logged")
        public int firstLogged;
        @SerializedName("Organic_Systems")
        public int organicSystems;
        @SerializedName("Organic_Planets")
        public int organicPlanets;
        @SerializedName("Organic_Genus")
        public int organicGenus;
        @SerializedName("Organic_Species")
        public int organicSpecies;

        public int getOrganicGenusEncountered() {
            return organicGenusEncountered;
        }

        public int getOrganicSpeciesEncountered() {
            return organicSpeciesEncountered;
        }

        public int getOrganicVariantEncountered() {
            return organicVariantEncountered;
        }

        public long getOrganicDataProfits() {
            return organicDataProfits;
        }

        public int getOrganicData() {
            return organicData;
        }

        public long getFirstLoggedProfits() {
            return firstLoggedProfits;
        }

        public int getFirstLogged() {
            return firstLogged;
        }

        public int getOrganicSystems() {
            return organicSystems;
        }

        public int getOrganicPlanets() {
            return organicPlanets;
        }

        public int getOrganicGenus() {
            return organicGenus;
        }

        public int getOrganicSpecies() {
            return organicSpecies;
        }
    }
}