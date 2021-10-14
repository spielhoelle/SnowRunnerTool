package net.schwarzbaer.java.games.snowrunner.tables;

import java.awt.Window;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Vector;

import net.schwarzbaer.gui.StyledDocumentInterface;
import net.schwarzbaer.java.games.snowrunner.Data;
import net.schwarzbaer.java.games.snowrunner.Data.Trailer;
import net.schwarzbaer.java.games.snowrunner.Data.Truck;
import net.schwarzbaer.java.games.snowrunner.Data.TruckAddon;
import net.schwarzbaer.java.games.snowrunner.SaveGameData.SaveGame;
import net.schwarzbaer.java.games.snowrunner.SnowRunner;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.Controllers;
import net.schwarzbaer.java.games.snowrunner.tables.VerySimpleTableModel.ExtendedVerySimpleTableModel2;

public class TrailersTableModel extends ExtendedVerySimpleTableModel2<Trailer> {
	
	private HashMap<String, TruckAddon> truckAddons;
	private HashMap<String, Trailer> trailers;
	private SaveGame saveGame;

	public TrailersTableModel(Window mainWindow, Controllers controllers, boolean connectToGlobalData, Data data, SaveGame saveGame) {
		this(mainWindow, controllers, connectToGlobalData);
		setExtraData(data);
		this.saveGame = saveGame;
	}
	public TrailersTableModel(Window mainWindow, Controllers controllers, boolean connectToGlobalData) {
		super(mainWindow, controllers, new ColumnID[] {
				new ColumnID("ID"       ,"ID"                   ,  String.class, 230,   null,      null, false, row->((Trailer)row).id),
				new ColumnID("Name"     ,"Name"                 ,  String.class, 200,   null,      null,  true, row->((Trailer)row).name_StringID), 
				new ColumnID("DLC"      ,"DLC"                  ,  String.class,  80,   null,      null, false, row->((Trailer)row).dlcName),
				new ColumnID("InstallSk","Install Socket"       ,  String.class, 130,   null,      null, false, row->((Trailer)row).installSocket),
				new ColumnID("CargoSlts","Cargo Slots"          , Integer.class,  70, CENTER,      null, false, row->((Trailer)row).cargoSlots),
				new ColumnID("Repairs"  ,"Repairs"              , Integer.class,  50,  RIGHT,    "%d R", false, row->((Trailer)row).repairsCapacity),
				new ColumnID("WheelRep" ,"Wheel Repairs"        , Integer.class,  85, CENTER,   "%d WR", false, row->((Trailer)row).wheelRepairsCapacity),
				new ColumnID("Fuel"     ,"Fuel"                 , Integer.class,  50,  RIGHT,    "%d L", false, row->((Trailer)row).fuelCapacity),
				new ColumnID("QuestItm" ,"Is Quest Item"        , Boolean.class,  80,   null,      null, false, row->((Trailer)row).isQuestItem),
				new ColumnID("Price"    ,"Price"                , Integer.class,  50,  RIGHT,   "%d Cr", false, row->((Trailer)row).price), 
				new ColumnID("UnlExpl"  ,"Unlock By Exploration", Boolean.class, 120,   null,      null, false, row->((Trailer)row).unlockByExploration), 
				new ColumnID("UnlRank"  ,"Unlock By Rank"       , Integer.class, 100, CENTER, "Rank %d", false, row->((Trailer)row).unlockByRank), 
				new ColumnID("AttachTyp","Attach Type"          ,  String.class,  70,   null,      null, false, row->((Trailer)row).attachType),
				new ColumnID("Desc"     ,"Description"          ,  String.class, 200,   null,      null,  true, row->((Trailer)row).description_StringID), 
				new ColumnID("ExclCargo","Excluded Cargo Types" ,  String.class, 150,   null,      null, false, row->SnowRunner.joinAddonIDs(((Trailer)row).excludedCargoTypes)),
				new ColumnID("RequAddon","Required Addons"      ,  String.class, 150,   null,      null, false, row->SnowRunner.joinRequiredAddonsToString_OneLine(((Trailer)row).requiredAddons)),
				new ColumnID("UsableBy" ,"Usable By"            ,  String.class, 150,   null,      null, (row,lang)->SnowRunner.joinTruckNames(((Trailer)row).usableBy, lang)),
		});
		
		truckAddons = null;
		trailers    = null;
		saveGame    = null;
		if (connectToGlobalData)
			connectToGlobalData(data->{
				truckAddons = data==null ? null : data.truckAddons;
				trailers    = data==null ? null : data.trailers;
				return trailers==null ? null : trailers.values();
			}, true);
		else
			controllers.dataReceivers.add(this,data -> {
				setExtraData(data);
				updateTextOutput();
			});
		
		controllers.saveGameListeners.add(this, saveGame->{
			this.saveGame = saveGame;
			updateTextOutput();
		});
		
		setInitialRowOrder(Comparator.<Trailer,String>comparing(row->row.id));
	}

	private void setExtraData(Data data) {
		truckAddons = data==null ? null : data.truckAddons;
		trailers    = data==null ? null : data.trailers;
	}

	@Override protected void setContentForRow(StyledDocumentInterface doc, Trailer row) {
		String description_StringID = row.description_StringID;
		String[][] requiredAddons = row.requiredAddons;
		String[] excludedCargoTypes = row.excludedCargoTypes;
		Vector<Truck> usableBy = row.usableBy;
		TruckAddonsTableModel.generateText(doc, description_StringID, requiredAddons, excludedCargoTypes, usableBy, language, truckAddons, trailers, saveGame);
	}
}