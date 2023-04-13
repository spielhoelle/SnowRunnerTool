package net.schwarzbaer.java.games.snowrunner.tables;

import java.awt.Color;
import java.awt.Point;
import java.awt.Window;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Vector;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JTable;

import net.schwarzbaer.gui.StyledDocumentInterface;
import net.schwarzbaer.gui.StyledDocumentInterface.Style;
import net.schwarzbaer.java.games.snowrunner.Data;
import net.schwarzbaer.java.games.snowrunner.Data.Language;
import net.schwarzbaer.java.games.snowrunner.Data.Trailer;
import net.schwarzbaer.java.games.snowrunner.Data.Truck;
import net.schwarzbaer.java.games.snowrunner.Data.TruckAddon;
import net.schwarzbaer.java.games.snowrunner.SaveGameData.SaveGame;
import net.schwarzbaer.java.games.snowrunner.SnowRunner;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.Controllers;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.SpecialTruckAddons;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.SpecialTruckAddons.SpecialTruckAddonList;
import net.schwarzbaer.java.games.snowrunner.tables.VerySimpleTableModel.ExtendedVerySimpleTableModelTPOS;

public class TruckAddonsTableModel extends ExtendedVerySimpleTableModelTPOS<TruckAddon> {
	
	private static final Color COLOR_SPECIALTRUCKADDON = new Color(0xFFF3AD);
	private static boolean enableSpecialTruckAddonsHighlighting = SnowRunner.settings.getBool(SnowRunner.AppSettings.ValueKey.TruckAddonsTableModel_enableSpecialTruckAddonsHighlighting, true);
	
	private HashMap<String, TruckAddon> truckAddons;
	private HashMap<String, Trailer> trailers;
	private TruckAddon clickedItem;
	private final SpecialTruckAddons specialTruckAddons;
	private SaveGame saveGame;
	
	public TruckAddonsTableModel(Window mainWindow, Controllers controllers, boolean connectToGlobalData, SpecialTruckAddons specialTruckAddons) {
		this(mainWindow, controllers, connectToGlobalData, specialTruckAddons, true);
	}
	public TruckAddonsTableModel(Window mainWindow, Controllers controllers, boolean connectToGlobalData, SpecialTruckAddons specialTruckAddons, boolean addExtraColumnsBeforeStandard, ColumnID... extraColumns) {
		super(mainWindow, controllers, SnowRunner.mergeArrays(extraColumns, addExtraColumnsBeforeStandard, new ColumnID[] {
				new ColumnID("ID"       ,"ID"                      ,  String.class, 230,   null,      null, false, row->((TruckAddon)row).id),
				new ColumnID("UpdateLvl", "Update Level"           ,  String.class,  80,   null,      null, false, row->((TruckAddon)row).updateLevel),
				new ColumnID("Category" ,"Category"                ,  String.class, 150,   null,      null, false, row->((TruckAddon)row).gameData.category),
				new ColumnID("Name"     ,"Name"                    ,  String.class, 200,   null,      null,  true, obj->{ TruckAddon row = (TruckAddon)obj; return row.gameData.name_StringID!=null ? row.gameData.name_StringID : row.gameData.cargoName_StringID; }), 
				new ColumnID("Owned"    ,"Owned"                   ,    Long.class,  60, CENTER,      null, false, get((model, lang, row)->getOwnedCount(model,row))),
				new ColumnID("InstallSk","Install Socket"          ,  String.class, 200,   null,      null, false, row->((TruckAddon)row).gameData.installSocket),
				new ColumnID("Original" ,"Original Addon"          ,  String.class, 130,   null,      null, false, row->((TruckAddon)row).gameData.originalAddon),
				new ColumnID("CargoSlts","Cargo Slots"             , Integer.class,  70, CENTER,      null, false, row->((TruckAddon)row).gameData.cargoSlots),
				new ColumnID("CargoCarr","Cargo Carrier"           , Boolean.class,  80,   null,      null, false, row->((TruckAddon)row).gameData.isCargoCarrier),
				new ColumnID("Repairs"  ,"Repairs"                 , Integer.class,  50,  RIGHT,    "%d R", false, row->((TruckAddon)row).repairsCapacity),
				new ColumnID("WheelRep" ,"Wheel Repairs"           , Integer.class,  85, CENTER,   "%d WR", false, row->((TruckAddon)row).wheelRepairsCapacity),
				new ColumnID("Fuel"     ,"Fuel"                    , Integer.class,  50,  RIGHT,    "%d L", false, row->((TruckAddon)row).fuelCapacity),
				new ColumnID("EnAWD"    ,"Enables AWD"             , Boolean.class,  80,   null,      null, false, row->((TruckAddon)row).enablesAllWheelDrive), 
				new ColumnID("EnDiffLck","Enables DiffLock"        , Boolean.class,  90,   null,      null, false, row->((TruckAddon)row).enablesDiffLock), 
				new ColumnID("Price"    ,"Price"                   , Integer.class,  50,  RIGHT,   "%d Cr", false, row->((TruckAddon)row).gameData.price), 
				new ColumnID("UnlExpl"  ,"Unlock By Exploration"   , Boolean.class, 120,   null,      null, false, row->((TruckAddon)row).gameData.unlockByExploration), 
				new ColumnID("UnlRank"  ,"Unlock By Rank"          , Integer.class, 100, CENTER, "Rank %d", false, row->((TruckAddon)row).gameData.unlockByRank), 
				new ColumnID("Desc"     ,"Description"             ,  String.class, 200,   null,      null,  true, obj->{ TruckAddon row = (TruckAddon)obj; return row.gameData.description_StringID!=null ? row.gameData.description_StringID : row.gameData.cargoDescription_StringID; }), 
				new ColumnID("ExclCargo","Excluded Cargo Types"    ,  String.class, 150,   null,      null, false, row->SnowRunner.joinAddonIDs(((TruckAddon)row).gameData.excludedCargoTypes,true)),
				new ColumnID("RequAddon","Required Addons"         ,  String.class, 200,   null,      null, false, row->SnowRunner.joinRequiredAddonsToString_OneLine(((TruckAddon)row).gameData.requiredAddons)),
		//		new ColumnID("GaragePts","Garage Points (obs?)"    , Integer.class, 120, CENTER,      null, false, row->((TruckAddon)row).gameData.garagePoints_obsolete),
		//		new ColumnID("Custmizbl","Is Customizable (obs?)"  , Boolean.class, 125,   null,      null, false, row->((TruckAddon)row).gameData.isCustomizable_obsolete),
				new ColumnID("LoadPts"  ,"Load Points"             , Integer.class,  70, CENTER,      null, false, row->((TruckAddon)row).gameData.loadPoints),
				new ColumnID("ManualPts","Manual Loads"            , Integer.class,  80, CENTER,      null, false, row->((TruckAddon)row).gameData.manualLoads),
				new ColumnID("ManlPts2" ,"Manual Loads 2"          , Integer.class,  90, CENTER,      null, false, row->((TruckAddon)row).gameData.manualLoads_IS),
		//		new ColumnID("SaddleT"  ,"Saddle Type (obsolete)"  ,  String.class, 130,   null,      null, false, row->((TruckAddon)row).gameData.saddleType_obsolete),
				new ColumnID("RequAddT" ,"Required Addon Type"     ,  String.class, 140,   null,      null, (obj,lang)->{ TruckAddon row = (TruckAddon)obj; return SnowRunner.getIdAndName(row.gameData.requiredAddonType, row.gameData.requiredAddonType_StringID, lang); }), 
				new ColumnID("Trls2Unlk","Trials to Unlock"        , Integer.class,  85, CENTER,      null, false, row->((TruckAddon)row).gameData.trialsToUnlock),
		//		new ColumnID("Wheel2Pck","Wheel to Pack"           , Integer.class,  80, CENTER,      null, false, row->((TruckAddon)row).gameData.wheelToPack_obsolete),
		//		new ColumnID("ShwPckStp","Show Packing Stoppers"   , Boolean.class, 125,   null,      null, false, row->((TruckAddon)row).gameData.showPackingStoppers_obsolete),
				new ColumnID("UnpTrlDet","Unpack on Trailer Detach", Boolean.class, 130,   null,      null, false, row->((TruckAddon)row).gameData.unpackOnTrailerDetach),
				new ColumnID("AddonType","Addon Type"              ,  String.class,  85,   null,      null, false, row->((TruckAddon)row).gameData.addonType),
				new ColumnID("SpecAddon","Special Addon"           ,  String.class,  85,   null,      null, false, row->getAssignedSpecialAddonList(specialTruckAddons,(TruckAddon)row)),
				new ColumnID("LoadAreas","Load Areas"              ,  String.class, 200,   null,      null, false, row->Data.GameData.GameDataT3NonTruck.LoadArea.toString(((TruckAddon)row).gameData.loadAreas)),
				new ColumnID("IsCargo"  ,"Is Cargo"                , Boolean.class,  80,   null,      null, false, row->((TruckAddon)row).gameData.isCargo),
				new ColumnID("CargLngth","Cargo Length"            , Integer.class,  80, CENTER,      null, false, row->((TruckAddon)row).gameData.cargoLength),
		//		new ColumnID("CargVal"  ,"Cargo Value"             , Integer.class,  80, CENTER,      null, false, row->((TruckAddon)row).gameData.cargoValue_obsolete),
				new ColumnID("CargType" ,"Cargo Type"              ,  String.class, 170,   null,      null, false, row->((TruckAddon)row).gameData.cargoType),
				new ColumnID("CargSType","Cargo Addon SubType"     ,  String.class, 120,   null,      null, false, row->((TruckAddon)row).gameData.cargoAddonSubtype),
				new ColumnID("UsableBy" ,"Usable By"               ,  String.class, 150,   null,      null, (row,lang)->SnowRunner.joinNames(((TruckAddon)row).usableBy, lang)),
		}));
		this.specialTruckAddons = specialTruckAddons;
		
		clickedItem = null;
		truckAddons = null;
		trailers    = null;
		saveGame    = null;
		if (connectToGlobalData)
			connectToGlobalData(true, data->{
				truckAddons = data==null ? null : data.truckAddons;
				trailers    = data==null ? null : data.trailers;
				return truckAddons==null ? null : truckAddons.values();
			});
		
		else
			finalizer.addDataReceiver(data -> {
				setExtraData(data);
				updateOutput();
			});
		
		finalizer.addSaveGameListener(saveGame->{
			this.saveGame = saveGame;
			updateOutput();
		});
		
		coloring.addBackgroundRowColorizer(addon->{
			if (enableSpecialTruckAddonsHighlighting)
				for (SpecialTruckAddons.AddonCategory listID : SpecialTruckAddons.AddonCategory.values()) {
					SpecialTruckAddonList list = this.specialTruckAddons.getList(listID);
					if (list.contains(addon)) return COLOR_SPECIALTRUCKADDON;
				}
			return null;
		});
		finalizer.addSpecialTruckAddonsListener((list, change) -> {
			int colM = findColumnByID("SpecAddon");
			if (colM>=0) fireTableColumnUpdate(colM);
			table.repaint();
		});
		
		Comparator<String> string_nullsLast = Comparator.nullsLast(Comparator.naturalOrder());
		setInitialRowOrder(Comparator.<TruckAddon,String>comparing(row->row.gameData.category,string_nullsLast).thenComparing(row->row.id));
	}
	
	private static <ResultType> ColumnID.TableModelBasedBuilder<ResultType> get(ColumnID.GetFunction_MLR<ResultType,TruckAddonsTableModel,TruckAddon> getFunction)
	{
		return ColumnID.get(TruckAddonsTableModel.class, TruckAddon.class, getFunction);
	}
	
	private static Long getOwnedCount(TruckAddonsTableModel model, TruckAddon row)
	{
		if (row==null) return null;
		if (model==null) return null;
		if (model.saveGame==null) return null;
		SaveGame.Addon addon = model.saveGame.addons.get(row.id);
		return addon==null ? null : addon.owned;
	}
	
	private static String getAssignedSpecialAddonList(SpecialTruckAddons specialTruckAddons, TruckAddon addon)
	{
		EnumSet<SpecialTruckAddons.AddonCategory> listIDs = EnumSet.noneOf(SpecialTruckAddons.AddonCategory.class);
		specialTruckAddons.foreachList((listID,list)->{
			if (list.contains(addon))
				listIDs.add(listID);
		});
		if (listIDs.isEmpty())
			return null;
		Iterable<String> it = ()->listIDs.stream().sorted().map(id->id.label).iterator();
		return String.join(", ", it);
	}

	public TruckAddonsTableModel set(Data data, SaveGame saveGame) {
		setExtraData(data);
		this.saveGame = saveGame;
		return this;
	}

	private void setExtraData(Data data) {
		truckAddons = data==null ? null : data.truckAddons;
		trailers    = data==null ? null : data.trailers;
	}

	@Override public void modifyTableContextMenu(JTable table_, TableSimplifier.ContextMenu contextMenu) {
		super.modifyTableContextMenu(table_, contextMenu);
		
		contextMenu.addSeparator();
		
		JMenu specialAddonsMenu = new JMenu("Add Addon to List of known special Addons");
		contextMenu.add(specialAddonsMenu);
		
		EnumMap<SpecialTruckAddons.AddonCategory, JCheckBoxMenuItem> menuItems = new EnumMap<>(SpecialTruckAddons.AddonCategory.class);
		for (SpecialTruckAddons.AddonCategory list : SpecialTruckAddons.AddonCategory.values()) {
			String listLabel = "";
			switch (list) {
			case MetalDetector   : listLabel = "Metal Detector for special missions"; break;
			case SeismicVibrator : listLabel = "Seismic Vibrator for special missions"; break;
			case LogLift         : listLabel = "Log Lift for loading logs"; break;
			case MiniCrane       : listLabel = "MiniCrane for loading cargo"; break;
			case BigCrane        : listLabel = "Big Crane for howling trucks"; break;
			case ShortLogs       : listLabel = "Short Logs"; break;
			case MediumLogs      : listLabel = "Medium Logs"; break;
			case LongLogs        : listLabel = "Long Logs"; break;
			case MediumLogs_burnt: listLabel = "Medium Logs (burnt)"; break;
			}
			
			JCheckBoxMenuItem  mi = SnowRunner.createCheckBoxMenuItem(listLabel, false, null, true, ()->{
				if (clickedItem==null) return;
				SpecialTruckAddonList addonList = specialTruckAddons.getList(list);
				if (addonList.contains(clickedItem))
					addonList.remove(clickedItem);
				else
					addonList.add(clickedItem);
			});
			specialAddonsMenu.add(mi);
			menuItems.put(list, mi);
		}
		
		contextMenu.addSeparator();
		
		JCheckBoxMenuItem miEnableSpecialTruckAddonsHighlighting;
		contextMenu.add(miEnableSpecialTruckAddonsHighlighting = SnowRunner.createCheckBoxMenuItem("Highlighting of special truck addons", enableSpecialTruckAddonsHighlighting, null, true, ()->{
			enableSpecialTruckAddonsHighlighting = !enableSpecialTruckAddonsHighlighting;
			SnowRunner.settings.putBool(SnowRunner.AppSettings.ValueKey.TruckAddonsTableModel_enableSpecialTruckAddonsHighlighting, enableSpecialTruckAddonsHighlighting);
			if (table!=null) table.repaint();
		}));
		
		contextMenu.addContextMenuInvokeListener((table, x, y) -> {
			int rowV = x==null || y==null ? -1 : table.rowAtPoint(new Point(x,y));
			int rowM = rowV<0 ? -1 : table.convertRowIndexToModel(rowV);
			clickedItem = rowM<0 ? null : getRow(rowM);
			
			if (clickedItem!=null)
				menuItems.forEach((list,mi)->{
					SpecialTruckAddonList addonList = specialTruckAddons.getList(list);
					mi.setSelected(addonList.contains(clickedItem));
					mi.setEnabled(list.isAllowed==null || list.isAllowed.test(clickedItem));
				});
			
			specialAddonsMenu.setEnabled(clickedItem!=null);
			specialAddonsMenu.setText(
				clickedItem==null
				? "Add Addon to a list of known special Addons"
				: String.format("Add Addon \"%s\" to a list of known special Addons", SnowRunner.solveStringID(clickedItem, language))
			);
			miEnableSpecialTruckAddonsHighlighting.setSelected(enableSpecialTruckAddonsHighlighting);
		});
	}

	@Override protected String getRowName(TruckAddon row)
	{
		return SnowRunner.solveStringID(row, language);
	}

	@Override protected void setOutputContentForRow(StyledDocumentInterface doc, int rowIndex, TruckAddon row) {
		String description_StringID = SnowRunner.selectNonNull( row.gameData.description_StringID, row.gameData.cargoDescription_StringID );
		String[][] requiredAddons   = row.gameData.requiredAddons;
		String[] excludedCargoTypes = row.gameData.excludedCargoTypes;
		Vector<Truck> usableBy = row.usableBy;
		generateText(doc, description_StringID, requiredAddons, excludedCargoTypes, usableBy, language, truckAddons, trailers, saveGame);
	}

	static void generateText(
			StyledDocumentInterface doc,
			String description_StringID,
			String[][] requiredAddons,
			String[] excludedCargoTypes,
			Vector<Truck> usableBy,
			Language language, HashMap<String, TruckAddon> truckAddons, HashMap<String, Trailer> trailers, SaveGame saveGame) {
		boolean isFirst = true;
		
		
		String description = SnowRunner.solveStringID(description_StringID, language);
		if (description!=null && !"EMPTY_LINE".equals(description)) {
			doc.append(Style.BOLD,"Description: ");
			doc.append(new Style(Color.GRAY,false,true,"Monospaced"),"<%s>%n", description_StringID);
			doc.append("%s", description);
			isFirst = false;
		}
		
		if (requiredAddons!=null && requiredAddons.length>0) {
			if (!isFirst) doc.append("%n%n");
			isFirst = false;
			doc.append(Style.BOLD,"Required Addons:%n");
			if (truckAddons==null && trailers==null)
				SnowRunner.writeRequiredAddonsToDoc(doc, Color.GRAY, requiredAddons, "    ");
				//doc.append("%s", SnowRunner.joinRequiredAddonsToString(requiredAddons, "    "));
			else {
				doc.append(Color.GRAY,"    [IDs]%n");
				SnowRunner.writeRequiredAddonsToDoc(doc, Color.GRAY, requiredAddons, "        ");
				//doc.append("%s%n", SnowRunner.joinRequiredAddonsToString(requiredAddons, "        "));
				doc.append(Color.GRAY,"%n    [Names]%n");
				SnowRunner.writeRequiredAddonsToDoc(doc, Color.GRAY, requiredAddons, SnowRunner.createGetNameFunction(truckAddons, trailers), language, "        ");
				//doc.append("%s", SnowRunner.joinRequiredAddonsToString(requiredAddons, SnowRunner.createGetNameFunction(truckAddons, trailers), language, "        "));
			}
		}
		
		if (excludedCargoTypes!=null && excludedCargoTypes.length>0) {
			if (!isFirst) doc.append("%n%n");
			isFirst = false;
			doc.append(Style.BOLD,"Excluded Cargo Types:%n");
			doc.append("    %s", SnowRunner.joinAddonIDs(excludedCargoTypes,false));
		}
		
		if (usableBy!=null && !usableBy.isEmpty()) {
			if (!isFirst) doc.append("%n%n");
			isFirst = false;
			doc.append(Style.BOLD,"Usable By:%n");
			SnowRunner.writeTruckNamesToDoc(doc, SnowRunner.COLOR_FG_OWNEDTRUCK, usableBy, language, saveGame);
			//doc.append("%s", SnowRunner.joinTruckNames(usableBy,language));
		}
		
	}
}